import org.example.DavoHash;

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
    private static final int BIRTHDAY_ATTACK_SAMPLE_SIZE = 1_000_000;
    private static final int MAX_BIT_FLIP_COUNT = 50;
    private static final int PARTIAL_MATCH_LENGTH = 32;


    @Test
    public void testPartialMatchAvoidance() {
        String input = "partialMatchTest";
        String targetHash = DavoHash.hash(input);
        String partialTarget = targetHash.substring(0, PARTIAL_MATCH_LENGTH);

        boolean foundPartialMatch = false;
        for (int i = 0; i < 2_000_000; i++) {
            String randomInput = "test" + Math.random();
            String hash = DavoHash.hash(randomInput);

            if (hash.startsWith(partialTarget)) {
                foundPartialMatch = true;
                break;
            }
        }
        assertFalse(foundPartialMatch, "Eine zufällige Eingabe erzeugte eine Hash-Teilübereinstimmung.");
    }

    // Erhöhte Kollisionstests mit komplexeren zufälligen Mustern
    @Test
    public void testEnhancedCollisionAvoidance() throws InterruptedException {
        Set<String> hashSet = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(64);

        try {
            for (int i = 0; i < NUM_MAX_TEST_CASES; i++) {
                final int index = i;
                executor.submit(() -> {
                    String randomInput = "complexInput" + index + Math.random();
                    String hash = DavoHash.hash(randomInput);
                    synchronized (hashSet) {
                        assertTrue(hashSet.add(hash), "Kollision erkannt: Zwei verschiedene Eingaben führten zum gleichen Hash.");
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

    // Leistungstests mit extremen Ultra-Großeingaben bis 1 GB
    @Test
    public void testPerformanceOnUltraMaxInput() {
        final int maxInputSize = 1_000_000_000; // 1 GB Eingabegröße
        String largeInput = "x".repeat(maxInputSize);

        long startTime = System.nanoTime();
        String hash = DavoHash.hash(largeInput);
        long duration = System.nanoTime() - startTime;

        assertNotNull(hash, "Hash sollte für extrem große Eingaben nicht null sein.");
        assertTrue(duration < 300_000_000_000L, "Hashing extrem großer Eingaben sollte innerhalb von 300 Sekunden abgeschlossen sein.");
        System.out.println("Hashing-Zeit für 1 GB große Eingabe: " + duration / 1_000_000_000.0 + " Sekunden");
    }

    // Test für Avalanche-Effekt mit extremen Bit-Änderungen und zufälliger Zeichenumstellung
    @Test
    public void testExtremeAvalancheEffect() {
        String input = "ExtremeAvalancheInput";
        String hash1 = DavoHash.hash(input);
        Random random = new Random();

        for (int bitFlips = 1; bitFlips <= MAX_BIT_FLIP_COUNT * 5; bitFlips++) {
            char[] chars = input.toCharArray();

            for (int i = 0; i < bitFlips; i++) {
                int index = random.nextInt(chars.length);
                chars[index] = (char) (chars[index] ^ (1 << random.nextInt(7)));
            }

            String modifiedInput = new String(chars);
            String hash2 = DavoHash.hash(modifiedInput);

            int difference = calculateBitDifference(hash1, hash2);
            assertTrue(difference > (hash1.length() * 4 * 0.58), "Extremer Avalanche-Effekt-Test fehlgeschlagen.");
        }
    }

    // Langlebiger Birthday-Attack-Test mit doppelter Stichprobengröße
    @Test
    public void testExtremeBirthdayAttackResistance() {
        Set<String> hashSet = new HashSet<>();

        for (int i = 0; i < BIRTHDAY_ATTACK_SAMPLE_SIZE * 2; i++) {
            String randomInput = "birthdayAttack" + i + Math.random();
            String hash = DavoHash.hash(randomInput);
            assertTrue(hashSet.add(hash), "Kollision erkannt: Zwei zufällige Eingaben erzeugten denselben Hash.");
        }
    }

    // Stresstest mit einzigartigen Multibyte- und Sonderzeichen-Mustern
    @Test
    public void testExtremeSpecialCharactersHandling() {
        String[] specialInputs = {
                "😊", "你好", "🌍🚀", "🎉🎈🥳", "𐍈𐌰𐌹", "🔥".repeat(500_000),
                "\uDBFF\uDFFF", "\uD83D\uDE00\uD83D\uDE02\uD83E\uDD23", "ℜ𝔬𝔟𝔲𝔰𝔱", "❄️☃️🔥🌞🌬️"
        };

        for (String input : specialInputs) {
            String hash = DavoHash.hash(input);
            assertNotNull(hash, "Hash sollte für Multibyte-Eingaben nicht null sein.");
            assertFalse(hash.isEmpty(), "Hash sollte nicht leer sein.");
        }
    }

    // Extrem schnelle, aufeinanderfolgende Eingaben zum Prüfen der Parallelverarbeitung
    @Test
    public void testRapidSequentialHashing() {
        for (int i = 0; i < 100_000; i++) {
            String input = "rapidTestInput" + i;
            String hash = DavoHash.hash(input);

            assertNotNull(hash, "Hash sollte nicht null sein bei sequentieller Eingabe.");
            assertFalse(hash.isEmpty(), "Hash sollte nicht leer sein.");
        }
    }

    // Großer Sliding Windows Test für konsistente Veränderungen in Eingabe-Fenstern
    @Test
    public void testExtremeSlidingWindowsEffect() {
        String baseInput = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        Set<String> hashes = new HashSet<>();

        for (int i = 0; i < baseInput.length() - 5; i++) {
            String window = baseInput.substring(i, i + 6);
            String hash = DavoHash.hash(window);

            assertTrue(hashes.add(hash), "Sliding Windows Test fehlgeschlagen: Zwei unterschiedliche Fenster erzeugten denselben Hash.");
        }
    }

    // Test auf Widerstand gegen Länge-Erweiterungsangriffe mit gestapelten Anhängen
    @Test
    public void testStackedLengthExtensionResistance() {
        String input = "secureBaseData";
        String hash1 = DavoHash.hash(input);

        String extendedInput = input;
        for (int i = 0; i < 10; i++) {
            extendedInput += "extension" + i;
            String hash2 = DavoHash.hash(extendedInput);

            assertNotEquals(hash1, hash2, "Längenverlängerungsangriff erfolgreich.");
        }
    }

    // Zufällige Wiederholungs- und Spiegelmuster für Eingaben testen
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

            String hash = DavoHash.hash(randomInput);
            assertTrue(hashSet.add(hash), "Kollision erkannt: Zwei zufällige Muster führten zum gleichen Hash.");
        }
    }

    // Helper-Methode zur Berechnung der Bitunterschiede zwischen zwei Hashes
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