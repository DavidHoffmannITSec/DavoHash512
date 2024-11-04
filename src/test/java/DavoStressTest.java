import org.example.DavoHash512;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class DavoStressTest {

    private static final int EXTREME_TEST_CASES = 25_000_000;  // höhere Anzahl an Testfällen
    private static final int EXTREME_ENTROPY_SAMPLE_SIZE = 5_000_000;
    private static final int MAX_BIT_FLIP_COUNT = 100;  // Maximale Anzahl an Bit-Änderungen

    // Extrem-zeitliche Konsistenzprüfung
    @Test
    public void testExtremeTimingConsistency() {
        String input = "timingExtremeTestInput";
        long[] durations = new long[100];  // Reduzierte Anzahl an Messungen

        for (int i = 0; i < durations.length; i++) {
            long startTime = System.nanoTime();
            DavoHash512.hash(input);
            durations[i] = System.nanoTime() - startTime;
        }

        long averageTime = calculateAverage(durations);
        for (long duration : durations) {
            assertTrue(Math.abs(duration - averageTime) < (averageTime * 0.1),
                    "Konsistenz verletzt: Unterschiedliche Zeiten für gleiche Eingabe erkannt.");
        }
    }


    // Extreme Zufalls- und Entropietests
    @Test
    public void testExtremeEntropyRandomness() {
        SecureRandom random = new SecureRandom();
        Set<String> hashSet = ConcurrentHashMap.newKeySet();  // Thread-sicher

        for (int i = 0; i < 1_000_000; i++) {  // Kleinere Testanzahl
            byte[] randomBytes = new byte[32];  // Reduzierte Größe
            random.nextBytes(randomBytes);
            String hash = DavoHash512.hash(new String(randomBytes, StandardCharsets.UTF_8));

            assertTrue(hashSet.add(hash), "Kollision bei extremem Entropietest gefunden.");
        }
    }


    // Extreme Bit-Änderungstests mit komplexen Mustern
    @Test
    public void testExtremeBitFlipsCollisionResistance() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(32);
        String input = "extremeCollisionTestInput";
        String hash1 = DavoHash512.hash(input);
        SecureRandom random = new SecureRandom();

        try {
            for (int bitFlips = 1; bitFlips <= 50; bitFlips++) {  // Kleinere Anzahl
                final int currentFlips = bitFlips;
                executor.submit(() -> {
                    char[] chars = input.toCharArray();
                    for (int i = 0; i < currentFlips; i++) {
                        int index = random.nextInt(chars.length);
                        chars[index] = (char) (chars[index] ^ (1 << random.nextInt(8)));
                    }
                    String modifiedHash = DavoHash512.hash(new String(chars));
                    assertTrue(calculateBitDifference(hash1, modifiedHash) > (hash1.length() * 4 * 0.56),
                            "Avalanche-Effekt-Test fehlgeschlagen.");
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.MINUTES);
        }
    }


    // Extrem-Belastungstests für Multi-Threading
    @Test
    public void testExtremeMultiThreadingStress() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(64);  // Reduzierte Thread-Anzahl
        int testCases = 1_000_000;  // Reduzierte Testanzahl

        try {
            for (int i = 0; i < testCases; i++) {
                final int index = i;
                executor.submit(() -> {
                    String input = "multiThreadTest" + index;
                    String hash = DavoHash512.hash(input);
                    assertNotNull(hash, "Hash sollte nicht null sein.");
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.MINUTES);
        }
    }


    // Kompressionstest für extrem kleine und extrem lange Eingaben
    @Test
    public void testExtremeCompressionStrength() {
        String smallInput = "a";
        String largeInput = "y".repeat(1_000_000_000); // 2 GB

        String smallHash = DavoHash512.hash(smallInput);
        String largeHash = DavoHash512.hash(largeInput);

        assertNotNull(smallHash, "Hash für extrem kleine Eingabe sollte nicht null sein.");
        assertNotNull(largeHash, "Hash für extrem große Eingabe sollte nicht null sein.");
        assertNotEquals(smallHash, largeHash, "Extrem kleine und große Eingaben sollten unterschiedliche Hashes erzeugen.");
    }

    // Initialisierungswert-Beständigkeitstest bei ähnlichen Eingaben
    @Test
    public void testExtremeInitializationConsistency() {
        String input1 = "initializationExtremeTest";
        String input2 = "InitializationExtremeTest";

        String hash1 = DavoHash512.hash(input1);
        String hash2 = DavoHash512.hash(input2);

        assertNotEquals(hash1, hash2, "Verschiedene Eingaben sollten unterschiedliche Hashes ergeben.");
    }

    // Speicherverwaltung bei extrem häufigem Gebrauch
    @Test
    public void testExtremeGarbageCollectionEfficiency() {
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int i = 0; i < 1_000_000; i++) {  // Reduzierte Iterationsanzahl
            String hash = DavoHash512.hash("gcTest" + i);
            assertNotNull(hash, "Hash sollte nicht null sein.");
        }

        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        assertTrue(memoryAfter < memoryBefore * 1.2, "Speicherleck erkannt.");
    }


    // Test auf Resilienz gegenüber extrem fehlerhaften Eingaben
    @Test
    public void testExtremeFaultyInputResilience() {
        String[] faultyInputs = {
                "", null, "\u0000", "a\u0000b", " ", "\t\n\r", "\uFFFF\uFFFE", "Null\0Byte", "\uD800\uDC00", "\uDBFF\uDFFF",
                "\uD83D\uDE00".repeat(500_000), "\uD800\uDC00\uD83D\uDE00\uD800\uDC00", "特殊字符".repeat(1000)
        };

        for (String input : faultyInputs) {
            try {
                String hash = DavoHash512.hash(input);
                assertNotNull(hash, "Hash sollte für extrem fehlerhafte Eingaben nicht null sein.");
            } catch (Exception e) {
                fail("Hashing von extrem fehlerhaften Eingaben sollte keine Ausnahme auslösen.");
            }
        }
    }

    // Helper zur Durchschnittsberechnung
    private long calculateAverage(long[] times) {
        long sum = 0;
        for (long time : times) sum += time;
        return sum / times.length;
    }

    // Helper zur Berechnung der Bitunterschiede zwischen zwei Hashes
    private int calculateBitDifference(String hash1, String hash2) {
        byte[] bytes1 = hash1.getBytes();
        byte[] bytes2 = hash2.getBytes();

        int differences = 0;
        for (int i = 0; i < bytes1.length; i++) {
            differences += Integer.bitCount(bytes1[i] ^ bytes2[i]);
        }
        return differences;
    }

}