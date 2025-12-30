package com.xray.saltcracker;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
// WICHTIG: Wir importieren direkt OpenGL, um Mapping-Probleme zu umgehen
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESPRenderer {
    
    private final MinecraftClient client;
    private final Map<String, Boolean> featureEnabled = new HashMap<>();
    private final Map<String, int[]> featureColors = new HashMap<>();
    
    public ESPRenderer() {
        this.client = MinecraftClient.getInstance();
        
        featureEnabled.put("Buried Treasure", true);
        featureEnabled.put("Diamond Ore", true);
        featureEnabled.put("Emerald Ore", true);
        featureEnabled.put("Gold Ore", false);
        featureEnabled.put("Iron Ore", false);
        
        featureColors.put("Buried Treasure", new int[]{255, 215, 0, 200});
        featureColors.put("Diamond Ore", new int[]{0, 255, 255, 200});
        featureColors.put("Emerald Ore", new int[]{0, 255, 0, 200});
        featureColors.put("Gold Ore", new int[]{255, 215, 0, 150});
        featureColors.put("Iron Ore", new int[]{192, 192, 192, 150});
        featureColors.put("Coal Ore", new int[]{64, 64, 64, 150});
    }
    
    public void render(WorldRenderContext context) {
        if (client.player == null || client.world == null) return;
        
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // --- PRO FIX: Direct OpenGL Calls ---
        // Umgeht "Symbol not found" bei RenderSystem
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // X-RAY TRICK: Depth Test deaktivieren (GL_ALWAYS = 519)
        // Wir nutzen GL11 direkt, da RenderSystem.depthFunc in 1.21.8 Mappings fehlt
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        GL11.glDepthMask(false);

        // HINWEIS: Wir haben 'setShader' entfernt. 
        // Grund: Wir nutzen unten 'RenderLayer.getLines()', welches seinen eigenen Shader mitbringt.
        // Das manuelle Setzen war redundant und verursachte den Fehler.

        PredictionEngine engine = XRaySaltCracker.getInstance().getPredictionEngine();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());
        
        renderPredictions(matrices, buffer, engine.getPredictions("buried_treasure"), "Buried Treasure");
        renderPredictions(matrices, buffer, engine.getPredictions("diamond"), "Diamond Ore");
        renderPredictions(matrices, buffer, engine.getPredictions("emerald"), "Emerald Ore");
        renderPredictions(matrices, buffer, engine.getPredictions("gold"), "Gold Ore");
        
        // Zeichnen erzwingen, solange GL States aktiv sind
        consumers.draw();
        
        // --- CLEANUP ---
        // Zustand sauber zurücksetzen, sonst glitchen andere GUI-Elemente
        GL11.glDepthFunc(GL11.GL_LEQUAL); // Standard (515)
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        
        matrices.pop();
    }
    
    private void renderPredictions(MatrixStack matrices, VertexConsumer buffer,
                                   List<PredictionEngine.PredictedFeature> predictions,
                                   String featureName) {
        if (!featureEnabled.getOrDefault(featureName, false) || predictions.isEmpty()) return;
        
        int[] color = featureColors.getOrDefault(featureName, new int[]{255, 255, 255, 200});
        float r = color[0] / 255f, g = color[1] / 255f, b = color[2] / 255f, a = color[3] / 255f;
        
        for (PredictionEngine.PredictedFeature feature : predictions) {
            BlockPos pos = feature.position;
            if (!isInRenderDistance(pos)) continue;
            
            drawBox(buffer, matrices.peek().getPositionMatrix(), pos, r, g, b, a);
        }
    }
    
    private void drawBox(VertexConsumer buffer, Matrix4f matrix, BlockPos pos,
                        float r, float g, float b, float a) {
        float x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ();
        float x2 = x1 + 1, y2 = y1 + 1, z2 = z1 + 1;
        
        drawLine(buffer, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLine(buffer, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLine(buffer, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLine(buffer, matrix, x1, y1, z2, x1, y2, z1, r, g, b, a);
        
        drawLine(buffer, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLine(buffer, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLine(buffer, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLine(buffer, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);
        
        drawLine(buffer, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLine(buffer, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLine(buffer, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLine(buffer, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }
    
    private void drawLine(VertexConsumer buffer, Matrix4f matrix,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
    
    private boolean isInRenderDistance(BlockPos pos) {
        if (client.player == null) return false;
        double maxDistance = client.options.getViewDistance().getValue() * 16;
        return client.player.squaredDistanceTo(pos.toCenterPos()) < maxDistance * maxDistance;
    }

    public void setFeatureEnabled(String name, boolean enabled) { featureEnabled.put(name, enabled); }
    public boolean isFeatureEnabled(String name) { return featureEnabled.getOrDefault(name, false); }
    public void setFeatureColor(String featureName, int r, int g, int b, int a) {
        this.featureColors.put(featureName, new int[]{r, g, b, a});
    }
}