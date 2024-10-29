package org.example;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

public class DaHoEncryption {

    // Initialwerte für den Hash-Algorithmus, die den Startwert des Hash-Zustands bestimmen
    private static final long[] INITIAL_VALUES = {
            0xb5c0fbcfec4d3b2fL, 0x89d3d931fe4015efL, 0xa4093822299f31d0L, 0xc02c7e3555e3e001L,
            0x7046e8b61c50da8dL, 0x77dd1e5a55c135dbL, 0x7f6a8a3e56a5d78fL, 0x3c08001e0140c17bL
    };

    // Basiskonstanten, die für adaptive Konstanten und Rundentransformationen verwendet werden
    private static final long[] BASE_CONSTANTS = {
            0xfad340cbe25c8bffL, 0x1e3cfec2e8e4f3a7L, 0x6aab4fc76d1ef5ffL, 0x44db6ac4e6ffeb34L,
            0x2c8f4fc6a3e24dfeL, 0x5fb10ed9cb235cbeL, 0x3b2e0e5d6a15cd14L, 0x8f9a1d2fcde723ffL,
            0x5f34c8d7b4f7f0e6L, 0xaac3746dc839b6cfL, 0x9e3b2a64f7d3c9b1L, 0x7c5e8123a4f15dc3L,
            0x4b21d9cf0234bf7dL, 0xcfd8a1246e8b3d9eL, 0xd4c0b2f3ea47f8c6L, 0xb1e84f23cde7654aL
    };

    // Statische Permutationsmatrix zur effizienteren Verteilung
    private static final int[] PERMUTATION_MATRIX = {2, 3, 7, 0, 5, 6, 1, 4};

    // Pool zur parallelen Verarbeitung, falls mehrere Blöcke verarbeitet werden
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool();

    /**
     * Haupt-Hash-Funktion, die den Eingabe-String normalisiert, vorbereitet und den Hash-Wert berechnet.
     * Der finale Hash-Wert enthält den Hash und ein Markierungs-Suffix "[DaHo]".
     *
     * @param input Eingabe-String, der gehasht werden soll
     * @return 512-Bit-Hash als Hex-String, gefolgt von "[DaHo]"
     */
    public static String hash(String input) {
        // Normalisiere den Eingabe-String zur konsistenten Verarbeitung von Sonderzeichen
        String normalizedInput = Normalizer.normalize(input, Normalizer.Form.NFC);

        // Bereite den Input zur Blockverarbeitung vor (Padding)
        byte[] paddedInput = padInput(normalizedInput.getBytes(StandardCharsets.UTF_8));

        // Erstelle den initialen Hash-Zustandsvektor und adaptive Konstanten für die Berechnung
        long[] stateVector = Arrays.copyOf(INITIAL_VALUES, INITIAL_VALUES.length);
        long[] adaptiveConstants = generateAdaptiveConstants(input);

        // Berechne Anzahl der Blöcke und passe die Rundenzahl an die Eingabelänge an
        int numBlocks = paddedInput.length / 128;
        int numRounds = 100 + (int) Math.log(input.length() + 1) * 50; // Skalierung basierend auf Eingabelänge

        // Führe den Hash-Vorgang in parallelen Tasks aus, wenn mehrere Blöcke vorhanden sind
        forkJoinPool.invoke(new HashTask(numBlocks, paddedInput, stateVector, adaptiveConstants, numRounds));

        // Erstelle den finalen Hash-String und füge "[DaHo]" als Markierung hinzu
        return buildHashString(stateVector) + "DaHo";
    }

    // Interne Klasse zur parallelen Berechnung der Blöcke (bei großen Eingaben effizienter)
    private static class HashTask extends RecursiveTask<Void> {
        private static final int THRESHOLD = 8; // Schwellwert für parallele Verarbeitung
        private final int numBlocks;
        private final byte[] paddedInput;
        private final long[] stateVector;
        private final long[] adaptiveConstants;
        private final int numRounds;

        HashTask(int numBlocks, byte[] paddedInput, long[] stateVector, long[] adaptiveConstants, int numRounds) {
            this.numBlocks = numBlocks;
            this.paddedInput = paddedInput;
            this.stateVector = stateVector;
            this.adaptiveConstants = adaptiveConstants;
            this.numRounds = numRounds;
        }

        @Override
        protected Void compute() {
            if (numBlocks <= THRESHOLD) {
                IntStream.range(0, numBlocks).parallel().forEach(this::processBlock);
            } else {
                int mid = numBlocks / 2;
                invokeAll(new HashTask(mid, paddedInput, stateVector, adaptiveConstants, numRounds),
                        new HashTask(numBlocks - mid, Arrays.copyOfRange(paddedInput, mid * 128, numBlocks * 128), stateVector, adaptiveConstants, numRounds));
            }
            return null;
        }

        private void processBlock(int blockIndex) {
            long[] block = getBlock(paddedInput, blockIndex);
            if (blockIndex > 0) {
                applyOptimizedFeedback(block, getBlock(paddedInput, blockIndex - 1));
            }
            processRound(block, stateVector, adaptiveConstants, numRounds);
            applyEfficientPermutation(stateVector);
        }
    }

    // Erzeuge adaptive Konstanten basierend auf Eingabelänge und Hashcode, um Vorhersagbarkeit zu reduzieren
    private static long[] generateAdaptiveConstants(String input) {
        long[] adaptiveConstants = new long[BASE_CONSTANTS.length];
        long inputLength = input.length();
        for (int i = 0; i < BASE_CONSTANTS.length; i++) {
            adaptiveConstants[i] = BASE_CONSTANTS[i] ^ (inputLength * (i + 1)) ^ input.hashCode();
        }
        return adaptiveConstants;
    }

    // Padding für Eingaben, um die Größe eines Vielfachen von 128 Bytes sicherzustellen
    private static byte[] padInput(byte[] input) {
        int originalLength = input.length;
        int paddingLength = 128 - ((originalLength + 8) % 128);
        byte[] padded = new byte[originalLength + paddingLength + 8];

        System.arraycopy(input, 0, padded, 0, originalLength);
        padded[originalLength] = (byte) 0x80;

        long bitLength = originalLength * 8L;
        for (int i = 0; i < 8; i++) {
            padded[padded.length - 1 - i] = (byte) (bitLength >>> (i * 8));
        }

        return padded;
    }

    // Block extrahieren und als 64-Bit-Werte speichern
    private static long[] getBlock(byte[] input, int blockIndex) {
        long[] block = new long[16];
        for (int i = 0; i < 16; i++) {
            block[i] = 0;
            for (int j = 0; j < 8; j++) {
                block[i] = (block[i] << 8) | (input[blockIndex * 128 + i * 8 + j] & 0xFF);
            }
        }
        return block;
    }

    // Rückkopplungssystem zur Verbesserung der Diffusion
    private static void applyOptimizedFeedback(long[] currentBlock, long[] previousBlock) {
        int i = 0;
        for (long value : currentBlock) {
            currentBlock[i] ^= (rotateLeft(previousBlock[i], 5) ^ rotateRight(value, 7));
            i++;
        }
    }

    // Statische Permutationsmatrix zur Verbesserung der Bitverteilung
    private static void applyEfficientPermutation(long[] values) {
        long[] permutedValues = new long[values.length];
        int i = 0;
        for (int index : PERMUTATION_MATRIX) {
            permutedValues[i] = values[index];
            i++;
        }
        System.arraycopy(permutedValues, 0, values, 0, values.length);
    }

    // Hauptrunde zur Verarbeitung von Blöcken
    private static void processRound(long[] block, long[] stateVector, long[] adaptiveConstants, int numRounds) {
        long[] w = new long[100];
        System.arraycopy(block, 0, w, 0, block.length);

        for (int i = 16; i < 100; i++) {
            long s0 = rotateRight(w[i - 15], (i % 5 + 1)) ^
                    rotateLeft(w[i - 15], (i % 8 + 7)) ^
                    rotateRight(w[i - 15], (i % 10 + 13));
            long s1 = rotateLeft(w[i - 2], (i % 3 + 19)) ^
                    rotateRight(w[i - 2], (i % 9 + 23)) ^
                    (w[i - 2] >>> 6);
            w[i] = w[i - 16] + s0 + w[i - 7] + s1;
        }

        long a = stateVector[0];
        long b = stateVector[1];
        long c = stateVector[2];
        long d = stateVector[3];
        long e = stateVector[4];
        long f = stateVector[5];
        long g = stateVector[6];
        long h = stateVector[7];

        for (int r = 0; r < numRounds; r++) {
            long S1 = rotateRight(e, (r % 7 + 14)) ^
                    rotateLeft(e, (r % 6 + 18)) ^
                    rotateRight(e, (r % 9 + 41));
            long ch = (e & f) ^ (~e & g);
            long temp1 = h + S1 + ch + adaptiveConstants[r % adaptiveConstants.length] + w[r % w.length];

            long S0 = rotateLeft(a, (r % 8 + 28)) ^
                    rotateRight(a, (r % 5 + 34)) ^
                    rotateLeft(a, (r % 11 + 39));
            long maj = (a & b) ^ (a & c) ^ (b & c);
            long temp2 = S0 + maj;

            h = g;
            g = f;
            f = e;
            e = d + temp1;
            d = c;
            c = b;
            b = a;
            a = temp1 + temp2;
        }

        for (int idx = 0; idx < stateVector.length; idx++) {
            stateVector[idx] ^= a + b + c + d + e + f + g + h;
        }
    }

    // Rotation nach rechts
    private static long rotateRight(long value, int bits) {
        return (value >>> bits) | (value << (64 - bits));
    }

    // Rotation nach links
    private static long rotateLeft(long value, int bits) {
        return (value << bits) | (value >>> (64 - bits));
    }

    // Baut den finalen Hex-String aus dem Zustand des Hash-Vektors
    private static String buildHashString(long[] stateVector) {
        StringBuilder hashString = new StringBuilder();
        for (long value : stateVector) {
            hashString.append(String.format("%016x", value));
        }
        return hashString.toString();
    }
}
