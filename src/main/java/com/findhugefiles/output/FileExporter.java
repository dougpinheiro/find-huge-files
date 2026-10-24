package com.findhugefiles.output;

import com.findhugefiles.model.FileInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports file information to JSON or CSV files.
 */
public class FileExporter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private final ObjectMapper objectMapper;

    public FileExporter() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Exports files to JSON format.
     *
     * @param files      list of FileInfo to export
     * @param outputPath path to output file
     * @throws IOException if an I/O error occurs
     */
    public void exportJson(List<FileInfo> files, Path outputPath) throws IOException {
        List<JsonFileInfo> jsonList = files.stream()
            .map(this::toJsonInfo)
            .toList();

        JsonResult result = new JsonResult(
            files.size(),
            files.stream().mapToLong(FileInfo::sizeBytes).sum(),
            java.time.Instant.now(),
            jsonList
        );

        String json = objectMapper.writeValueAsString(result);
        Files.writeString(outputPath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Exports files to CSV format.
     *
     * @param files      list of FileInfo to export
     * @param outputPath path to output file
     * @throws IOException if an I/O error occurs
     */
    public void exportCsv(List<FileInfo> files, Path outputPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Rank,File Name,Absolute Path,Size (Bytes),Size (Human),Last Modified,Owner\n");

        for (int i = 0; i < files.size(); i++) {
            FileInfo file = files.get(i);
            csv.append(i + 1).append(',')
                .append(escapeCsv(file.fileName())).append(',')
                .append(escapeCsv(file.absolutePath())).append(',')
                .append(file.sizeBytes()).append(',')
                .append(escapeCsv(file.formattedSize())).append(',')
                .append(file.lastModified().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(',')
                .append(escapeCsv(file.owner())).append('\n');
        }

        Files.writeString(outputPath, csv.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Exports files to both JSON and CSV.
     *
     * @param files       list of FileInfo to export
     * @param basePath    base path for output files (extension will be added)
     * @throws IOException if an I/O error occurs
     */
    public void exportBoth(List<FileInfo> files, Path basePath) throws IOException {
        String baseName = basePath.toString();
        if (baseName.endsWith(".json") || baseName.endsWith(".csv")) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }
        exportJson(files, Path.of(baseName + ".json"));
        exportCsv(files, Path.of(baseName + ".csv"));
    }

    private JsonFileInfo toJsonInfo(FileInfo file) {
        return new JsonFileInfo(
            file.fileName(),
            file.absolutePath(),
            file.sizeBytes(),
            file.formattedSize(),
            file.lastModified(),
            file.owner()
        );
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // JSON DTOs
    public record JsonResult(
        int totalFiles,
        long totalSizeBytes,
        java.time.Instant scanTime,
        List<JsonFileInfo> files
    ) {}

    public record JsonFileInfo(
        String fileName,
        String absolutePath,
        long sizeBytes,
        String formattedSize,
        java.time.Instant lastModified,
        String owner
    ) {}
}