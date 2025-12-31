package com.xray.saltcracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataCollector {

    public static class DataPoint {
        public final String type;
        public final BlockPos pos; // Heißt jetzt 'pos' (früher 'position')
        public final ChunkPos chunkPos;
        public final long timestamp;
        public final boolean validated;

        public DataPoint(String type, BlockPos pos, boolean validated) {
            this.type = type;
            this.pos = pos;
            this.chunkPos = new ChunkPos(pos);
            this.timestamp = System.currentTimeMillis();
            this.validated = validated;
        }
    }

    private final Map<String, List<DataPoint>> collectedData = new ConcurrentHashMap<>();
    private final Set<Long> scannedChunks = Collections.synchronizedSet(new HashSet<>());
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public DataCollector() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        if (!XRaySaltCracker.getInstance().isEnabled()) return;

        ChunkPos currentChunkPos = client.player.getChunkPos();
        int radius = 2; 

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos pos = new ChunkPos(currentChunkPos.x + x, currentChunkPos.z + z);
                long chunkKey = pos.toLong();

                if (!scannedChunks.contains(chunkKey)) {
                    if (client.world.isChunkLoaded(pos.x, pos.z)) {
                        WorldChunk chunk = client.world.getChunk(pos.x, pos.z);
                        scanChunk(chunk);
                        scannedChunks.add(chunkKey);
                    }
                }
            }
        }
    }

    // MUSS PUBLIC SEIN FÜR MIXIN
    public void scanChunk(WorldChunk chunk) {
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ChestBlockEntity) {
                BlockPos pos = be.getPos();
                if (pos.getY() < 60) { 
                     addData("structure_buried_treasure", pos);
                }
            }
        }
    }
    
    public void addData(String type, BlockPos pos) {
        // Prüfen ob schon existiert
        List<DataPoint> list = collectedData.computeIfAbsent(type, k -> new ArrayList<>());
        for (DataPoint dp : list) {
            if (dp.pos.equals(pos)) return;
        }
        list.add(new DataPoint(type, pos, true));
        XRaySaltCracker.LOGGER.info("Gefunden: " + type + " bei " + pos.toShortString());
    }

    // Methode für ConfigManager
    public void addManualEntry(String type, String subType, int x, int y, int z) {
        String fullType = type + "_" + subType; // z.B. structure_buried_treasure
        if (subType == null || subType.isEmpty()) fullType = type;
        
        addData(fullType, new BlockPos(x, y, z));
    }

    public List<DataPoint> getDataPointsByType(String type) {
        return collectedData.getOrDefault(type, new ArrayList<>());
    }
    
    public int getDataCount(String type) {
        return getDataPointsByType(type).size();
    }

    public Map<String, List<DataPoint>> getAllData() {
        return collectedData;
    }

    public void clearAllData() {
        collectedData.clear();
        scannedChunks.clear();
    }

    public String exportAsJson() {
        return gson.toJson(collectedData);
    }
}