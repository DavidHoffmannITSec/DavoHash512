import org.example.DavoHash512;
import org.junit.Test;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class HashTest {

    private static final int NUM_MAX_TEST_CASES = 10_000_000;
    private static final int BIRTHDAY_ATTACK_SAMPLE_SIZE = 2_000_000;
    private static final int MAX_BIT_FLIP_COUNT = 50;
    private static final int PARTIAL_MATCH_LENGTH = 32;

    @Test
    public void testPartialMatchAvoidance() {
        String input = "partialMatchTest";
        byte[] targetHashBytes = DavoHash512.hash(input);
        String targetHashHex = DavoHash512.bytesToHex(targetHashBytes);
        String partialTargetHex = targetHashHex.substring(0, PARTIAL_MATCH_LENGTH);

        boolean foundPartialMatch = false;
        for (int i = 0; i < 2_000_000; i++) {
            String randomInput = "test" + Math.random();
            byte[] hashBytes = DavoHash512.hash(randomInput);
            String hashHex = DavoHash512.bytesToHex(hashBytes);

            if (hashHex.startsWith(partialTargetHex)) {
                foundPartialMatch = true;
                break;
            }
        }
        assertFalse(foundPartialMatch, "Eine zufällige Eingabe erzeugte eine Hash-Teilübereinstimmung.");
    }

    @Test
    public void testEnhancedCollisionAvoidance() throws InterruptedException {
        Set<String> hashSet = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int i = 0; i < NUM_MAX_TEST_CASES; i++) {
                final int index = i;
                executor.submit(() -> {
                    String randomInput = "complexInput" + index + Math.random();
                    byte[] hashBytes = DavoHash512.hash(randomInput);
                    String hashHex = DavoHash512.bytesToHex(hashBytes);
                    synchronized (hashSet) {
                        assertTrue(hashSet.add(hashHex), "Kollision erkannt: Zwei verschiedene Eingaben führten zum gleichen Hash.");
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
        final int maxInputSize = 1_000_000_000; // 1 GB Eingabegröße
        String largeInput = "x".repeat(maxInputSize);

        long startTime = System.nanoTime();
        byte[] hashBytes = DavoHash512.hash(largeInput);
        long duration = System.nanoTime() - startTime;

        assertNotNull(hashBytes, "Hash sollte für extrem große Eingaben nicht null sein.");
        assertTrue(duration < 300_000_000_000L, "Hashing extrem großer Eingaben sollte innerhalb von 300 Sekunden abgeschlossen sein.");
        System.out.println("Hashing-Zeit für 1 GB große Eingabe: " + duration / 1_000_000_000.0 + " Sekunden");
    }

    @Test
    public void testExtremeAvalancheEffect() throws InterruptedException {
        String input = "ExtremeAvalancheInput";
        byte[] hash1 = DavoHash512.hash(input);
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
                    byte[] hash2 = DavoHash512.hash(modifiedInput);
                    int difference = calculateBitDifference(hash1, hash2);
                    assertTrue(difference > (hash1.length * 8 * 0.90), "Extremer Avalanche-Effekt-Test fehlgeschlagen.");
                });
            }
        } finally {
            executor.shutdown();
            boolean terminated = executor.awaitTermination(120, TimeUnit.SECONDS);
            if (!terminated) {
                System.err.println("Der ExecutorService wurde nicht rechtzeitig beendet.");
            }
        }
    }

    @Test
    public void testExtremeBirthdayAttackResistance() {
        Set<String> hashSet = new HashSet<>();

        for (int i = 0; i < BIRTHDAY_ATTACK_SAMPLE_SIZE * 2; i++) {
            String randomInput = "birthdayAttack" + i + Math.random();
            byte[] hashBytes = DavoHash512.hash(randomInput);
            String hashHex = DavoHash512.bytesToHex(hashBytes);
            assertTrue(hashSet.add(hashHex), "Kollision erkannt: Zwei zufällige Eingaben erzeugten denselben Hash.");
        }
    }

    @Test
    public void testExtremeSpecialCharactersHandling() {
        String[] specialInputs = {
                "😊", "你好", "🌍🚀", "🎉🎈🥳", "𐍈𐌰𐌹", "🔥".repeat(500_000),
                "\uDBFF\uDFFF", "\uD83D\uDE00\uD83D\uDE02\uD83E\uDD23", "ℜ𝔬𝔟𝔲𝔰𝔱", "❄️☃️🔥🌞🌬️"
        };

        for (String input : specialInputs) {
            byte[] hashBytes = DavoHash512.hash(input);
            assertNotNull(hashBytes, "Hash sollte für Multibyte-Eingaben nicht null sein.");
            assertTrue(hashBytes.length > 0, "Hash sollte nicht leer sein.");
        }
    }

    @Test
    public void testRapidSequentialHashing() {
        for (int i = 0; i < 100_000; i++) {
            String input = "rapidTestInput" + i;
            byte[] hashBytes = DavoHash512.hash(input);

            assertNotNull(hashBytes, "Hash sollte nicht null sein bei sequentieller Eingabe.");
            assertTrue(hashBytes.length > 0, "Hash sollte nicht leer sein.");
        }
    }

    @Test
    public void testExtremeSlidingWindowsEffect() {
        String baseInput = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        Set<String> hashes = new HashSet<>();

        for (int i = 0; i < baseInput.length() - 5; i++) {
            String window = baseInput.substring(i, i + 6);
            byte[] hashBytes = DavoHash512.hash(window);
            String hashHex = DavoHash512.bytesToHex(hashBytes);
            assertTrue(hashes.add(hashHex), "Sliding Windows Test fehlgeschlagen: Zwei unterschiedliche Fenster erzeugten denselben Hash.");
        }
    }

    @Test
    public void testStackedLengthExtensionResistance() {
        String input = "secureBaseData";
        byte[] hash1 = DavoHash512.hash(input);

        String extendedInput = input;
        for (int i = 0; i < 10; i++) {
            extendedInput += "extension" + i;
            byte[] hash2 = DavoHash512.hash(extendedInput);

            assertNotEquals(DavoHash512.bytesToHex(hash1), DavoHash512.bytesToHex(hash2), "Längenverlängerungsangriff erfolgreich.");
        }
    }

    @Test
    public void testRandomRepeatedPatterns() {
        Random random = new Random();
        Set<String> hashSet = new HashSet<>();

        for (int i = 0; i < NUM_MAX_TEST_CASES / 100; i++) {
            StringBuilder inputBuilder = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                inputBuilder.append((char) (random.nextInt(26) + 'a'));
            }
            String repeatedPattern = inputBuilder.toString().repeat(random.nextInt(5) + 1);
            String mirroredPattern = new StringBuilder(repeatedPattern).reverse().toString();
            String randomInput = repeatedPattern + mirroredPattern;

            byte[] hashBytes = DavoHash512.hash(randomInput);
            String hashHex = DavoHash512.bytesToHex(hashBytes);
            assertTrue(hashSet.add(hashHex), "Kollision erkannt: Zwei zufällige Muster führten zum gleichen Hash.");
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
