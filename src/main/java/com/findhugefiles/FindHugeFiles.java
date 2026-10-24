package com.findhugefiles;

import com.findhugefiles.output.ConsoleFormatter;
import com.findhugefiles.output.FileExporter;
import com.findhugefiles.model.FileInfo;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Main entry point for the Find Huge Files application.
 */
@Command(
    name = "find-huge-files",
    version = "1.0.0",
    mixinStandardHelpOptions = true,
    description = "Scans the filesystem and lists the largest files.",
    sortOptions = false
)
public class FindHugeFiles implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Root directory to scan (default: /)",
        defaultValue = "/"
    )
    Path rootPath;

    @Option(
        names = {"-n", "--top"},
        description = "Number of largest files to display (default: 20)",
        defaultValue = "20"
    )
    int topN;

    @Option(
        names = {"-o", "--output"},
        description = "Output file path (JSON and/or CSV). If no extension, both .json and .csv are created."
    )
    Path outputPath;

    @Option(
        names = {"-f", "--format"},
        description = "Output format(s): console, json, csv, all (default: console)",
        defaultValue = "console"
    )
    String format;

    @Option(
        names = {"-e", "--exclude"},
        description = "Comma-separated list of paths to exclude from scanning"
    )
    String excludePaths;

    @Option(
        names = {"--min-size"},
        description = "Minimum file size to consider (e.g., 10MB, 1GB). Default: 0"
    )
    String minSize;

    @Option(
        names = {"-L", "--follow-links"},
        description = "Follow symbolic links",
        defaultValue = "false"
    )
    boolean followLinks;

    @Option(
        names = {"-p", "--progress"},
        description = "Progress report interval (number of files). 0 to disable (default: 10000)",
        defaultValue = "10000"
    )
    long progressInterval;

    @Option(
        names = {"-q", "--quiet"},
        description = "Suppress progress output",
        defaultValue = "false"
    )
    boolean quiet;

    @Option(
        names = {"--simple"},
        description = "Use simple output format instead of table",
        defaultValue = "false"
    )
    boolean simpleOutput;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FindHugeFiles()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Parse excluded paths
        Set<String> excluded = parseExcludedPaths();

        // Build scanner
        FileScanner.Builder scannerBuilder = new FileScanner.Builder()
            .excludePaths(excluded)
            .followSymlinks(followLinks);

        if (minSize != null && !minSize.isBlank()) {
            scannerBuilder.minSize(minSize);
        }

        FileScanner scanner = scannerBuilder.build();

        // Validate root path
        if (!rootPath.toFile().exists()) {
            System.err.println("Error: Root path does not exist: " + rootPath);
            return 1;
        }

        if (!rootPath.toFile().isDirectory()) {
            System.err.println("Error: Root path is not a directory: " + rootPath);
            return 1;
        }

        // Scan
        System.out.println("Scanning " + rootPath.toAbsolutePath() + " for largest " + topN + " files...");
        if (!quiet) {
            System.out.println("Excluded paths: " + String.join(", ", excluded));
        }

        List<FileInfo> results = scanner.scan(rootPath.toAbsolutePath(), topN, quiet ? 0 : progressInterval);

        if (results.isEmpty()) {
            System.out.println("No files found matching criteria.");
            return 0;
        }

        // Output
        ConsoleFormatter formatter = new ConsoleFormatter();
        FileExporter exporter = new FileExporter();

        Set<String> formats = Set.of(format.toLowerCase().split("\\s*,\\s*"));

        if (formats.contains("console") || formats.contains("all") || formats.contains("table")) {
            if (simpleOutput) {
                formatter.printSimple(results);
            } else {
                formatter.printTable(results);
            }
        }

        if (outputPath != null) {
            boolean wantJson = formats.contains("json") || formats.contains("all");
            boolean wantCsv = formats.contains("csv") || formats.contains("all");
            
            if (wantJson && wantCsv) {
                // Export both with proper extensions
                exporter.exportBoth(results, outputPath);
                String baseName = outputPath.toString();
                if (baseName.endsWith(".json") || baseName.endsWith(".csv")) {
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                }
                System.out.println("JSON exported to: " + baseName + ".json");
                System.out.println("CSV exported to: " + baseName + ".csv");
            } else if (wantJson) {
                exporter.exportJson(results, outputPath);
                System.out.println("JSON exported to: " + outputPath);
            } else if (wantCsv) {
                exporter.exportCsv(results, outputPath);
                System.out.println("CSV exported to: " + outputPath);
            } else {
                // Default: export both
                exporter.exportBoth(results, outputPath);
                String baseName = outputPath.toString();
                if (baseName.endsWith(".json") || baseName.endsWith(".csv")) {
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                }
                System.out.println("Exported to: " + baseName + ".json and .csv");
            }
        }

        return 0;
    }

    private Set<String> parseExcludedPaths() {
        // Default exclusions: virtual/pseudo filesystems only
        // /Volumes on macOS contains user-mounted volumes - legitimate to scan
        Set<String> excluded = new java.util.HashSet<>(Set.of(
            "/System", "/private", "/dev", "/proc", "/sys", "/run", "/tmp", "/var/tmp"
        ));

        if (excludePaths != null && !excludePaths.isBlank()) {
            String[] custom = excludePaths.split("\\s*,\\s*");
            for (String path : custom) {
                Path p = Paths.get(path).toAbsolutePath().normalize();
                excluded.add(p.toString());
            }
        }

        return excluded;
    }
}