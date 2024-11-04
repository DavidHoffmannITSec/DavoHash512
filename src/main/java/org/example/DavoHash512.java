package org.example;

import java.io.StringReader;
import java.text.Normalizer;

public class DavoHash512 {
    private static final int STATE_SIZE = 8;
    private static final int BLOCK_SIZE = 64;
    private static final int WORD_SIZE = 64;
    private static final int BASE_ROUNDS = 80;

    // Verbesserte S-Box mit komplexen und extremen Werten
    private static final int[] S_BOX = {
            0x3A, 0x7F, 0xC6, 0x2E, 0xB5, 0xD4, 0x19, 0xA8,
            0x4F, 0x53, 0x9E, 0x1C, 0x82, 0xB0, 0x67, 0xDA,
            0x2B, 0x8D, 0xF1, 0x6A, 0x43, 0xBC, 0x75, 0x1E,
            0xD3, 0x97, 0x64, 0xEA, 0x5B, 0x29, 0x84, 0xC2,
            0xAD, 0x3C, 0xF8, 0x61, 0x96, 0x27, 0x4A, 0xBD,
            0xE5, 0x1B, 0x92, 0x78, 0xCF, 0x06, 0xA1, 0x53,
            0xD7, 0x38, 0x4E, 0xF9, 0x6C, 0xB2, 0x25, 0x8A,
            0x79, 0x03, 0xC0, 0x9D, 0x4B, 0xE6, 0x17, 0xA4,
            0x6F, 0x5A, 0x1D, 0xC7, 0xB8, 0x32, 0x98, 0xE1,
            0x23, 0xD6, 0xAF, 0x4C, 0x89, 0x50, 0x7B, 0x02,
            0x9C, 0x68, 0xF2, 0x35, 0xB1, 0xD9, 0xAE, 0x4D,
            0x1F, 0x96, 0x70, 0xC3, 0xE8, 0x5C, 0x14, 0xB7,
            0x2A, 0x8F, 0xD2, 0x65, 0x1A, 0xE4, 0x9B, 0x30,
            0x7D, 0xAC, 0x52, 0xC9, 0x46, 0xE3, 0x28, 0x9F,
            0x34, 0xB6, 0xC5, 0x07, 0xF3, 0x40, 0x89, 0xDA,
            0x61, 0x57, 0x1E, 0x9A, 0x8B, 0x24, 0xF7, 0x43,
            0xA9, 0x56, 0xB0, 0xCD, 0x78, 0x05, 0xEC, 0x29,
            0x3F, 0xC1, 0x6B, 0xD8, 0x94, 0x32, 0xEF, 0x47,
            0xA3, 0x08, 0x5D, 0xC6, 0xBF, 0x14, 0x7E, 0x92,
            0x6E, 0xF5, 0x2C, 0x87, 0xDA, 0x39, 0x4B, 0xA6,
            0x50, 0x8E, 0x31, 0xFB, 0xC9, 0x25, 0x74, 0x98,
            0x4D, 0xD3, 0xB5, 0x07, 0xE2, 0x63, 0xAC, 0x18,
            0x5F, 0xB9, 0x02, 0x74, 0xC8, 0x3A, 0x6D, 0xE1,
            0x93, 0x49, 0x1C, 0xA5, 0xBF, 0x62, 0x78, 0x0E,
            0xCB, 0x36, 0xD1, 0x94, 0xE7, 0x58, 0xAC, 0x23,
            0x40, 0x7F, 0xD5, 0xA2, 0x1B, 0x6C, 0x95, 0xF0,
            0x9E, 0x37, 0x84, 0x21, 0xB6, 0xF3, 0x5D, 0x0A,
            0xC2, 0x6A, 0xE4, 0x19, 0x7B, 0xD0, 0x53, 0xC9,
            0x2F, 0x8A, 0xB1, 0x64, 0xFE, 0x38, 0x92, 0xD7
    };

    private static final int[] P_BOX = {
            57, 19, 41, 23, 59, 17, 61, 29,   // Pi-basiert
            11, 31, 47, 53, 37, 43, 13, 3,    // e-basiert
            34, 8, 25, 10, 5, 16, 21, 0,      // Wurzel(2) und Wurzel(3)
            46, 38, 49, 27, 12, 18, 33, 7,    // Goldener Schnitt
            1, 24, 32, 22, 6, 50, 48, 55,     // Pi und e XOR-Verrechnungen
            60, 2, 28, 40, 15, 30, 14, 44,    // Logarithmen-basiert
            39, 35, 20, 4, 36, 45, 52, 26,    // eulerische Transformation
            63, 42, 56, 9, 54, 62, 58, 51     // rotationsoptimiert
    };

    private static final long[] INITIAL_VALUES = {
            0xA54FF53A5F1D36F1L, 0x510E527FADE682D1L, 0x9B05688C2B3E6C1FL, 0x1F83D9ABFB41BD6BL,
            0x5BE0CD19137E2179L, 0x983E5152EE66DFABL, 0x3C6EF372FE94F82BL, 0xA54FF53A2DE92C6FL
    };

    private static final long[] ROUND_CONSTANTS = {
            0x428A2F98D728AE22L, 0x7137449123EF65CDL, 0xB5C0FBCFEC4D3B2FL, 0xE9B5DBA58189DBBCL,
            0x3956C25BF348B538L, 0x59F111F1B605D019L, 0x923F82A4AF194F9BL, 0xAB1C5ED5DA6D8118L,
            0xD807AA9812008DEEL, 0xA30302427F537C2CL, 0xC6E00BF33DA88F83L, 0x06CA6351E003826FL,
            0x142929670A0E6E70L, 0x27B70A8546D22FFCL, 0x2E1B21385C26C926L, 0x4D2C6DFC5AC42AEDL
    };

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String hash(String input) {
        if (input == null) input = "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
        long[] state = initializeState(normalized.length());

        try (StringReader reader = new StringReader(normalized)) {
            processStream(reader, state);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        doubleFinalization(state);
        return buildHashString(state);
    }

    private static void processStream(StringReader reader, long[] state) throws Exception {
        char[] charBuffer = new char[BLOCK_SIZE];
        byte[] byteBuffer = new byte[BLOCK_SIZE];
        int charsRead;

        while ((charsRead = reader.read(charBuffer)) != -1) {
            for (int i = 0; i < BLOCK_SIZE; i++) {
                byteBuffer[i] = (i < charsRead) ? (byte) charBuffer[i] : 0;
            }
            processBlock(toLongArray(padInput(byteBuffer, charsRead)), state);
        }

        processBlock(toLongArray(padInput(new byte[0], 0)), state);
    }

    private static long[] toLongArray(byte[] bytes) {
        int length = (bytes.length + 7) / 8;
        long[] longs = new long[length];
        for (int i = 0; i < length; i++) {
            longs[i] = getLongFromBytes(bytes, i * 8);
        }
        return longs;
    }

    private static long getLongFromBytes(byte[] bytes, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if (offset + i < bytes.length) {
                value |= ((long) (bytes[offset + i] & 0xFF)) << ((7 - i) * 8);
            }
        }
        return value;
    }

    private static byte[] padInput(byte[] input, int length) {
        int paddingLength = BLOCK_SIZE - ((length + 16) % BLOCK_SIZE);
        byte[] padded = new byte[length + paddingLength + 16];

        System.arraycopy(input, 0, padded, 0, length);
        padded[length] = (byte) 0x80;

        long bitLength = length * 8L;
        for (int i = 0; i < 8; i++) {
            padded[padded.length - 16 + i] = (byte) (bitLength >>> (i * 8));
            padded[padded.length - 8 + i] = (byte) ~(bitLength >>> ((7 - i) * 8));
        }

        return padded;
    }

    private static long[] initializeState(int inputLength) {
        long[] state = new long[STATE_SIZE];
        for (int i = 0; i < STATE_SIZE; i++) {
            state[i] = INITIAL_VALUES[i] ^ ((long) inputLength * 0x1B + i * 0x7F);
        }
        return state;
    }

    private static void processBlock(long[] block, long[] state) {
        long a = state[0], b = state[1], c = state[2], d = state[3];
        long e = state[4], f = state[5], g = state[6], h = state[7];

        for (int r = 0; r < BASE_ROUNDS; r++) {
            long k = ROUND_CONSTANTS[r % ROUND_CONSTANTS.length] ^ block[r % block.length];
            long ch = (e & f) ^ (~e & g);
            long maj = (a & b) ^ (a & c) ^ (b & c);
            long sigma0 = rotateRight(a, 28) ^ rotateRight(a, 34) ^ rotateRight(a, 39);
            long sigma1 = rotateRight(e, 14) ^ rotateRight(e, 18) ^ rotateRight(e, 41);

            long t1 = h + sigma1 + ch + k + applySBoxToLong(block[r % block.length]);
            long t2 = sigma0 + maj;

            h = g;
            g = f;
            f = e;
            e = d + t1;
            d = c;
            c = b;
            b = a;
            a = t1 + t2;

            if (r % 10 == 0) applyDynamicPBox(state, r);
            // Dummy-Operationen für Timing-Konsistenz
            a ^= (a + r) & 0xFF;
        }

        for (int i = 0; i < STATE_SIZE; i++) {
            state[i] ^= avalancheMix(a + b + c + d + e + f + g + h);
        }
    }

    private static void applyDynamicPBox(long[] state, int round) {
        int[] dynamicPBox = new int[P_BOX.length];
        for (int i = 0; i < P_BOX.length; i++) {
            dynamicPBox[i] = (P_BOX[i] + round * 3) % STATE_SIZE;
        }

        long[] temp = state.clone();
        for (int i = 0; i < STATE_SIZE; i++) {
            state[i] = Long.reverse(temp[dynamicPBox[i]]);
        }
    }

    private static long applySBoxToLong(long value) {
        byte[] bytes = longToBytes(value);
        for (int i = 0; i < bytes.length; i++) {
            int index = bytes[i] & 0xFF;
            bytes[i] = (byte) S_BOX[index % S_BOX.length];
        }
        return bytesToLong(bytes);
    }

    private static byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40), (byte) (value >> 32),
                (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
        };
    }

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value |= ((long) (bytes[i] & 0xFF)) << ((7 - i) * 8);
        }
        return value;
    }

    private static void doubleFinalization(long[] state) {
        for (int round = 0; round < 24; round++) {
            for (int i = 0; i < STATE_SIZE; i++) {
                long value = state[i];
                value = avalancheMix(value);
                value ^= rotateRight(state[(i + 1) % STATE_SIZE], round + i);
                value += state[(i + 3) % STATE_SIZE] * 0x9E3779B97F4A7C15L;
                state[i] = value;
            }
        }
    }

    private static long avalancheMix(long x) {
        x ^= x >>> 29;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return x;
    }

    private static long rotateRight(long value, int bits) {
        return (value >>> bits) | (value << (WORD_SIZE - bits));
    }

    private static String buildHashString(long[] state) {
        char[] hashChars = new char[STATE_SIZE * 16];
        for (int i = 0; i < state.length; i++) {
            long value = state[i];
            for (int j = 0; j < 16; j++) {
                hashChars[i * 16 + j] = HEX_CHARS[(int) ((value >> (j * 4)) & 0xF)];
            }
        }
        return new String(hashChars);
    }
}
