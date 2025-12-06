package com.hng.docxtractor.util;
public final class FilenameSanitizer {
    public static String sanitize(String input) {
        if (input == null) return "file";
        return input.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
