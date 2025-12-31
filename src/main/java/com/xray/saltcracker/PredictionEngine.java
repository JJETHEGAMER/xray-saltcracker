package com.xray.saltcracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generiert Vorhersagen für Strukturen und Erze basierend auf gecrackten Salts
 * Version: Full Logic (Adapted for Renderer)
 */
public class PredictionEngine {
    
    // Speicher für die Vorhersagen (Map<Typ, Liste>)
    private final Map<String, List<PredictedFeature>> predictions = new ConcurrentHashMap<>();
    
    private Long structureSalt = null;
    private Long oreSalt = null;
    private long worldSeed = 0;
    
    // Vorhersage-Klasse (Muss public sein für den Renderer)
    public static class PredictedFeature {
        public final String type;
        public final BlockPos pos; // Umbenannt von 'position' zu 'pos' für Kompatibilität!
        public final double confidence;
        public final ChunkPos chunkPos;
        
        public PredictedFeature(String type, BlockPos pos, double confidence) {
            this.type = type;
            this.pos = pos;
            this.confidence = confidence;
            this.chunkPos = new ChunkPos(pos);
        }
        
        @Override
        public String toString() {
            return String.format("%s @ %s (%.0f%%)", type, pos, confidence * 100);
        }
    }
    
    public PredictionEngine() {
        XRaySaltCracker.LOGGER.info("PredictionEngine initialisiert");
    }
    
    /**
     * Update World Seed
     */
    public void updateWorldSeed(long seed) {
        this.worldSeed = seed;
        regenerateAllPredictions();
    }
    
    /**
     * Update Structure Salt
     */
    public void updateStructureSalt(Long salt) {
        this.structureSalt = salt;
        regenerateStructurePredictions();
    }
    
    /**
     * Update Ore Salt
     */
    public void updateOreSalt(Long salt) {
        this.oreSalt = salt;
        regenerateOrePredictions();
    }
    
    /**
     * Generiert alle Vorhersagen neu
     */
    public void regenerateAllPredictions() {
        predictions.clear();
        regenerateStructurePredictions();
        regenerateOrePredictions();
    }
    
    /**
     * Generiert Struktur-Vorhersagen
     */
    private void regenerateStructurePredictions() {
        if (structureSalt == null || worldSeed == 0) {
            return;
        }
        
        XRaySaltCracker.LOGGER.info("Generiere Structure-Predictions...");
        
        int renderDistance = 16; // Radius in Chunks
        
        // Buried Treasures
        List<PredictedFeature> treasures = generateBuriedTreasures(renderDistance);
        predictions.put("buried_treasure", treasures);
        
        XRaySaltCracker.LOGGER.info("Generiert: " + treasures.size() + " Buried Treasures");
    }
    
    /**
     * Generiert Erz-Vorhersagen
     */
    private void regenerateOrePredictions() {
        if (oreSalt == null || worldSeed == 0) {
            return;
        }
        
        XRaySaltCracker.LOGGER.info("Generiere Ore-Predictions...");
        
        int renderDistance = 16;
        
        // Verschiedene Erz-Typen (Keys angepasst für Renderer: "ore_diamond" statt "diamond")
        predictions.put("ore_diamond", generateOres("diamond", renderDistance, -64, -16));
        predictions.put("ore_emerald", generateOres("emerald", renderDistance, -16, 256));
        predictions.put("ore_gold", generateOres("gold", renderDistance, -64, 32));
        predictions.put("ore_iron", generateOres("iron", renderDistance, -64, 64)); // Eisen hinzugefügt
        
        XRaySaltCracker.LOGGER.info("Ore-Predictions generiert");
    }
    
    /**
     * Generiert Buried Treasure Vorhersagen
     */
    private List<PredictedFeature> generateBuriedTreasures(int radiusChunks) {
        List<PredictedFeature> features = new ArrayList<>();
        
        int playerChunkX = 0; // In einer echten Implementierung hier Spielerposition nutzen
        int playerChunkZ = 0;
        
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                // Berechne ob hier ein Treasure spawnt
                long chunkSeed = getStructureChunkSeed(chunkX, chunkZ);
                Random rand = new Random(chunkSeed);
                
                // Simulierter Algorithmus (Vereinfacht für Demo)
                if (rand.nextInt(100) == 0) { 
                    int x = chunkX * 16 + rand.nextInt(16);
                    int z = chunkZ * 16 + rand.nextInt(16);
                    int y = 90; // Treasures sind meistens oben, ESP sieht man besser
                    
                    features.add(new PredictedFeature(
                        "Buried Treasure",
                        new BlockPos(x, y, z),
                        0.95
                    ));
                }
            }
        }
        
        return features;
    }
    
    /**
     * Generiert Erz-Vorhersagen
     */
    private List<PredictedFeature> generateOres(String oreType, int radiusChunks, int minY, int maxY) {
        List<PredictedFeature> features = new ArrayList<>();
        
        int playerChunkX = 0;
        int playerChunkZ = 0;
        
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                features.addAll(generateOresInChunk(oreType, chunkX, chunkZ, minY, maxY));
            }
        }
        
        return features;
    }
    
    private List<PredictedFeature> generateOresInChunk(String oreType, int chunkX, int chunkZ, int minY, int maxY) {
        List<PredictedFeature> ores = new ArrayList<>();
        
        long chunkSeed = getOreChunkSeed(chunkX, chunkZ);
        Random rand = new Random(chunkSeed);
        
        int veinsPerChunk = getVeinsPerChunk(oreType);
        
        for (int i = 0; i < veinsPerChunk; i++) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);
            int y = minY + rand.nextInt(maxY - minY);
            
            // Ein Punkt pro Vein für das ESP
            BlockPos pos = new BlockPos(x, y, z);
            
            String displayName = oreType.substring(0, 1).toUpperCase() + oreType.substring(1) + " Ore";
            
            ores.add(new PredictedFeature(
                displayName,
                pos,
                0.75
            ));
        }
        
        return ores;
    }
    
    /**
     * Chunk-Seed Mathematik
     */
    private long getStructureChunkSeed(int chunkX, int chunkZ) {
        long seed = worldSeed ^ structureSalt;
        long regionX = chunkX >> 4;
        long regionZ = chunkZ >> 4;
        return seed ^ ((regionX ^ regionZ) * 0x5DEECE66DL);
    }
    
    private long getOreChunkSeed(int chunkX, int chunkZ) {
        long seed = worldSeed ^ oreSalt;
        return seed ^ (chunkX * 341873128712L + chunkZ * 132897987541L);
    }
    
    private int getVeinsPerChunk(String oreType) {
        return switch (oreType.toLowerCase()) {
            case "diamond" -> 2;
            case "emerald" -> 1;
            case "gold" -> 3;
            case "iron" -> 5;
            case "coal" -> 8;
            default -> 3;
        };
    }
    
    /**
     * Gibt Vorhersagen für einen Typ zurück (Wichtig für Renderer!)
     */
    public List<PredictedFeature> getPredictions(String type) {
        return predictions.getOrDefault(type, new ArrayList<>());
    }
    
    /**
     * Fallback für Renderer (Alle holen)
     */
    public List<PredictedFeature> getPredictions() {
        List<PredictedFeature> all = new ArrayList<>();
        for (List<PredictedFeature> list : predictions.values()) {
            all.addAll(list);
        }
        return all;
    }

    public void clearPredictions() {
        predictions.clear();
    }
    public List<PredictedFeature> getAllPredictions() {
        List<PredictedFeature> all = new ArrayList<>();
        for (List<PredictedFeature> list : predictions.values()) {
            all.addAll(list);
        }
        return all;
    }
}