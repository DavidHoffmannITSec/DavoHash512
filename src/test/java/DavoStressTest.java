import org.example.DavoHash512;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class DavoStressTest {

    private static final int EXTREME_TEST_CASES = 25_000_000;  // höhere Anzahl an Testfällen
    private static final int EXTREME_ENTROPY_SAMPLE_SIZE = 5_000_000;
    private static final int MAX_BIT_FLIP_COUNT = 100;  // Maximale Anzahl an Bit-Änderungen

    // Extrem-zeitliche Konsistenzprüfung
    @Test
    public void testExtremeTimingConsistency() {
        String input = "timingExtremeTestInput";
        long[] durations = new long[500];

        for (int i = 0; i < durations.length; i++) {
            long startTime = System.nanoTime();
            DavoHash512.hash(input);
            durations[i] = System.nanoTime() - startTime;
        }

        long averageTime = calculateAverage(durations);
        for (long duration : durations) {
            assertTrue(Math.abs(duration - averageTime) < (averageTime * 0.05),
                    "Extrem-Konsistenz verletzt: Unterschiedliche Zeiten für gleiche Eingabe erkannt.");
        }
    }

    // Extreme Zufalls- und Entropietests
    @Test
    public void testExtremeEntropyRandomness() {
        SecureRandom random = new SecureRandom();
        Set<String> hashSet = new HashSet<>();

        for (int i = 0; i < EXTREME_ENTROPY_SAMPLE_SIZE; i++) {
            byte[] randomBytes = new byte[64];  // größere Eingaben für Entropietests
            random.nextBytes(randomBytes);
            String hash = DavoHash512.hash(new String(randomBytes, StandardCharsets.UTF_8));

            assertTrue(hashSet.add(hash), "Kollision bei extremem Entropietest gefunden.");
        }
    }

    // Extreme Bit-Änderungstests mit komplexen Mustern
    @Test
    public void testExtremeBitFlipsCollisionResistance() throws InterruptedException, ExecutionException {
        String input = "extremeCollisionTestInput";
        String hash1 = DavoHash512.hash(input);
        ExecutorService executor = Executors.newFixedThreadPool(32);

        try {
            List<Callable<Boolean>> tasks = createBitFlipTasks(input, hash1, MAX_BIT_FLIP_COUNT);

            // Execute tasks and validate results
            List<Future<Boolean>> results = executor.invokeAll(tasks);
            for (Future<Boolean> result : results) {
                assertTrue(result.get(), "Extreme Avalanche Effect Test failed.");
            }
        } finally {
            executor.shutdown();
            boolean terminated = executor.awaitTermination(120, TimeUnit.SECONDS);
            if (!terminated) {
                System.err.println("ExecutorService did not terminate in time.");
            }
        }
    }

    private List<Callable<Boolean>> createBitFlipTasks(String input, String hash1, int maxBitFlipCount) {
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int bitFlips = 1; bitFlips <= maxBitFlipCount; bitFlips++) {
            final int currentBitFlips = bitFlips;
            tasks.add(() -> {
                char[] chars = input.toCharArray();
                SecureRandom random = new SecureRandom();

                for (int i = 0; i < currentBitFlips; i++) {
                    int index = random.nextInt(chars.length);
                    chars[index] = (char) (chars[index] ^ (1 << random.nextInt(8)));
                }

                String modifiedInput = new String(chars);
                String hash2 = DavoHash512.hash(modifiedInput);

                int difference = calculateBitDifference(hash1, hash2);
                return difference > (hash1.length() * 4 * 0.57);
            });
        }
        return tasks;
    }

    // Extrem-Belastungstests für Multi-Threading
    @Test
    public void testExtremeMultiThreadingStress() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(256);

        try {
            for (int i = 0; i < EXTREME_TEST_CASES; i++) {
                final int index = i;
                executor.submit(() -> {
                    String input = "extremeThreadingTest" + index;
                    String hash = DavoHash512.hash(input);
                    assertNotNull(hash, "Hash sollte nicht null sein.");
                });
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(180, TimeUnit.MINUTES)) {
                System.err.println("Executor konnte nicht rechtzeitig beendet werden.");
            }
        }

        assertTrue(executor.isTerminated(), "Extreme Multi-Threading-Belastungstest nicht abgeschlossen.");
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

        for (int i = 0; i < 10_000_000; i++) {
            String hash = DavoHash512.hash("gcExtremeTest" + i);
            assertNotNull(hash, "Hash sollte nicht null sein.");
        }

        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        assertTrue(memoryAfter < memoryBefore * 1.2, "Speicherleck erkannt: Speicherverbrauch ist extrem hoch.");
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