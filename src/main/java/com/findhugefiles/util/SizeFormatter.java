package com.findhugefiles.util;

/**
 * Utility for formatting file sizes in human-readable format.
 */
public final class SizeFormatter {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
    private static final long KILOBYTE = 1024L;

    private SizeFormatter() {
        // Utility class
    }

    /**
     * Formats bytes into human-readable string.
     *
     * @param bytes the size in bytes
     * @return formatted string (e.g., "1.5 GB")
     */
    public static String format(long bytes) {
        if (bytes < KILOBYTE) {
            return bytes + " B";
        }

        int unitIndex = (int) (Math.log(bytes) / Math.log(KILOBYTE));
        if (unitIndex >= UNITS.length) {
            unitIndex = UNITS.length - 1;
        }

        double size = bytes / Math.pow(KILOBYTE, unitIndex);
        return String.format("%.2f %s", size, UNITS[unitIndex]);
    }

    /**
     * Formats bytes with fixed precision for table alignment.
     *
     * @param bytes the size in bytes
     * @return formatted string with fixed width
     */
    public static String formatFixed(long bytes) {
        if (bytes < KILOBYTE) {
            return String.format("%7d B", bytes);
        }

        int unitIndex = (int) (Math.log(bytes) / Math.log(KILOBYTE));
        if (unitIndex >= UNITS.length) {
            unitIndex = UNITS.length - 1;
        }

        double size = bytes / Math.pow(KILOBYTE, unitIndex);
        return String.format("%6.2f %s", size, UNITS[unitIndex]);
    }
}