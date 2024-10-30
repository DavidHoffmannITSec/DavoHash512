package org.example;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;

public class DaHoHash
{

    // Abgestimmte Anfangswerte für maximale Diffusion
    private static final long[] INITIAL_VALUES = {
            0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL, 0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
            0x510e527fade682d1L, 0x9b05688c2b3e6c1fL, 0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
    };

    // Verbesserte, schwer vorhersagbare Konstanten
    private static final long[] K_CONSTANTS = {
            0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL,
            0x3956c25bf348b538L, 0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
            0x550c7dc3d5ffb4e2L, 0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L,
            0xc19bf174cf692694L, 0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L,
            0x240ca1cc77ac9c65L, 0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L,
            0x76f988da831153b5L, 0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL,
            0xbf597fc7beef0ee4L, 0xc6e00bf33da88fc2L, 0xd5a79147930aa725L, 0x06ca6351e003826fL,
            0x142929670a0e6e70L, 0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL
    };

    public static String hash(String input) {
        String normalizedInput = Normalizer.normalize(input, Normalizer.Form.NFC);
        byte[] paddedInput = padInput(normalizedInput.getBytes(StandardCharsets.UTF_8));

        long[] stateVector = Arrays.copyOf(INITIAL_VALUES, INITIAL_VALUES.length);
        int numBlocks = paddedInput.length / 128;
        int numRounds = 120;  // Optimierte Rundenzahl für gute Diffusion und Geschwindigkeit

        for (int i = 0; i < numBlocks; i++) {
            long[] block = getBlock(paddedInput, i);
            processBlock(block, stateVector, numRounds, i);
        }

        applyEnhancedFinalization(stateVector);
        return buildHashString(stateVector);
    }

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

    private static void processBlock(long[] block, long[] stateVector, int numRounds, int blockIndex) {
        long[] w = new long[80];
        System.arraycopy(block, 0, w, 0, block.length);

        for (int i = 16; i < 80; i++) {
            long s0 = rotateRight(w[i - 15], 1) ^ rotateRight(w[i - 15], 8) ^ (w[i - 15] >>> 7);
            long s1 = rotateRight(w[i - 2], 19) ^ rotateLeft(w[i - 2], 3) ^ (w[i - 2] >>> 10);
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

        long adaptiveConstant = K_CONSTANTS[blockIndex % K_CONSTANTS.length];

        for (int r = 0; r < numRounds; r++) {
            long S1 = rotateRight(e, 14) ^ rotateLeft(e, 18) ^ rotateRight(e, 41);
            long ch = (e & f) ^ (~e & g);
            long temp1 = h + S1 + ch + adaptiveConstant + w[r % w.length];

            long S0 = rotateRight(a, 28) ^ rotateRight(a, 34) ^ rotateRight(a, 39);
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

            stateVector[r % stateVector.length] ^= temp1 ^ adaptiveConstant;
            adaptiveConstant = ~adaptiveConstant;  // Dynamische Adaption optimiert aus Code 2
        }

        for (int idx = 0; idx < stateVector.length; idx++) {
            stateVector[idx] ^= (a + b + c + d + e + f + g + h);
        }
    }

    private static void applyEnhancedFinalization(long[] stateVector) {
        for (int round = 0; round < 16; round++) {
            stateVector = permuteStateWithConstants(stateVector);
            for (int i = 0; i < stateVector.length; i++) {
                stateVector[i] ^= K_CONSTANTS[i % K_CONSTANTS.length] ^ INITIAL_VALUES[i];
                stateVector[i] ^= rotateLeft(stateVector[(i + 1) % stateVector.length], (i % 7) + 1) * 41;
                stateVector[i] ^= rotateRight(stateVector[(i + 3) % stateVector.length], i % 5 + 21);
            }
        }
    }

    private static long[] permuteStateWithConstants(long[] stateVector) {
        int length = stateVector.length;
        long[] permutedState = new long[length];
        for (int i = 0; i < length; i++) {
            int permutedIndex = (i * 5 + 11) % length;
            permutedState[permutedIndex] = stateVector[i] ^ K_CONSTANTS[i % K_CONSTANTS.length];
        }
        return permutedState;
    }

    private static long rotateRight(long value, int bits) {
        return (value >>> bits) | (value << (64 - bits));
    }

    private static long rotateLeft(long value, int bits) {
        return (value << bits) | (value >>> (64 - bits));
    }

    private static String buildHashString(long[] stateVector) {
        StringBuilder hashString = new StringBuilder();
        for (long value : stateVector) {
            hashString.append(String.format("%016x", value));
        }
        return hashString.toString();
    }
}
