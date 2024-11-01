import org.example.DavoHash;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
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
        long[] durations = new long[500];

        for (int i = 0; i < durations.length; i++) {
            long startTime = System.nanoTime();
            DavoHash.hash(input);
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
            String hash = DavoHash.hash(new String(randomBytes, StandardCharsets.UTF_8));

            assertTrue(hashSet.add(hash), "Kollision bei extremem Entropietest gefunden.");
        }
    }

    // Extreme Bit-Änderungstests mit komplexen Mustern
    @Test
    public void testExtremeBitFlipsCollisionResistance() {
        String input = "extremeCollisionTestInput";
        String hash1 = DavoHash.hash(input);

        for (int bitFlips = 1; bitFlips <= MAX_BIT_FLIP_COUNT; bitFlips++) {
            char[] chars = input.toCharArray();
            SecureRandom random = new SecureRandom();

            for (int i = 0; i < bitFlips; i++) {
                int index = random.nextInt(chars.length);
                chars[index] = (char) (chars[index] ^ (1 << random.nextInt(8)));
            }

            String modifiedInput = new String(chars);
            String hash2 = DavoHash.hash(modifiedInput);

            int difference = calculateBitDifference(hash1, hash2);
            assertTrue(difference > (hash1.length() * 4 * 0.56),
                    "Extremer Avalanche-Effekt-Test fehlgeschlagen mit " + bitFlips + " Bit-Änderungen.");
        }
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
                    String hash = DavoHash.hash(input);
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

        String smallHash = DavoHash.hash(smallInput);
        String largeHash = DavoHash.hash(largeInput);

        assertNotNull(smallHash, "Hash für extrem kleine Eingabe sollte nicht null sein.");
        assertNotNull(largeHash, "Hash für extrem große Eingabe sollte nicht null sein.");
        assertNotEquals(smallHash, largeHash, "Extrem kleine und große Eingaben sollten unterschiedliche Hashes erzeugen.");
    }

    // Initialisierungswert-Beständigkeitstest bei ähnlichen Eingaben
    @Test
    public void testExtremeInitializationConsistency() {
        String input1 = "initializationExtremeTest";
        String input2 = "InitializationExtremeTest";

        String hash1 = DavoHash.hash(input1);
        String hash2 = DavoHash.hash(input2);

        assertNotEquals(hash1, hash2, "Verschiedene Eingaben sollten unterschiedliche Hashes ergeben.");
    }

    // Speicherverwaltung bei extrem häufigem Gebrauch
    @Test
    public void testExtremeGarbageCollectionEfficiency() {
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int i = 0; i < 10_000_000; i++) {
            String hash = DavoHash.hash("gcExtremeTest" + i);
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
                String hash = DavoHash.hash(input);
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