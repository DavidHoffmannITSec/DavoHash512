import org.example.DavoHash512;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class DavoStressTest {

    private static final int EXTREME_TEST_CASES = 25_000_000;
    private static final int EXTREME_ENTROPY_SAMPLE_SIZE = 5_000_000;
    private static final int MAX_BIT_FLIP_COUNT = 100;

    private static final int TIME_LIMIT_NS = 5_000_000; // 5 ms in Nanosekunden
    private static final int NUM_ITERATIONS = 1_000;
    private static final int NUM_INPUTS = 100; // Anzahl zufälliger Eingaben für bessere Erkennung
    private static final Random RANDOM = new Random();

    @Test
    public void testTimingAttackVulnerability() {
        long totalVariance = 0;

        for (int i = 0; i < NUM_INPUTS; i++) {
            byte[] input1 = generateRandomInput();
            byte[] input2 = generateRandomInput();

            long timeInput1 = measureHashTime(input1);
            long timeInput2 = measureHashTime(input2);

            long timeDifference = Math.abs(timeInput1 - timeInput2);
            totalVariance += timeDifference;
        }

        long averageVariance = totalVariance / NUM_INPUTS;
        assertTrue(averageVariance < TIME_LIMIT_NS, "Timing-Angriff möglich: Durchschnittliche Zeitdifferenz zu groß");
    }

    private byte[] generateRandomInput() {
        byte[] input = new byte[16];
        RANDOM.nextBytes(input);
        return input;
    }

    private long measureHashTime(byte[] input) {
        long totalTime = 0;

        for (int i = 0; i < NUM_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            DavoHash512.hash(new String(input));
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        return totalTime / NUM_ITERATIONS;
    }

    @Test
    public void testExtremeEntropyRandomness() {
        SecureRandom random = new SecureRandom();
        Set<String> hashSet = new HashSet<>();

        for (int i = 0; i < EXTREME_ENTROPY_SAMPLE_SIZE; i++) {
            byte[] randomBytes = new byte[64];
            random.nextBytes(randomBytes);
            byte[] hashBytes = DavoHash512.hash(new String(randomBytes, StandardCharsets.UTF_8));
            String hashHex = DavoHash512.bytesToHex(hashBytes);

            assertTrue(hashSet.add(hashHex), "Kollision bei extremem Entropietest gefunden.");
        }
    }

    @Test
    public void testExtremeBitFlipsCollisionResistance() throws InterruptedException, ExecutionException {
        String input = "extremeCollisionTestInput";
        byte[] hash1 = DavoHash512.hash(input);
        ExecutorService executor = Executors.newFixedThreadPool(32);

        try {
            List<Callable<Boolean>> tasks = createBitFlipTasks(input, hash1);

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

    private List<Callable<Boolean>> createBitFlipTasks(String input, byte[] hash1) {
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int bitFlips = 1; bitFlips <= DavoStressTest.MAX_BIT_FLIP_COUNT; bitFlips++) {
            final int currentBitFlips = bitFlips;
            tasks.add(() -> {
                char[] chars = input.toCharArray();
                SecureRandom random = new SecureRandom();

                for (int i = 0; i < currentBitFlips; i++) {
                    int index = random.nextInt(chars.length);
                    chars[index] = (char) (chars[index] ^ (1 << random.nextInt(8)));
                }

                String modifiedInput = new String(chars);
                byte[] hash2 = DavoHash512.hash(modifiedInput);

                int difference = calculateBitDifference(hash1, hash2);
                return difference > (hash1.length * 8 * 0.57);
            });
        }
        return tasks;
    }

    @Test
    public void testExtremeMultiThreadingStress() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(128);

        try {
            for (int i = 0; i < EXTREME_TEST_CASES; i++) {
                final int index = i;
                executor.submit(() -> {
                    String input = "extremeThreadingTest" + index;
                    byte[] hashBytes = DavoHash512.hash(input);
                    assertNotNull(hashBytes, "Hash sollte nicht null sein.");
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

    @Test
    public void testExtremeCompressionStrength() {
        String smallInput = "a";
        String largeInput = "y".repeat(1_000_000_000); // 1 GB

        byte[] smallHash = DavoHash512.hash(smallInput);
        byte[] largeHash = DavoHash512.hash(largeInput);

        assertNotNull(smallHash, "Hash für extrem kleine Eingabe sollte nicht null sein.");
        assertNotNull(largeHash, "Hash für extrem große Eingabe sollte nicht null sein.");
        assertNotEquals(DavoHash512.bytesToHex(smallHash), DavoHash512.bytesToHex(largeHash), "Extrem kleine und große Eingaben sollten unterschiedliche Hashes erzeugen.");
    }

    @Test
    public void testExtremeInitializationConsistency() {
        String input1 = "initializationExtremeTest";
        String input2 = "InitializationExtremeTest";

        byte[] hash1 = DavoHash512.hash(input1);
        byte[] hash2 = DavoHash512.hash(input2);

        assertNotEquals(DavoHash512.bytesToHex(hash1), DavoHash512.bytesToHex(hash2), "Verschiedene Eingaben sollten unterschiedliche Hashes ergeben.");
    }

    @Test
    public void testExtremeGarbageCollectionEfficiency() {
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int i = 0; i < 10_000_000; i++) {
            byte[] hash = DavoHash512.hash("gcExtremeTest" + i);
            assertNotNull(hash, "Hash sollte nicht null sein.");
        }

        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        assertTrue(memoryAfter < memoryBefore * 1.2, "Speicherleck erkannt: Speicherverbrauch ist extrem hoch.");
    }

    @Test
    public void testExtremeFaultyInputResilience() {
        String[] faultyInputs = {
                "", null, "\u0000", "a\u0000b", " ", "\t\n\r", "\uFFFF\uFFFE", "Null\0Byte", "\uD800\uDC00", "\uDBFF\uDFFF",
                "\uD83D\uDE00".repeat(500_000), "\uD800\uDC00\uD83D\uDE00\uD800\uDC00", "特殊字符".repeat(1000)
        };

        for (String input : faultyInputs) {
            try {
                byte[] hash = DavoHash512.hash(input);
                assertNotNull(hash, "Hash sollte für extrem fehlerhafte Eingaben nicht null sein.");
            } catch (Exception e) {
                fail("Hashing von extrem fehlerhaften Eingaben sollte keine Ausnahme auslösen.");
            }
        }
    }

    private int calculateBitDifference(byte[] hash1, byte[] hash2) {
        int differences = 0;
        for (int i = 0; i < hash1.length; i++) {
            differences += Integer.bitCount(hash1[i] ^ hash2[i]);
        }
        return differences;
    }
}
