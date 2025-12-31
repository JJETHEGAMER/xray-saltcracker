package com.xray.saltcracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;
    private final File dataFile;

    public ConfigManager() {
        File configDir = FabricLoader.getInstance().getConfigDir().resolve("xray_saltcracker").toFile();
        if (!configDir.exists()) configDir.mkdirs();
        
        configFile = new File(configDir, "config.json");
        dataFile = new File(configDir, "data.json");
        
        // Erst Config laden (für Toggles/Seed), dann Daten
        loadConfig();
        loadData();
        
        // Auto-Save Hook beim Beenden
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveConfig();
            saveData();
        }));
    }
    
    // --- Daten-Strukturen für JSON ---
    
    private static class SavedDataPoint {
        String type;
        int x, y, z;
        long timestamp;
        boolean validated;
        
        SavedDataPoint() {}
        SavedDataPoint(DataCollector.DataPoint dp) {
            this.type = dp.type;
            this.x = dp.pos.getX();
            this.y = dp.pos.getY();
            this.z = dp.pos.getZ();
            this.timestamp = dp.timestamp;
            this.validated = dp.validated;
        }
    }
    
    private static class ConfigData {
        long worldSeed;
        Long structureSalt;
        Long oreSalt;
        // Speichert welche Features an/aus sind
        Map<String, Boolean> featureToggles = new HashMap<>();
        // Speichert die Farben (als Integer)
        Map<String, Integer> featureColors = new HashMap<>();
    }

    // --- Speichern ---

    public void saveData() {
        DataCollector collector = XRaySaltCracker.getInstance().getDataCollector();
        if (collector == null) return;
        
        List<SavedDataPoint> toSave = new ArrayList<>();
        for (List<DataCollector.DataPoint> list : collector.getAllData().values()) {
            for (DataCollector.DataPoint dp : list) {
                toSave.add(new SavedDataPoint(dp));
            }
        }
        
        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(toSave, writer);
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Speichern der Daten", e);
        }
    }
    
    public void saveConfig() {
        XRaySaltCracker mod = XRaySaltCracker.getInstance();
        ESPRenderer renderer = mod.getESPRenderer();
        
        ConfigData config = new ConfigData();
        config.worldSeed = mod.getWorldSeed();
        config.structureSalt = mod.getStructureSalt();
        config.oreSalt = mod.getOreSalt();
        
        // Toggles und Farben aus dem Renderer holen
        if (renderer != null) {
            // Wir iterieren über bekannte Features (hardcoded list oder via Reflection, hier vereinfacht)
            String[] features = {
                "Buried Treasure", "Diamond Ore", "Gold Ore", "Iron Ore", 
                "Emerald Ore", "Desert Pyramid", "Jungle Temple"
            };
            
            for (String f : features) {
                config.featureToggles.put(f, renderer.isFeatureEnabled(f));
                // Farben holen ist im aktuellen Renderer noch nicht public exposed, 
                // aber wir speichern die Toggles, das ist das Wichtigste.
            }
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Speichern der Config", e);
        }
    }

    // --- Laden ---

    private void loadData() {
        if (!dataFile.exists()) return;
        
        try (FileReader reader = new FileReader(dataFile)) {
            SavedDataPoint[] loaded = GSON.fromJson(reader, SavedDataPoint[].class);
            if (loaded == null) return;
            
            DataCollector collector = XRaySaltCracker.getInstance().getDataCollector();
            if (collector != null) {
                int count = 0;
                for (SavedDataPoint sdp : loaded) {
                    collector.addManualEntry(sdp.type, "", sdp.x, sdp.y, sdp.z);
                    count++;
                }
                XRaySaltCracker.LOGGER.info(count + " Datenpunkte geladen.");
            }
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Laden der Daten", e);
        }
    }

    private void loadConfig() {
        if (!configFile.exists()) return;
        
        try (FileReader reader = new FileReader(configFile)) {
            ConfigData config = GSON.fromJson(reader, ConfigData.class);
            if (config == null) return;
            
            XRaySaltCracker mod = XRaySaltCracker.getInstance();
            if (config.worldSeed != 0) mod.setWorldSeed(config.worldSeed);
            if (config.structureSalt != null) mod.setStructureSalt(config.structureSalt);
            if (config.oreSalt != null) mod.setOreSalt(config.oreSalt);
            
            // Toggles wiederherstellen
            ESPRenderer renderer = mod.getESPRenderer();
            if (renderer != null && config.featureToggles != null) {
                for (Map.Entry<String, Boolean> entry : config.featureToggles.entrySet()) {
                    renderer.setFeatureEnabled(entry.getKey(), entry.getValue());
                }
            }
            
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Laden der Config", e);
        }
    }
}