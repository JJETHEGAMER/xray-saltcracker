package com.xray.saltcracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataCollector {
    
    // Die Datenpunkt-Klasse muss genau diese Felder haben, damit ConfigManager & Solver funktionieren
    public static class DataPoint {
        public final BlockPos position;
        public final ChunkPos chunkPos;
        public final String type;
        public final long timestamp;
        public boolean validated = false;

        public DataPoint(BlockPos pos, String type) {
            this.position = pos;
            this.chunkPos = new ChunkPos(pos);
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, List<DataPoint>> collectedData = new HashMap<>();

    /**
     * Wird vom Mixin aufgerufen, um neu geladene Chunks zu scannen.
     */
    public void scanChunk(WorldChunk chunk) {
        // Hier würde die Logik zum Scannen von Blöcken/Strukturen implementiert werden
    }

    /**
     * Methode für den ConfigManager zum Laden gespeicherter Daten.
     */
    public void addManualEntry(String category, String type, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        collectedData.computeIfAbsent(category, k -> new ArrayList<>()).add(new DataPoint(pos, type));
    }

    /**
     * Gibt alle Daten zurück (wichtig für den ConfigManager beim Speichern).
     */
    public Map<String, List<DataPoint>> getAllData() {
        return collectedData;
    }

    /**
     * Filtert Datenpunkte nach Typ (z.B. "structure").
     */
    public List<DataPoint> getDataPointsByType(String type) {
        // Sucht in allen Kategorien nach dem Typ
        return collectedData.values().stream()
                .flatMap(List::stream)
                .filter(dp -> dp.type.equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    /**
     * Hilfsmethode für die GUI-Statistik.
     */
    public int getDataCount(String category) {
        return collectedData.getOrDefault(category, new ArrayList<>()).size();
    }

    public int size() {
        return collectedData.values().stream().mapToInt(List::size).sum();
    }

    public void clear() {
        collectedData.clear();
    }

    public void clearAllData() {
        clear();
    }

    /**
     * Dummy für den Export-Button in der GUI.
     */
    public String exportAsJson() {
        return "{ \"points\": " + size() + " }";
    }
}