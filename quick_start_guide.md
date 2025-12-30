# 🚀 Quick Start Guide - XRay Salt Cracker

## 📋 Checkliste vor dem Start

- [ ] Fabric Loader installiert
- [ ] Java 17+ installiert
- [ ] World Seed bekannt
- [ ] Mindestens 5 Buried Treasures gefunden

---

## ⚡ 5-Minuten-Setup

### 1. Installation
```bash
# Mod bauen
./gradlew build

# JAR kopieren
cp build/libs/xray-saltcracker-1.0.0.jar ~/.minecraft/mods/
```

### 2. Ersten Start
1. Minecraft starten (Fabric-Profile)
2. Welt betreten
3. **Taste `X` drücken**
4. GUI öffnet sich ✅

### 3. World Seed eingeben
```
Beispiel-Seed: -4172144997902289642
```
- Seed in Textfeld eingeben
- "Seed Setzen" klicken
- Mod aktivieren (Toggle auf "AN")

### 4. Daten sammeln

**Automatisch (empfohlen):**
- Einfach normal spielen
- Mod scannt automatisch alle geladenen Chunks
- Fortschritt im GUI sichtbar

**Manuell:**
```
GUI → "Manuell Hinzufügen"
- Typ: Buried Treasure
- X/Y/Z Koordinaten eingeben
```

### 5. Salt cracken

Sobald **5+ Buried Treasures** gefunden:
```
GUI → "Cracke Structure Salt"
```

**Erwartete Dauer:**
- 4-Core CPU: ~30 Min
- 8-Core CPU: ~15 Min
- 16-Core CPU: ~8 Min

**Was passiert:**
```
[20:15:23] Starte Brute-Force für 8 Datenpunkte...
[20:15:45] Progress: 12.5%
[20:30:12] Progress: 75.3%
[20:45:01] Perfect Match gefunden: Salt = 123456789
[20:45:02] Structure Salt: 123456789 (Konfidenz: 0.95)
```

### 6. ESP nutzen

Salt automatisch gesetzt → ESP aktiv! 🎉

**Sichtbare Features:**
- 🟡 Buried Treasures (Gold)
- 🔵 Diamanten (Cyan) - wenn Ore Salt gecrackt
- 🟢 Smaragde (Grün) - wenn Ore Salt gecrackt

---

## 🎯 Tipps für Anfänger

### Wo finde ich Buried Treasures?

**Methode 1: Schatzkarten**
- Schiffswracks durchsuchen
- Schatzkarten finden
- X markiert die Stelle

**Methode 2: Zufälliges Graben**
- Strände absuchen
- Bei Y=40-60 graben
- Kisten sind meist unter Sand/Kies

**Methode 3: Andere Spieler beobachten**
- Auf Multiplayer-Servern
- Wenn du siehst wo andere graben
- Position notieren

### Wie erkenne ich gute Server?

**✅ Funktioniert gut:**
- Vanilla-Server
- Standard Paper/Spigot
- Server ohne Custom-Worldgen

**❌ Funktioniert schlecht:**
- Modded-Server (Forge/Fabric)
- Server mit Custom-Worldgen-Plugins
- Skyblock/Oneblock-Server

### Wie vermeide ich Detection?

**Do's:**
- ✅ Sammle zuerst genug Daten
- ✅ Finde nicht JEDEN Diamanten
- ✅ Grabe realistische Muster
- ✅ Variiere deine Routen

**Don'ts:**
- ❌ Geradewegs zu jedem Erz graben
- ❌ Alle Erze in kurzer Zeit finden
- ❌ Perfekte Strip-Mine-Patterns
- ❌ Immer die beste Route nehmen

---

## 🔧 Troubleshooting Express

### Problem: GUI öffnet nicht
```bash
# Lösung 1: Keybinding prüfen
Optionen → Steuerung → XRay Salt Cracker

# Lösung 2: Mod aktiviert?
logs/latest.log → "XRay Salt Cracker initialisiert"

# Lösung 3: Fabric API installiert?
mods/ → fabric-api-*.jar vorhanden?
```

### Problem: Kein Salt gefunden
```bash
# Checkliste:
1. World Seed korrekt? → Überprüfe mit /seed
2. Genug Daten? → Min. 5 Treasures
3. Server Custom-Gen? → Teste auf eigenem Server

# Lösung:
- Sammle 10+ Datenpunkte
- Teste Salt-Range erhöhen (Code-Anpassung)
```

### Problem: ESP zeigt nichts
```bash
# Checkliste:
1. Salt gefunden? → Im GUI angezeigt?
2. Features aktiviert? → Checkboxen im GUI
3. In Render-Distance? → Max. 16 Chunks
4. Mod aktiviert? → Toggle auf "AN"

# Debug:
logs/latest.log → "Generiert: X Buried Treasures"
```

### Problem: Predictions falsch
```bash
# Mögliche Ursachen:
1. Anti-Xray → Erze erst beim Graben sichtbar (normal!)
2. Falscher Salt → Confidence < 80%? Mehr Daten!
3. Custom-Gen → Server nutzt modifizierte Generation

# Test:
- Grabe zu vorhergesagter Position
- Wenn Erz da ist → Alles gut!
- Wenn nicht → Salt überprüfen
```

---

## 📊 Performance-Tuning

### Für langsame PCs

```java
// In SaltSolver.java ändern:
long testRange = 100_000_000L; // Statt 1_000_000_000L

// Scannt nur 100M Salts (schneller, aber weniger genau)
```

### Für schnelle PCs

```java
// In SaltSolver.java ändern:
long testRange = 4_294_967_296L; // Volle 2^32 Range

// Scannt alle möglichen Salts (langsamer, aber 100% genau)
```

### Chunk-Scan-Frequenz

```java
// In ChunkLoadMixin.java ändern:
for (int dx = -3; dx <= 3; dx++) { // Statt -1 bis 1
    for (int dz = -3; dz <= 3; dz++) {
        // Scannt 7x7 Chunks (mehr Daten, aber laggy)
    }
}
```

---

## 🎓 Weiterführende Tutorials

### Tutorial 1: Ore Salt Cracking
```
1. Sammle 50+ Diamant-Positionen
2. Exportiere Daten: GUI → "Daten Exportieren"
3. Analysiere Pattern manuell (TODO: Auto-Solver)
4. Teste verschiedene Salt-Werte
5. Validiere mit neuen Chunks
```

### Tutorial 2: Custom-Server anpassen
```java
// Wenn Server anderen Salt-Algorithmus nutzt:

// 1. Reverse-Engineer Server-Code
// 2. Passe getStructureChunkSeed() an
// 3. Teste mit bekannten Positionen
// 4. Iteriere bis Match
```

### Tutorial 3: Multi-Server-Support
```java
// Speichere Salts pro Server:

Map<String, ServerConfig> servers = new HashMap<>();
servers.put("hypixel.net", new ServerConfig(
    worldSeed, structureSalt, oreSalt
));
```

---

## 📚 Weiterführende Ressourcen

- **SASSA-Algorithmus**: [GitHub](https://github.com/example/sassa)
- **Minecraft Worldgen**: [Wiki](https://minecraft.wiki)
- **Fabric-Mod-Entwicklung**: [Docs](https://fabricmc.net/wiki)
- **ChunkRandom**: [Minecraft-Quellcode](https://github.com/example)

---

## 🤝 Community & Support

**Discord**: [Link]
**GitHub Issues**: [Link]
**Wiki**: [Link]

---

**Viel Erfolg beim Hacken! 💎🔥**

> *"Mit großer Macht kommt große Verantwortung"* - Uncle Ben (und dieser Mod)
