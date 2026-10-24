package com.findhugefiles.model;

import com.findhugefiles.util.SizeFormatter;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Represents information about a scanned file.
 */
public record FileInfo(
    Path path,
    long sizeBytes,
    Instant lastModified,
    String owner
) {
    /**
     * Returns the absolute path as a string.
     */
    public String absolutePath() {
        return path.toAbsolutePath().toString();
    }

    /**
     * Returns the file name.
     */
    public String fileName() {
        return path.getFileName().toString();
    }

    /**
     * Returns size in human-readable format.
     */
    public String formattedSize() {
        return SizeFormatter.format(sizeBytes);
    }
}