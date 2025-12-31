package com.xray.saltcracker;

import java.util.HashMap;
import java.util.Map;

public class ESPRenderer {

    // Speicher für die Einstellungen (damit GUI & Config funktionieren)
    private final Map<String, Boolean> featureToggles = new HashMap<>();

    public ESPRenderer() {
        // Standard-Werte setzen
        featureToggles.put("Buried Treasure", true);
        featureToggles.put("Diamond Ore", true);
        featureToggles.put("Gold Ore", true);
        featureToggles.put("Iron Ore", true);
        featureToggles.put("Emerald Ore", true);
        featureToggles.put("Desert Pyramid", true);
        featureToggles.put("Jungle Temple", true);
    }

    // --- WICHTIG: Die Dummy-Render-Methode ---
    // Sie existiert, tut aber NICHTS. Das verhindert den Crash beim Rendern.
    public void onRender(Object context) {
        // Safe Mode: Kein 3D Rendering Code hier.
    }

    // --- WICHTIG: Die Methoden für GUI & Config ---
    public boolean isFeatureEnabled(String feature) {
        return featureToggles.getOrDefault(feature, true);
    }

    public void setFeatureEnabled(String feature, boolean enabled) {
        featureToggles.put(feature, enabled);
    }
    
    // Falls ConfigManager Farben speichern will (Dummy)
    public void setFeatureColor(String feature, int r, int g, int b, int a) {
        // Wir speichern keine Farben im Safe Mode, aber die Methode muss existieren
    }
}