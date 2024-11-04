import org.example.DavoHash512;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class HashTest {

    private static final int NUM_MAX_TEST_CASES = 2_000_000;
    private static final int BIRTHDAY_ATTACK_SAMPLE_SIZE = 1_000_000;
    private static final int MAX_BIT_FLIP_COUNT = 100;
    private static final int PARTIAL_MATCH_LENGTH = 64;

    @Test
    public void testPartialMatchAvoidance() {
        String input = "partialMatchTest";
        String targetHash = DavoHash512.hash(input);
        String partialTarget = targetHash.substring(0, PARTIAL_MATCH_LENGTH);

        boolean foundPartialMatch = false;
        for (int i = 0; i < 2_000_000; i++) {
            String randomInput = "test" + Math.random();
            String hash = DavoHash512.hash(randomInput);

            if (hash.startsWith(partialTarget)) {
                foundPartialMatch = true;
                break;
            }
        }
        assertFalse(foundPartialMatch, "Eine zufällige Eingabe erzeugte eine Hash-Teilübereinstimmung.");
    }

    @Test
    public void testEnhancedCollisionAvoidance() throws InterruptedException {
        Set<String> hashSet = ConcurrentHashMap.newKeySet(); // Thread-sicheres Set
        ExecutorService executor = Executors.newFixedThreadPool(32); // Reduzierte Anzahl an Threads
        List<String> inputs = new ArrayList<>(NUM_MAX_TEST_CASES);

        // Vorbereitung der Eingaben außerhalb der Threads
        for (int i = 0; i < NUM_MAX_TEST_CASES; i++) {
            inputs.add("complexInput" + i + (i * 9973)); // Verwenden einer deterministischen Berechnung für Variation
        }

        try {
            for (String input : inputs) {
                executor.submit(() -> {
                    String hash = DavoHash512.hash(input);
                    if (!hashSet.add(hash)) {
                        fail("Kollision erkannt: Zwei verschiedene Eingaben führten zum gleichen Hash.");
                    }
                });
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(120, TimeUnit.MINUTES)) {
                System.err.println("Executor wurde nicht rechtzeitig beendet.");
            }
        }

        assertTrue(executor.isTerminated(), "Die erweiterten Kollisionstests wurden nicht innerhalb der maximalen Zeit abgeschlossen.");
    }


    @Test
    public void testPerformanceOnUltraMaxInput() {
        final int maxInputSize = 1_000_000_000;
        String largeInput = "x".repeat(maxInputSize);

        long startTime = System.nanoTime();
        String hash = DavoHash512.hash(largeInput);
        long duration = System.nanoTime() - startTime;

        assertNotNull(hash, "Hash sollte für extrem große Eingaben nicht null sein.");
        assertTrue(duration < 300_000_000_000L, "Hashing extrem großer Eingaben sollte innerhalb von 300 Sekunden abgeschlossen sein.");
        System.out.println("Hashing-Zeit für 1 GB große Eingabe: " + duration / 1_000_000_000.0 + " Sekunden");
    }

    @Test
    public void testExtremeAvalancheEffect() {
        String input = "ExtremeAvalancheInput";
        String hash1 = DavoHash512.hash(input);
        Random random = new Random();
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int bitFlips = 1; bitFlips <= MAX_BIT_FLIP_COUNT * 5; bitFlips++) {
                final int currentBitFlips = bitFlips;
                executor.submit(() -> {
                    char[] chars = input.toCharArray();
                    for (int i = 0; i < currentBitFlips; i++) {
                        int index = random.nextInt(chars.length);
                        chars[index] = (char) (chars[index] ^ (1 << random.nextInt(7)));
                    }
                    String modifiedInput = new String(chars);
                    String hash2 = DavoHash512.hash(modifiedInput);
                    int difference = calculateBitDifference(hash1, hash2);
                    assertTrue(difference > (hash1.length() * 4 * 0.57), "Extremer Avalanche-Effekt-Test fehlgeschlagen.");
                });
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(120, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    @Test
    public void testExtremeBirthdayAttackResistance() {
        Set<String> hashSet = new HashSet<>();
        for (int i = 0; i < BIRTHDAY_ATTACK_SAMPLE_SIZE; i++) {
            String randomInput = "birthdayAttack" + i + Math.random();
            String hash = DavoHash512.hash(randomInput);
            assertTrue(hashSet.add(hash), "Kollision erkannt: Zwei zufällige Eingaben erzeugten denselben Hash.");
        }
    }

    @Test
    public void testExtremeSpecialCharactersHandling() throws InterruptedException {
        String[] specialInputs = {
                "😊", "你好", "🌍🚀", "🎉🎈🥳", "𐍈𐌰𐌹", "🔥".repeat(500_000),
                "\uDBFF\uDFFF", "\uD83D\uDE00\uD83D\uDE02\uD83E\uDD23", "ℜ𝔬𝔟𝔲𝔰𝔱", "❄️☃️🔥🌞🌬️"
        };

        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (String input : specialInputs) {
                executor.submit(() -> {
                    String hash = DavoHash512.hash(input);
                    assertNotNull(hash, "Hash sollte für Multibyte-Eingaben nicht null sein.");
                    assertFalse(hash.isEmpty(), "Hash sollte nicht leer sein.");
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(120, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testRapidSequentialHashing() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int i = 0; i < 100_000; i++) {
                final int index = i;
                executor.submit(() -> {
                    String input = "rapidTestInput" + index;
                    String hash = DavoHash512.hash(input);
                    assertNotNull(hash, "Hash sollte nicht null sein bei sequentieller Eingabe.");
                    assertFalse(hash.isEmpty(), "Hash sollte nicht leer sein.");
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(120, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testExtremeSlidingWindowsEffect() throws InterruptedException {
        String baseInput = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        Set<String> hashes = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int i = 0; i < baseInput.length() - 5; i++) {
                final int index = i;
                executor.submit(() -> {
                    String window = baseInput.substring(index, index + 6);
                    String hash = DavoHash512.hash(window);
                    synchronized (hashes) {
                        assertTrue(hashes.add(hash), "Sliding Windows Test fehlgeschlagen: Zwei unterschiedliche Fenster erzeugten denselben Hash.");
                    }
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(120, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testStackedLengthExtensionResistance() throws InterruptedException {
        String input = "secureBaseData";
        String hash1 = DavoHash512.hash(input);
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int i = 0; i < 10; i++) {
                final int index = i;
                executor.submit(() -> {
                    String extendedInput = input + "extension" + index;
                    String hash2 = DavoHash512.hash(extendedInput);
                    assertNotEquals(hash1, hash2, "Längenverlängerungsangriff erfolgreich.");
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(120, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testRandomRepeatedPatterns() throws InterruptedException {
        Random random = new Random();
        Set<String> hashSet = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int i = 0; i < NUM_MAX_TEST_CASES / 100; i++) {
                executor.submit(() -> {
                    StringBuilder inputBuilder = new StringBuilder();
                    for (int j = 0; j < 10; j++) {
                        inputBuilder.append((char) (random.nextInt(26) + 'a'));
                    }
                    String repeatedPattern = inputBuilder.toString().repeat(random.nextInt(5) + 1);
                    String randomInput = repeatedPattern + new StringBuilder(repeatedPattern).reverse();
                    String hash = DavoHash512.hash(randomInput);
                    synchronized (hashSet) {
                        assertTrue(hashSet.add(hash), "Kollision erkannt: Zwei zufällige Muster führten zum gleichen Hash.");
                    }
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(120, TimeUnit.SECONDS);
        }
    }

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
