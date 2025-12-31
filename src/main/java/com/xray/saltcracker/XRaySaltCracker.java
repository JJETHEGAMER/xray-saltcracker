package com.xray.saltcracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XRaySaltCracker implements ClientModInitializer {
    
    public static final String MOD_ID = "xray_saltcracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static XRaySaltCracker instance;
    private boolean wasInsertPressed = false;
    
    private DataCollector dataCollector;
    private SaltSolver saltSolver;
    private PredictionEngine predictionEngine;
    private ConfigManager configManager;
    private ESPRenderer espRenderer; // WICHTIG: Darf nicht fehlen!
    
    private boolean enabled = true;
    private long worldSeed = 0;
    private Long structureSalt = null;
    private Long oreSalt = null;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("XRay Salt Cracker: Initializing CHAT-FEEDBACK Mode...");
        
        try {
            this.predictionEngine = new PredictionEngine();
            this.dataCollector = new DataCollector();
            this.saltSolver = new SaltSolver();
            
            // WICHTIG: Wir müssen den Renderer laden, auch wenn er nichts zeichnet!
            // Sonst stürzt der ConfigManager ab (NullPointerException).
            this.espRenderer = new ESPRenderer(); 
            
            this.configManager = new ConfigManager();
            LOGGER.info("Systeme erfolgreich geladen.");
        } catch (Exception e) {
            LOGGER.error("Fehler beim Laden der Systeme:", e);
        }
        
        // --- Input Loop (EINFG Taste) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            
            long handle = client.getWindow().getHandle();
            int keyState = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_INSERT);
            boolean isPressed = (keyState == GLFW.GLFW_PRESS);
            
            if (isPressed && !wasInsertPressed) {
                // 1. Menü öffnen
                if (client.currentScreen == null || client.currentScreen instanceof XRayGui) {
                    client.execute(() -> client.setScreen(new XRayGui()));
                }
                
                // 2. Status in den Chat schreiben
                if (client.player != null) {
                    String status = "Idle";
                    if (saltSolver.isSolving()) status = "Cracking... " + (int)(saltSolver.getProgress() * 100) + "%";
                    else if (structureSalt != null) status = "§aSALT FOUND: " + structureSalt;
                    
                    int chests = (dataCollector != null) ? dataCollector.getDataCount("structure_buried_treasure") : 0;
                    
                    String msg = "§6[XRay] §fStatus: " + status + " §7| §fTreasures: §e" + chests;
                    client.player.sendMessage(Text.of(msg), false);
                }
            }
            wasInsertPressed = isPressed;
        });
        
        LOGGER.info("Initialisierung fertig.");
    }
    
    public static XRaySaltCracker getInstance() { return instance; }
    
    public void execute(Runnable task) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.execute(task);
    }
    
    // --- Getters & Setters ---
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public long getWorldSeed() { return worldSeed; }
    public void setWorldSeed(long worldSeed) {
        this.worldSeed = worldSeed;
        if (predictionEngine != null) predictionEngine.updateWorldSeed(worldSeed);
    }

    public Long getStructureSalt() { return structureSalt; }
    public void setStructureSalt(Long structureSalt) {
        this.structureSalt = structureSalt;
        if (predictionEngine != null) predictionEngine.updateStructureSalt(structureSalt);
    }

    public Long getOreSalt() { return oreSalt; }
    public void setOreSalt(Long oreSalt) {
        this.oreSalt = oreSalt;
        if (predictionEngine != null) predictionEngine.updateOreSalt(oreSalt);
    }
    
    public DataCollector getDataCollector() { return dataCollector; }
    public SaltSolver getSaltSolver() { return saltSolver; }
    public PredictionEngine getPredictionEngine() { return predictionEngine; }
    
    // WICHTIG: Muss den echten (Dummy) Renderer zurückgeben, NICHT null!
    public ESPRenderer getESPRenderer() { return espRenderer; } 
    
    public ConfigManager getConfigManager() { return configManager; }
}