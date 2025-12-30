package com.xray.saltcracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Crackt Server-Salts durch Brute-Force-Matching
 */
public class SaltSolver {
    
    private final ExecutorService executor;
    private volatile boolean solving = false;
    private volatile double progress = 0.0;
    private String currentStatus = "Bereit";
    
    // Ergebnis-Callback
    public interface SaltFoundCallback {
        void onSaltFound(long salt, double confidence);
    }
    
    public SaltSolver() {
        // Thread-Pool für paralleles Solving
        int threads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(threads);
        XRaySaltCracker.LOGGER.info("SaltSolver initialisiert mit " + threads + " Threads");
    }
    
    /**
     * Startet das Cracking für Struktur-Salts
     */
    public void solveStructureSalt(long worldSeed, List<DataCollector.DataPoint> dataPoints, 
                                    SaltFoundCallback callback) {
        if (solving) {
            XRaySaltCracker.LOGGER.warn("Solver läuft bereits!");
            return;
        }
        
        if (dataPoints.size() < 5) {
            XRaySaltCracker.LOGGER.warn("Nicht genug Datenpunkte! Minimum: 5, Vorhanden: " + dataPoints.size());
            currentStatus = "Fehler: Zu wenig Datenpunkte (min. 5 benötigt)";
            return;
        }
        
        solving = true;
        progress = 0.0;
        currentStatus = "Starte Structure Salt Cracking...";
        
        CompletableFuture.runAsync(() -> {
            try {
                long foundSalt = crackStructureSalt(worldSeed, dataPoints);
                
                if (foundSalt != Long.MAX_VALUE) {
                    double confidence = validateSalt(worldSeed, foundSalt, dataPoints);
                    currentStatus = String.format("Salt gefunden! Konfidenz: %.1f%%", confidence * 100);
                    callback.onSaltFound(foundSalt, confidence);
                } else {
                    currentStatus = "Kein Salt gefunden - eventuell Custom World Gen?";
                }
            } catch (Exception e) {
                currentStatus = "Fehler: " + e.getMessage();
                XRaySaltCracker.LOGGER.error("Solver-Fehler", e);
            } finally {
                solving = false;
            }
        }, executor);
    }
    
    /**
     * Hauptalgorithmus für Structure Salt Cracking
     * Basiert auf dem SASSA-Ansatz
     */
    private long crackStructureSalt(long worldSeed, List<DataCollector.DataPoint> dataPoints) {
        XRaySaltCracker.LOGGER.info("Starte Brute-Force für " + dataPoints.size() + " Datenpunkte...");
        
        // Konvertiere zu Chunk-Positionen (Buried Treasure spawnt pro Chunk)
        Set<ChunkPos> observedChunks = new HashSet<>();
        for (DataCollector.DataPoint dp : dataPoints) {
            observedChunks.add(dp.chunkPos);
        }
        
        AtomicLong bestSalt = new AtomicLong(Long.MAX_VALUE);
        AtomicLong bestMatches = new AtomicLong(0);
        
        // Salt-Range: -2^31 bis 2^31 (aber wir testen erst kleinere Ranges)
        long testRange = 1_000_000_000L; // 1 Milliarde für schnellen Test
        int numThreads = Runtime.getRuntime().availableProcessors();
        long saltPerThread = testRange / numThreads;
        
        currentStatus = "Teste " + testRange + " Salt-Werte...";
        
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            final long startSalt = -testRange/2 + (t * saltPerThread);
            final long endSalt = startSalt + saltPerThread;
            final int threadId = t;
            
            executor.submit(() -> {
                try {
                    long localBestSalt = Long.MAX_VALUE;
                    long localBestMatches = 0;
                    
                    for (long salt = startSalt; salt < endSalt; salt++) {
                        // Progress-Update alle 10000 Salts
                        if (salt % 10000 == 0) {
                            progress = (double)(salt - startSalt) / (endSalt - startSalt) / numThreads + 
                                      (double)threadId / numThreads;
                        }
                        
                        // Teste diesen Salt
                        long matches = countMatches(worldSeed, salt, observedChunks);
                        
                        if (matches > localBestMatches) {
                            localBestMatches = matches;
                            localBestSalt = salt;
                        }
                        
                        // Perfect Match gefunden?
                        if (matches == observedChunks.size()) {
                            synchronized (bestSalt) {
                                if (matches > bestMatches.get()) {
                                    bestMatches.set(matches);
                                    bestSalt.set(salt);
                                    XRaySaltCracker.LOGGER.info("Perfect Match gefunden: Salt = " + salt);
                                }
                            }
                            break;
                        }
                    }
                    
                    // Update global best
                    synchronized (bestSalt) {
                        if (localBestMatches > bestMatches.get()) {
                            bestMatches.set(localBestMatches);
                            bestSalt.set(localBestSalt);
                        }
                    }
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        progress = 1.0;
        
        if (bestMatches.get() >= observedChunks.size() * 0.8) { // 80% Match-Rate
            XRaySaltCracker.LOGGER.info("Salt gefunden mit " + bestMatches.get() + "/" + 
                                       observedChunks.size() + " Matches: " + bestSalt.get());
            return bestSalt.get();
        }
        
        return Long.MAX_VALUE;
    }
    
    /**
     * Zählt wie viele beobachtete Chunks mit diesem Salt matchen würden
     */
    private long countMatches(long worldSeed, long salt, Set<ChunkPos> observedChunks) {
        long matches = 0;
        
        for (ChunkPos observed : observedChunks) {
            // Simuliere Minecraft's Buried Treasure Generation
            long chunkSeed = getChunkSeed(worldSeed, salt, observed.x, observed.z);
            
            // Buried Treasure hat 1/100 Chance pro Chunk (vereinfacht)
            Random rand = new Random(chunkSeed);
            
            // Minecraft's Struktur-Check (vereinfacht)
            if (shouldGenerateStructure(rand)) {
                matches++;
            }
        }
        
        return matches;
    }
    
    /**
     * Berechnet Chunk-Seed wie Minecraft es tut
     */
    private long getChunkSeed(long worldSeed, long salt, int chunkX, int chunkZ) {
        // Minecraft's ChunkRandom Algorithmus (vereinfacht)
        long seed = worldSeed;
        seed ^= salt;
        
        // Region-Seed (für Strukturen)
        long regionX = chunkX >> 4;
        long regionZ = chunkZ >> 4;
        
        long regionSeed = (regionX ^ regionZ) * 0x5DEECE66DL;
        return seed ^ regionSeed;
    }
    
    /**
     * Struktur-Generations-Check
     */
    private boolean shouldGenerateStructure(Random rand) {
        // Buried Treasure: nextInt(0.01) == 0 (vereinfacht)
        return rand.nextInt(100) == 0;
    }
    
    /**
     * Validiert gefundenen Salt mit allen Datenpunkten
     */
    private double validateSalt(long worldSeed, long salt, List<DataCollector.DataPoint> dataPoints) {
        Set<ChunkPos> observedChunks = new HashSet<>();
        for (DataCollector.DataPoint dp : dataPoints) {
            observedChunks.add(dp.chunkPos);
        }
        
        long matches = countMatches(worldSeed, salt, observedChunks);
        return (double) matches / observedChunks.size();
    }
    
    /**
     * Status-Getter
     */
    public boolean isSolving() {
        return solving;
    }
    
    public double getProgress() {
        return progress;
    }
    
    public String getStatus() {
        return currentStatus;
    }
    
    /**
     * Bricht aktuelles Solving ab
     */
    public void cancel() {
        if (solving) {
            solving = false;
            currentStatus = "Abgebrochen";
        }
    }
    
    /**
     * Cleanup
     */
    public void shutdown() {
        executor.shutdown();
    }
}