package com.xray.saltcracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Constructor;

/**
 * XRay Salt Cracker Mod
 * Version: Ultra-Safe Mode (Direct LWJGL Input)
 */
public class XRaySaltCracker implements ClientModInitializer {
    public static final String MOD_ID = "xray_saltcracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static XRaySaltCracker instance;
    
    // Komponenten
    private DataCollector dataCollector;
    private SaltSolver saltSolver;
    private PredictionEngine predictionEngine;
    private ESPRenderer espRenderer;
    private ConfigManager configManager;
    
    // GUI
    private XRayGui gui;
    
    // Status
    private boolean wasOpenKeyPressed = false;
    private boolean modEnabled = false;
    private long worldSeed = 0;
    private Long structureSalt = null;
    private Long oreSalt = null;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initialisiere XRay Salt Cracker (Ultra Safe Mode)...");
        
        // 1. Komponenten
        predictionEngine = new PredictionEngine();
        espRenderer = new ESPRenderer();
        dataCollector = new DataCollector();
        saltSolver = new SaltSolver();
        
        // 2. Config
        configManager = new ConfigManager();
        
        // 3. GUI
        gui = new XRayGui();
        
        // Wir versuchen, ein KeyBinding zu registrieren, damit es sauber aussieht.
        // Wenn das fehlschlägt (wie im Log gesehen), ist es egal, da wir unten den Direct-Check haben.
        registerDummyKeyBinding();
        
        // Event-Handler
        registerEvents();
        
        LOGGER.info("XRay Salt Cracker bereit! Druecke EINFG (Insert) fuer das Menü.");
    }
    
    private void registerDummyKeyBinding() {
        try {
            KeyBinding tempKey = createSafeKeyBinding(
                "key.xray_saltcracker.open_gui",
                GLFW.GLFW_KEY_INSERT,
                "category.xray_saltcracker"
            );
            if (tempKey != null) {
                KeyBindingHelper.registerKeyBinding(tempKey);
            }
        } catch (Exception e) {
            // Ignorieren, ist nur Kosmetik
        }
    }

    private KeyBinding createSafeKeyBinding(String name, int code, String category) {
        try {
            Constructor<KeyBinding> c4 = KeyBinding.class.getConstructor(String.class, InputUtil.Type.class, int.class, String.class);
            return c4.newInstance(name, InputUtil.Type.KEYSYM, code, category);
        } catch (Exception e1) {
            try {
                Constructor<KeyBinding> c3 = KeyBinding.class.getConstructor(String.class, int.class, String.class);
                return c3.newInstance(name, code, category);
            } catch (Exception e2) {
                return null;
            }
        }
    }
    
    private void registerEvents() {
        // Client-Tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            
            long handle = client.getWindow().getHandle();
            
            // --- FIX: ULTRA DIRECT INPUT ---
            // Wir nutzen nicht InputUtil (Minecraft), sondern GLFW direkt (Grafik-Bibliothek).
            // Das kann nicht crashen, solange das Fenster existiert.
            int state = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_INSERT);
            boolean isPressed = (state == GLFW.GLFW_PRESS);
            
            if (isPressed && !wasOpenKeyPressed) {
                if (client.currentScreen == null) {
                    client.setScreen(gui);
                } else if (client.currentScreen == gui) {
                    client.setScreen(null);
                }
            }
            
            wasOpenKeyPressed = isPressed;
        });
        
        // Rendering ist weiterhin auskommentiert, um Crashs zu vermeiden
    }
    
    // --- Getter & Setter ---
    public static XRaySaltCracker getInstance() { return instance; }
    public DataCollector getDataCollector() { return dataCollector; }
    public SaltSolver getSaltSolver() { return saltSolver; }
    public PredictionEngine getPredictionEngine() { return predictionEngine; }
    public ESPRenderer getESPRenderer() { return espRenderer; }
    public ConfigManager getConfigManager() { return configManager; }
    
    public void setEnabled(boolean enabled) {
        this.modEnabled = enabled;
        LOGGER.info("Mod " + (enabled ? "aktiviert" : "deaktiviert"));
    }
    public boolean isEnabled() { return modEnabled; }
    
    public void setWorldSeed(long seed) {
        this.worldSeed = seed;
        LOGGER.info("World Seed gesetzt: " + seed);
    }
    public long getWorldSeed() { return worldSeed; }
    
    public void setStructureSalt(Long salt) {
        this.structureSalt = salt;
        if (salt != null && predictionEngine != null) predictionEngine.updateStructureSalt(salt);
    }
    public Long getStructureSalt() { return structureSalt; }
    
    public void setOreSalt(Long salt) {
        this.oreSalt = salt;
        if (salt != null && predictionEngine != null) predictionEngine.updateOreSalt(salt);
    }
    public Long getOreSalt() { return oreSalt; }
}