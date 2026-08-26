package com.example.application.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the next sequential code for a prefix, given the last code
 * currently in the database (or null if none exist yet).
 *
 * Examples: nextCode("CLI", null, 4)      -> "CLI0001"
 *           nextCode("CLI", "CLI0007", 4) -> "CLI0008"
 *           nextCode("EMP", "EMP9999", 4) -> "EMP10000" (width grows rather than wrapping/failing)
 *
 * Callers are responsible for fetching "the last code" scoped correctly
 * (globally for company codes, per-tenant for employee/sub-client/site
 * codes) - this class only does the string arithmetic.
 */
public final class CodeGeneratorService {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("(\\d+)$");

    private CodeGeneratorService() {}

    public static String nextCode(String prefix, String lastCode, int padWidth) {
        if (lastCode == null || lastCode.isBlank()) {
            return prefix + zeroPad(1, padWidth);
        }
        Matcher matcher = TRAILING_DIGITS.matcher(lastCode);
        if (!matcher.find()) {
            // Last code doesn't end in digits (e.g. was manually entered oddly) - start fresh at 1
            // rather than throwing, so a bad historical value can never block new records.
            return prefix + zeroPad(1, padWidth);
        }
        String digits = matcher.group(1);
        long next = Long.parseLong(digits) + 1;
        int width = Math.max(padWidth, digits.length());
        return prefix + zeroPad(next, width);
    }

    private static String zeroPad(long value, int width) {
        String raw = Long.toString(value);
        if (raw.length() >= width) {
            return raw;
        }
        return "0".repeat(width - raw.length()) + raw;
    }
}
