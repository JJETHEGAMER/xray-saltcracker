package com.xray.saltcracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Haupt-GUI für die XRay Mod
 * Version: No-Inheritance (Verhindert AbstractMethodError durch Nutzung von Standard-Buttons)
 */
public class XRayGui extends Screen {
    
    private final XRaySaltCracker mod;
    
    // GUI-Komponenten
    private TextFieldWidget worldSeedField;
    private TextFieldWidget structureSaltField;
    private TextFieldWidget oreSaltField;
    
    // Wir brauchen keine Referenzen auf die Buttons speichern,
    // da sie sich selbst über Callbacks verwalten.
    
    public XRayGui() {
        super(Text.literal("XRay Salt Cracker"));
        this.mod = XRaySaltCracker.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 40;
        
        // TextRenderer sicher holen
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // === Seed-Eingaben ===
        // World Seed Feld
        worldSeedField = new TextFieldWidget(textRenderer, centerX - 150, startY, 300, 20, Text.literal("Seed"));
        worldSeedField.setMaxLength(20);
        worldSeedField.setText(String.valueOf(mod.getWorldSeed()));
        this.addSelectableChild(worldSeedField);

        // Structure Salt Feld
        structureSaltField = new TextFieldWidget(textRenderer, centerX - 150, startY + 40, 300, 20, Text.literal("Structure Salt"));
        structureSaltField.setEditable(false);
        structureSaltField.setText(mod.getStructureSalt() != null ? String.valueOf(mod.getStructureSalt()) : "Noch nicht gefunden");
        this.addSelectableChild(structureSaltField);

        // Ore Salt Feld
        oreSaltField = new TextFieldWidget(textRenderer, centerX - 150, startY + 80, 300, 20, Text.literal("Ore Salt"));
        oreSaltField.setEditable(false);
        oreSaltField.setText(mod.getOreSalt() != null ? String.valueOf(mod.getOreSalt()) : "Noch nicht gefunden");
        this.addSelectableChild(oreSaltField);
        
        // === Control-Buttons ===
        
        // Mod An/Aus
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(mod.isEnabled() ? "Mod: AN" : "Mod: AUS"),
            button -> {
                mod.setEnabled(!mod.isEnabled());
                button.setMessage(Text.literal(mod.isEnabled() ? "Mod: AN" : "Mod: AUS"));
            })
            .dimensions(centerX - 150, startY + 120, 145, 20)
            .build());
        
        // World Seed setzen
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Seed Setzen"),
            button -> {
                try {
                    String text = worldSeedField.getText();
                    if (text != null && !text.isEmpty()) {
                        long seed = Long.parseLong(text);
                        mod.setWorldSeed(seed);
                        if (mod.getPredictionEngine() != null) {
                            mod.getPredictionEngine().updateWorldSeed(seed);
                        }
                    }
                } catch (NumberFormatException e) {
                    XRaySaltCracker.LOGGER.error("Ungültiger Seed");
                }
            })
            .dimensions(centerX + 5, startY + 120, 145, 20)
            .build());
        
        // === Solver-Buttons ===
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Cracke Structure Salt"),
            button -> startStructureSolving())
            .dimensions(centerX - 150, startY + 150, 145, 20)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Cracke Ore Salt"),
            button -> startOreSolving())
            .dimensions(centerX + 5, startY + 150, 145, 20)
            .build());
        
        // === Daten-Management ===
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Daten Löschen"),
            button -> {
                mod.getDataCollector().clearAllData();
                if (mod.getPredictionEngine() != null) {
                    mod.getPredictionEngine().clearPredictions();
                }
            })
            .dimensions(centerX - 150, startY + 180, 145, 20)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Daten Exportieren"),
            button -> exportData())
            .dimensions(centerX + 5, startY + 180, 145, 20)
            .build());
        
        // === Feature-Toggles ===
        
        initializeFeatureToggles(centerX, startY + 220);
        
        // === Schließen-Button ===
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Schließen"),
            button -> this.close())
            .dimensions(centerX - 75, this.height - 30, 150, 20)
            .build());
    }
    
    private void initializeFeatureToggles(int centerX, int startY) {
        String[] features = {
            "Buried Treasure", "Desert Pyramid", "Jungle Temple",
            "Diamond Ore", "Emerald Ore", "Gold Ore", "Iron Ore"
        };
        
        int y = startY;
        ESPRenderer renderer = mod.getESPRenderer();
        
        for (String feature : features) {
            boolean isEnabled = renderer != null && renderer.isFeatureEnabled(feature);
            String label = feature + ": " + (isEnabled ? "AN" : "AUS");
            
            // Wir erstellen hier Standard-Buttons statt einer eigenen Klasse
            // Das verhindert den AbstractMethodError
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(label),
                button -> {
                    // Toggle Logic
                    if (renderer != null) {
                        boolean newState = !renderer.isFeatureEnabled(feature);
                        renderer.setFeatureEnabled(feature, newState);
                        // Update Text
                        button.setMessage(Text.literal(feature + ": " + (newState ? "AN" : "AUS")));
                    }
                })
                .dimensions(centerX - 150, y, 300, 20)
                .build());
                
            y += 25;
        }
    }
    
    private void startStructureSolving() {
        DataCollector collector = mod.getDataCollector();
        List<DataCollector.DataPoint> treasureData = 
            collector.getDataPointsByType("buried_treasure");
        
        if (treasureData.size() < 5) {
            XRaySaltCracker.LOGGER.warn("Zu wenig Buried Treasures gefunden: " + treasureData.size());
            return;
        }
        
        long worldSeed = mod.getWorldSeed();
        if (worldSeed == 0) {
            XRaySaltCracker.LOGGER.warn("World Seed nicht gesetzt!");
            return;
        }
        
        mod.getSaltSolver().solveStructureSalt(worldSeed, treasureData, 
            (salt, confidence) -> {
                mod.setStructureSalt(salt);
                structureSaltField.setText(String.valueOf(salt));
                XRaySaltCracker.LOGGER.info("Structure Salt: " + salt + " (Konfidenz: " + confidence + ")");
            }
        );
    }
    
    private void startOreSolving() {
        XRaySaltCracker.LOGGER.info("Ore Salt Solving noch nicht implementiert");
    }
    
    private void exportData() {
        String json = mod.getDataCollector().exportAsJson();
        XRaySaltCracker.LOGGER.info("Exportierte Daten:\n" + json);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Hintergrund füllen (Safe Mode)
        context.fill(0, 0, this.width, this.height, 0xAA000000);
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Titel
        context.drawCenteredTextWithShadow(
            textRenderer,
            this.title,
            this.width / 2,
            10,
            0xFFFFFF
        );
        
        // Labels
        int centerX = this.width / 2;
        context.drawTextWithShadow(
            textRenderer,
            "World Seed:",
            centerX - 150, 30,
            0xAAAAAA
        );
        context.drawTextWithShadow(
            textRenderer,
            "Structure Salt:",
            centerX - 150, 70,
            0xAAAAAA
        );
        context.drawTextWithShadow(
            textRenderer,
            "Ore Salt:",
            centerX - 150, 110,
            0xAAAAAA
        );
        
        // Status-Anzeige
        String status = mod.getSaltSolver().getStatus();
        
        context.drawCenteredTextWithShadow(
            textRenderer,
            "Status: " + status,
            this.width / 2,
            this.height - 50,
            0xFFFF00
        );
        
        if (mod.getSaltSolver().isSolving()) {
            double progress = mod.getSaltSolver().getProgress();
            context.drawCenteredTextWithShadow(
                textRenderer,
                String.format("Progress: %.1f%%", progress * 100),
                this.width / 2,
                this.height - 65,
                0x00FF00
            );
        }
        
        // Gesammelte Daten-Statistik
        DataCollector collector = mod.getDataCollector();
        int treasureCount = collector.getDataCount("structure_buried_treasure");
        int diamondCount = collector.getDataCount("ore_diamond");
        
        context.drawTextWithShadow(
            textRenderer,
            "Gesammelt: " + treasureCount + " Treasures, " + diamondCount + " Diamonds",
            10, this.height - 20,
            0xAAAAAA
        );
        
        // WICHTIG: Das hier rendert alle Buttons, die wir mit addDrawableChild hinzugefügt haben.
        // Da wir nun Standard-Buttons nutzen, sollte das nicht mehr crashen.
        super.render(context, mouseX, mouseY, delta);
        
        // Manuelles Zeichnen der Textfelder (muss oft separat gemacht werden)
        worldSeedField.render(context, mouseX, mouseY, delta);
        structureSaltField.render(context, mouseX, mouseY, delta);
        oreSaltField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}