package com.xray.saltcracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generiert Vorhersagen für Strukturen und Erze basierend auf gecr ackten Salts
 */
public class PredictionEngine {
    
    private final Map<String, List<PredictedFeature>> predictions = new ConcurrentHashMap<>();
    
    private Long structureSalt = null;
    private Long oreSalt = null;
    private long worldSeed = 0;
    
    // Vorhersage-Klasse
    public static class PredictedFeature {
        public final String type;
        public final BlockPos position;
        public final double confidence;
        public final ChunkPos chunkPos;
        
        public PredictedFeature(String type, BlockPos position, double confidence) {
            this.type = type;
            this.position = position;
            this.confidence = confidence;
            this.chunkPos = new ChunkPos(position);
        }
        
        @Override
        public String toString() {
            return String.format("%s @ %s (%.0f%%)", type, position, confidence * 100);
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
            XRaySaltCracker.LOGGER.warn("Structure Salt oder World Seed nicht gesetzt");
            return;
        }
        
        XRaySaltCracker.LOGGER.info("Generiere Structure-Predictions...");
        
        // Generiere für Render-Distance
        int renderDistance = 16; // Chunks
        
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
            XRaySaltCracker.LOGGER.warn("Ore Salt oder World Seed nicht gesetzt");
            return;
        }
        
        XRaySaltCracker.LOGGER.info("Generiere Ore-Predictions...");
        
        int renderDistance = 16;
        
        // Verschiedene Erz-Typen
        predictions.put("diamond", generateOres("diamond", renderDistance, -64, -16));
        predictions.put("emerald", generateOres("emerald", renderDistance, -16, 256));
        predictions.put("gold", generateOres("gold", renderDistance, -64, 32));
        
        XRaySaltCracker.LOGGER.info("Ore-Predictions generiert");
    }
    
    /**
     * Generiert Buried Treasure Vorhersagen
     */
    private List<PredictedFeature> generateBuriedTreasures(int radiusChunks) {
        List<PredictedFeature> features = new ArrayList<>();
        
        // Spieler-Position (vereinfacht: 0,0)
        int playerChunkX = 0;
        int playerChunkZ = 0;
        
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                // Berechne ob hier ein Treasure spawnt
                long chunkSeed = getStructureChunkSeed(chunkX, chunkZ);
                Random rand = new Random(chunkSeed);
                
                if (rand.nextInt(100) == 0) { // 1% Chance (vereinfacht)
                    // Treasure spawnt bei ca. Y=40-60
                    int x = chunkX * 16 + rand.nextInt(16);
                    int z = chunkZ * 16 + rand.nextInt(16);
                    int y = 40 + rand.nextInt(20);
                    
                    features.add(new PredictedFeature(
                        "Buried Treasure",
                        new BlockPos(x, y, z),
                        0.95 // 95% Confidence
                    ));
                }
            }
        }
        
        return features;
    }
    
    /**
     * Generiert Erz-Vorhersagen
     */
    private List<PredictedFeature> generateOres(String oreType, int radiusChunks, 
                                                 int minY, int maxY) {
        List<PredictedFeature> features = new ArrayList<>();
        
        int playerChunkX = 0;
        int playerChunkZ = 0;
        
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                // Generiere Erze für diesen Chunk
                features.addAll(generateOresInChunk(oreType, chunkX, chunkZ, minY, maxY));
            }
        }
        
        return features;
    }
    
    /**
     * Generiert Erze in einem bestimmten Chunk
     */
    private List<PredictedFeature> generateOresInChunk(String oreType, int chunkX, int chunkZ,
                                                        int minY, int maxY) {
        List<PredictedFeature> ores = new ArrayList<>();
        
        long chunkSeed = getOreChunkSeed(chunkX, chunkZ);
        Random rand = new Random(chunkSeed);
        
        // Anzahl der Veins pro Chunk (abhängig vom Erz-Typ)
        int veinsPerChunk = getVeinsPerChunk(oreType);
        
        for (int i = 0; i < veinsPerChunk; i++) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);
            int y = minY + rand.nextInt(maxY - minY);
            
            // Vein-Größe
            int veinSize = getVeinSize(oreType, rand);
            
            // Generiere Blöcke im Vein
            for (int v = 0; v < veinSize; v++) {
                int offsetX = rand.nextInt(3) - 1;
                int offsetY = rand.nextInt(3) - 1;
                int offsetZ = rand.nextInt(3) - 1;
                
                BlockPos pos = new BlockPos(x + offsetX, y + offsetY, z + offsetZ);
                
                ores.add(new PredictedFeature(
                    oreType.substring(0, 1).toUpperCase() + oreType.substring(1) + " Ore",
                    pos,
                    0.75 // 75% Confidence (Erze sind schwieriger)
                ));
            }
        }
        
        return ores;
    }
    
    /**
     * Chunk-Seed für Strukturen
     */
    private long getStructureChunkSeed(int chunkX, int chunkZ) {
        long seed = worldSeed ^ structureSalt;
        long regionX = chunkX >> 4;
        long regionZ = chunkZ >> 4;
        return seed ^ ((regionX ^ regionZ) * 0x5DEECE66DL);
    }
    
    /**
     * Chunk-Seed für Erze
     */
    private long getOreChunkSeed(int chunkX, int chunkZ) {
        long seed = worldSeed ^ oreSalt;
        return seed ^ (chunkX * 341873128712L + chunkZ * 132897987541L);
    }
    
    /**
     * Veins pro Chunk (abhängig vom Erz-Typ)
     */
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
     * Vein-Größe (abhängig vom Erz-Typ)
     */
    private int getVeinSize(String oreType, Random rand) {
        return switch (oreType.toLowerCase()) {
            case "diamond" -> 2 + rand.nextInt(6); // 2-8
            case "emerald" -> 1 + rand.nextInt(3); // 1-3
            case "gold" -> 3 + rand.nextInt(6); // 3-9
            default -> 4 + rand.nextInt(8); // 4-12
        };
    }
    
    /**
     * Gibt alle Vorhersagen zurück
     */
    public Map<String, List<PredictedFeature>> getAllPredictions() {
        return new HashMap<>(predictions);
    }
    
    /**
     * Gibt Vorhersagen für einen Typ zurück
     */
    public List<PredictedFeature> getPredictions(String type) {
        return predictions.getOrDefault(type, new ArrayList<>());
    }
    
    /**
     * Löscht alle Vorhersagen
     */
    public void clearPredictions() {
        predictions.clear();
    }
}