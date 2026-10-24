package com.findhugefiles;

import com.findhugefiles.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Scans the filesystem and finds the largest files using a min-heap for efficiency.
 */
public class FileScanner {
    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    private final Set<String> excludedPaths;
    private final Predicate<Path> pathFilter;
    private final long minSizeBytes;
    private final boolean followSymlinks;
    private final AtomicLong scannedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * Creates a new FileScanner with default settings.
     *
     * @param excludedPaths paths to exclude from scanning
     * @param minSizeBytes  minimum file size to consider (in bytes)
     * @param followSymlinks whether to follow symbolic links
     */
    public FileScanner(Set<String> excludedPaths, long minSizeBytes, boolean followSymlinks) {
        this.excludedPaths = excludedPaths != null ? new HashSet<>(excludedPaths) : Set.of(
            "/System", "/private", "/dev", "/proc", "/sys", "/run", "/tmp", "/var/tmp"
        );
        this.minSizeBytes = Math.max(0, minSizeBytes);
        this.followSymlinks = followSymlinks;
        this.pathFilter = this::shouldScanPath;
    }

    /**
     * Scans the filesystem starting from rootPath and returns the top N largest files.
     *
     * @param rootPath the root directory to scan
     * @param topN     number of largest files to return
     * @param progressInterval report progress every N files (0 to disable)
     * @return list of FileInfo sorted by size descending
     * @throws IOException if an I/O error occurs
     */
    public List<FileInfo> scan(Path rootPath, int topN, long progressInterval) throws IOException {
        if (topN <= 0) {
            throw new IllegalArgumentException("topN must be positive");
        }

        // Min-heap to keep track of top N largest files
        PriorityQueue<FileInfo> topFiles = new PriorityQueue<>(Comparator.comparingLong(FileInfo::sizeBytes));

        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile() || attrs.size() < minSizeBytes) {
                    return FileVisitResult.CONTINUE;
                }

                if (!pathFilter.test(file)) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    String owner = getFileOwner(file);
                    Instant lastModified = attrs.lastModifiedTime().toInstant();

                    FileInfo info = new FileInfo(file, attrs.size(), lastModified, owner);
                    addToTopFiles(topFiles, info, topN);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.debug("Error reading file {}: {}", file, e.getMessage());
                }

                long count = scannedCount.incrementAndGet();
                if (progressInterval > 0 && count % progressInterval == 0) {
                    logger.info("Scanned {} files...", count);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                errorCount.incrementAndGet();
                logger.debug("Failed to visit {}: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!pathFilter.test(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        };

        EnumSet<FileVisitOption> options = followSymlinks ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : EnumSet.noneOf(FileVisitOption.class);
        Files.walkFileTree(rootPath, options, Integer.MAX_VALUE, visitor);

        // Convert heap to sorted list (largest first)
        List<FileInfo> result = new ArrayList<>(topFiles);
        result.sort(Comparator.comparingLong(FileInfo::sizeBytes).reversed());

        logger.info("Scan complete. Scanned: {} files, Errors: {}", scannedCount.get(), errorCount.get());
        return result;
    }

    /**
     * Adds a file to the top-N heap if it qualifies.
     */
    private void addToTopFiles(PriorityQueue<FileInfo> heap, FileInfo info, int topN) {
        if (heap.size() < topN) {
            heap.offer(info);
        } else if (info.sizeBytes() > heap.peek().sizeBytes()) {
            heap.poll();
            heap.offer(info);
        }
    }

    /**
     * Determines if a path should be scanned.
     */
    private boolean shouldScanPath(Path path) {
        String pathStr = path.toAbsolutePath().toString();

        for (String excluded : excludedPaths) {
            if (pathStr.startsWith(excluded)) {
                return false;
            }
        }

        // Skip hidden files/directories (starting with .)
        Path fileName = path.getFileName();
        if (fileName != null) {
            String name = fileName.toString();
            if (name.startsWith(".") && !name.equals(".") && !name.equals("..")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the file owner name.
     */
    private String getFileOwner(Path file) {
        try {
            FileOwnerAttributeView view = Files.getFileAttributeView(file, FileOwnerAttributeView.class);
            if (view != null) {
                UserPrincipal owner = view.getOwner();
                return owner != null ? owner.getName() : "unknown";
            }
        } catch (Exception e) {
            // Ignore - return unknown
        }
        return "unknown";
    }

    /**
     * Returns the number of files scanned.
     */
    public long getScannedCount() {
        return scannedCount.get();
    }

    /**
     * Returns the number of errors encountered.
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * Builder for FileScanner.
     */
    public static class Builder {
        private Set<String> excludedPaths = new HashSet<>();
        private long minSizeBytes = 0;
        private boolean followSymlinks = false;

        public Builder excludePaths(Collection<String> paths) {
            this.excludedPaths.addAll(paths);
            return this;
        }

        public Builder minSizeBytes(long bytes) {
            this.minSizeBytes = bytes;
            return this;
        }

        public Builder minSize(String humanSize) {
            this.minSizeBytes = parseHumanSize(humanSize);
            return this;
        }

        public Builder followSymlinks(boolean follow) {
            this.followSymlinks = follow;
            return this;
        }

        public FileScanner build() {
            return new FileScanner(excludedPaths, minSizeBytes, followSymlinks);
        }

        private long parseHumanSize(String size) {
            size = size.trim().toUpperCase();
            long multiplier = 1;
            if (size.endsWith("KB")) multiplier = 1024L;
            else if (size.endsWith("MB")) multiplier = 1024L * 1024L;
            else if (size.endsWith("GB")) multiplier = 1024L * 1024L * 1024L;
            else if (size.endsWith("TB")) multiplier = 1024L * 1024L * 1024L * 1024L;

            String numStr = size.replaceAll("[KMGT]B", "").trim();
            return (long) (Double.parseDouble(numStr) * multiplier);
        }
    }
}