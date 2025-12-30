package com.xray.saltcracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Verwaltet Konfiguration und gespeicherte Daten
 */
public class ConfigManager {
    
    private static final String CONFIG_FILE = "xray_saltcracker.json";
    private final Gson gson;
    private final Path configPath;
    
    // Konfiguration
    public static class Config {
        public long worldSeed = 0;
        public Long structureSalt = null;
        public Long oreSalt = null;
        public Map<String, Boolean> featureEnabled = new HashMap<>();
        public Map<String, int[]> featureColors = new HashMap<>();
        
        // Gesammelte Daten
        public Map<String, SavedDataPoint[]> collectedData = new HashMap<>();
    }
    
    public static class SavedDataPoint {
        public String type;
        public int x, y, z;
        public long timestamp;
        public boolean validated;
        
        public SavedDataPoint() {}
        
        public SavedDataPoint(DataCollector.DataPoint dp) {
            this.type = dp.type;
            this.x = dp.position.getX();
            this.y = dp.position.getY();
            this.z = dp.position.getZ();
            this.timestamp = dp.timestamp;
            this.validated = dp.validated;
        }
    }
    
    private Config config;
    
    public ConfigManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Config-Pfad
        try {
            configPath = Paths.get("config", CONFIG_FILE);
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Konnte Config-Verzeichnis nicht erstellen", e);
        }
        
        // Lade Config
        loadConfig();
        
        XRaySaltCracker.LOGGER.info("ConfigManager initialisiert");
    }
    
    /**
     * Lädt Konfiguration von Datei
     */
    public void loadConfig() {
        if (!Files.exists(configPath)) {
            config = new Config();
            saveConfig();
            XRaySaltCracker.LOGGER.info("Neue Config erstellt");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(configPath)) {
            config = gson.fromJson(reader, Config.class);
            if (config == null) {
                config = new Config();
            }
            XRaySaltCracker.LOGGER.info("Config geladen");
            
            // Wende geladene Config an
            applyConfig();
            
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Laden der Config", e);
            config = new Config();
        }
    }
    
    /**
     * Speichert Konfiguration in Datei
     */
    public void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            // Sammle aktuelle Daten
            collectCurrentData();
            
            gson.toJson(config, writer);
            XRaySaltCracker.LOGGER.info("Config gespeichert");
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Speichern der Config", e);
        }
    }
    
    /**
     * Wendet geladene Config an
     */
    private void applyConfig() {
        XRaySaltCracker mod = XRaySaltCracker.getInstance();
        
        // Seeds
        if (config.worldSeed != 0) {
            mod.setWorldSeed(config.worldSeed);
        }
        if (config.structureSalt != null) {
            mod.setStructureSalt(config.structureSalt);
        }
        if (config.oreSalt != null) {
            mod.setOreSalt(config.oreSalt);
        }
        
        // Feature-Einstellungen
        ESPRenderer renderer = mod.getESPRenderer();
        for (Map.Entry<String, Boolean> entry : config.featureEnabled.entrySet()) {
            renderer.setFeatureEnabled(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, int[]> entry : config.featureColors.entrySet()) {
            int[] color = entry.getValue();
            if (color.length == 4) {
                renderer.setFeatureColor(entry.getKey(), color[0], color[1], color[2], color[3]);
            }
        }
        
        // Gesammelte Daten laden
        DataCollector collector = mod.getDataCollector();
        for (Map.Entry<String, SavedDataPoint[]> entry : config.collectedData.entrySet()) {
            for (SavedDataPoint sdp : entry.getValue()) {
                collector.addManualEntry(
                    entry.getKey().split("_")[0], // "ore" oder "structure"
                    sdp.type,
                    sdp.x, sdp.y, sdp.z
                );
            }
        }
        
        XRaySaltCracker.LOGGER.info("Config angewendet: " + 
            config.collectedData.size() + " Kategorien geladen");
    }
    
    /**
     * Sammelt aktuelle Daten für Speicherung
     */
    private void collectCurrentData() {
        XRaySaltCracker mod = XRaySaltCracker.getInstance();
        
        // Aktuelle Seeds
        config.worldSeed = mod.getWorldSeed();
        config.structureSalt = mod.getStructureSalt();
        config.oreSalt = mod.getOreSalt();
        
        // Feature-Einstellungen
        ESPRenderer renderer = mod.getESPRenderer();
        config.featureEnabled.clear();
        String[] features = {
            "Buried Treasure", "Desert Pyramid", "Jungle Temple",
            "Diamond Ore", "Emerald Ore", "Gold Ore", "Iron Ore"
        };
        for (String feature : features) {
            config.featureEnabled.put(feature, renderer.isFeatureEnabled(feature));
        }
        
        // Gesammelte Daten
        DataCollector collector = mod.getDataCollector();
        config.collectedData.clear();
        
        for (Map.Entry<String, java.util.List<DataCollector.DataPoint>> entry : 
             collector.getAllData().entrySet()) {
            SavedDataPoint[] savedPoints = entry.getValue().stream()
                .map(SavedDataPoint::new)
                .toArray(SavedDataPoint[]::new);
            config.collectedData.put(entry.getKey(), savedPoints);
        }
    }
    
    /**
     * Exportiert gesammelte Daten als separates JSON
     */
    public void exportData(String filename) {
        collectCurrentData();
        
        Path exportPath = Paths.get("exports", filename);
        try {
            Files.createDirectories(exportPath.getParent());
            
            try (Writer writer = Files.newBufferedWriter(exportPath)) {
                gson.toJson(config.collectedData, writer);
                XRaySaltCracker.LOGGER.info("Daten exportiert nach: " + exportPath);
            }
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Export", e);
        }
    }
    
    /**
     * Importiert Daten aus JSON-Datei
     */
    public void importData(String filename) {
        Path importPath = Paths.get("exports", filename);
        
        if (!Files.exists(importPath)) {
            XRaySaltCracker.LOGGER.error("Import-Datei existiert nicht: " + importPath);
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(importPath)) {
            @SuppressWarnings("unchecked")
            Map<String, SavedDataPoint[]> importedData = 
                gson.fromJson(reader, Map.class);
            
            if (importedData != null) {
                config.collectedData = importedData;
                applyConfig();
                XRaySaltCracker.LOGGER.info("Daten importiert: " + 
                    importedData.size() + " Kategorien");
            }
        } catch (IOException e) {
            XRaySaltCracker.LOGGER.error("Fehler beim Import", e);
        }
    }
    
    /**
     * Gibt aktuelle Config zurück
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * Speichert Config beim Herunterfahren
     */
    public void onShutdown() {
        saveConfig();
        XRaySaltCracker.LOGGER.info("Config beim Herunterfahren gespeichert");
    }
}