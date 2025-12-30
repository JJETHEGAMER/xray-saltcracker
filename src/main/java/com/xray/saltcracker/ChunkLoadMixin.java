package com.xray.saltcracker.mixin;

import com.xray.saltcracker.XRaySaltCracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin zum automatischen Scannen von Chunks beim Laden
 */
@Mixin(ClientWorld.class)
public class ChunkLoadMixin {
    
    /**
     * Hook in Chunk-Load-Event
     */
    @Inject(method = "addEntityPrivate", at = @At("HEAD"))
    private void onChunkLoad(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        // Nur wenn Mod aktiv ist
        XRaySaltCracker mod = XRaySaltCracker.getInstance();
        if (!mod.isEnabled()) return;
        
        // Scanne alle geladenen Chunks in der Nähe
        int playerChunkX = client.player.getChunkPos().x;
        int playerChunkZ = client.player.getChunkPos().z;
        
        // Scanne 3x3 Chunks um den Spieler
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                WorldChunk chunk = client.world.getChunk(chunkX, chunkZ);
                if (chunk != null && !chunk.isEmpty()) {
                    // Scanne diesen Chunk
                    scanChunkAsync(chunk);
                }
            }
        }
    }
    
    /**
     * Scannt Chunk asynchron (um Performance zu schonen)
     */
    private void scanChunkAsync(WorldChunk chunk) {
        // Führe Scan in separatem Thread aus
        new Thread(() -> {
            try {
                XRaySaltCracker.getInstance()
                    .getDataCollector()
                    .scanChunk(chunk);
            } catch (Exception e) {
                XRaySaltCracker.LOGGER.error("Fehler beim Chunk-Scan", e);
            }
        }, "ChunkScanner").start();
    }
}