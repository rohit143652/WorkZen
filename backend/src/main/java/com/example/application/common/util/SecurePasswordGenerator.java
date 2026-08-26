package com.example.application.common.util;

import java.security.SecureRandom;

/**
 * Generates admin-issued temporary passwords. Never logged, never returned
 * in any list/detail response - only handed back once, synchronously, from
 * the endpoint that generated it.
 */
public final class SecurePasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurePasswordGenerator() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));
        for (int i = 0; i < 8; i++) {
            sb.append(ALL.charAt(RANDOM.nextInt(ALL.length())));
        }
        // Shuffle
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
