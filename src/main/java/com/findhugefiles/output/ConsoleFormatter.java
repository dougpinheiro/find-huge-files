package com.findhugefiles.output;

import com.findhugefiles.model.FileInfo;
import com.findhugefiles.util.SizeFormatter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Formats file information as a console table.
 */
public class ConsoleFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PATH_WIDTH = 80;
    private static final int MIN_PATH_WIDTH = 40;

    /**
     * Prints a formatted table of files to stdout.
     *
     * @param files list of FileInfo to display
     */
    public void printTable(List<FileInfo> files) {
        if (files.isEmpty()) {
            System.out.println("No files found.");
            return;
        }

        // Calculate column widths
        int rankWidth = String.valueOf(files.size()).length();
        rankWidth = Math.max(rankWidth, 4); // "Rank" header

        int sizeWidth = 12; // Fixed for formatted size
        int dateWidth = 19; // "yyyy-MM-dd HH:mm:ss"
        int ownerWidth = calculateOwnerWidth(files);
        ownerWidth = Math.max(ownerWidth, 6); // "Owner" header

        // Path gets remaining width
        int terminalWidth = getTerminalWidth();
        int usedWidth = rankWidth + 1 + sizeWidth + 1 + dateWidth + 1 + ownerWidth + 1 + 2; // separators + padding
        int pathWidth = Math.min(Math.max(terminalWidth - usedWidth, MIN_PATH_WIDTH), MAX_PATH_WIDTH);

        // Print header
        String headerFormat = "%-" + rankWidth + "s  %-" + sizeWidth + "s  %-" + dateWidth + "s  %-" + ownerWidth + "s  %s%n";
        System.out.printf(headerFormat, "Rank", "Size", "Modified", "Owner", "Path");
        System.out.println("─".repeat(Math.min(terminalWidth, rankWidth + sizeWidth + dateWidth + ownerWidth + pathWidth + 10)));

        // Print rows
        String rowFormat = "%-" + rankWidth + "d  %-" + sizeWidth + "s  %-" + dateWidth + "s  %-" + ownerWidth + "s  %s%n";

        for (int i = 0; i < files.size(); i++) {
            FileInfo file = files.get(i);
            String path = truncatePath(file.absolutePath(), pathWidth);
            String size = SizeFormatter.formatFixed(file.sizeBytes());
            String date = file.lastModified().atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER);
            String owner = truncate(file.owner(), ownerWidth);

            System.out.printf(rowFormat, i + 1, size, date, owner, path);
        }

        // Print summary
        long totalSize = files.stream().mapToLong(FileInfo::sizeBytes).sum();
        System.out.println();
        System.out.printf("Total: %d files, %s%n", files.size(), SizeFormatter.format(totalSize));
    }

    /**
     * Prints a simple list format (one line per file).
     *
     * @param files list of FileInfo to display
     */
    public void printSimple(List<FileInfo> files) {
        if (files.isEmpty()) {
            System.out.println("No files found.");
            return;
        }

        for (int i = 0; i < files.size(); i++) {
            FileInfo file = files.get(i);
            System.out.printf("%d. %s  (%s)  %s%n",
                i + 1,
                file.absolutePath(),
                file.formattedSize(),
                file.lastModified().atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER)
            );
        }
    }

    private int calculateOwnerWidth(List<FileInfo> files) {
        return files.stream()
            .mapToInt(f -> f.owner().length())
            .max()
            .orElse(6);
    }

    private String truncate(String str, int maxWidth) {
        if (str.length() <= maxWidth) {
            return str;
        }
        if (maxWidth <= 3) {
            return str.substring(0, maxWidth);
        }
        return "..." + str.substring(str.length() - maxWidth + 3);
    }

    private String truncatePath(String path, int maxWidth) {
        if (path.length() <= maxWidth) {
            return path;
        }
        if (maxWidth <= 10) {
            return "..." + path.substring(path.length() - maxWidth + 3);
        }
        // Try to keep the filename intact
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            String fileName = path.substring(lastSlash + 1);
            if (fileName.length() < maxWidth - 4) {
                String prefix = "...";
                int prefixLen = maxWidth - fileName.length() - 3;
                return prefix + path.substring(lastSlash - prefixLen + 1);
            }
        }
        return "..." + path.substring(path.length() - maxWidth + 3);
    }

    private int getTerminalWidth() {
        try {
            ProcessBuilder pb = new ProcessBuilder("stty", "size");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            Scanner scanner = new Scanner(process.getInputStream());
            if (scanner.hasNext()) {
                scanner.next(); // rows
                if (scanner.hasNextInt()) {
                    return scanner.nextInt();
                }
            }
        } catch (Exception ignored) {
        }
        return 120; // Default fallback
    }
}