package edu.nie.campusfix.util;

import edu.nie.campusfix.exception.StorageException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * RFC-4180 compliant CSV export utility featuring formula injection mitigation
 * for spreadsheet security.
 */
public final class CsvExportUtil {

    private CsvExportUtil() {
        // Utility
    }

    /**
     * Sanitizes a field against formula injection and formats per RFC-4180.
     */
    public static String escapeCell(String value) {
        if (value == null) {
            return "";
        }
        String text = value;

        // Formula injection defense: neutralize spreadsheet execution prefixes
        if (!text.isEmpty()) {
            char first = text.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                text = "'" + text;
            }
        }

        boolean mustQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        if (mustQuote) {
            text = text.replace("\"", "\"\"");
            return "\"" + text + "\"";
        }
        return text;
    }

    /**
     * Writes tabular data to a CSV file safely and atomically in UTF-8.
     */
    public static void exportCsv(Path destination, List<String> headers, List<List<String>> rows) {
        if (destination == null) {
            throw new StorageException("CSV destination path cannot be null.");
        }
        Path absoluteDest = destination.toAbsolutePath().normalize();
        if (absoluteDest.getParent() != null) {
            FileStorageUtil.ensureDirectory(absoluteDest.getParent());
        }

        Path tempPath = absoluteDest.resolveSibling(absoluteDest.getFileName().toString() + ".tmp." + System.nanoTime());

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

                // Write header
                if (headers != null && !headers.isEmpty()) {
                    StringBuilder headerLine = new StringBuilder();
                    for (int i = 0; i < headers.size(); i++) {
                        if (i > 0) headerLine.append(",");
                        headerLine.append(escapeCell(headers.get(i)));
                    }
                    writer.write(headerLine.toString());
                    writer.newLine();
                }

                // Write data rows
                if (rows != null) {
                    for (List<String> row : rows) {
                        StringBuilder line = new StringBuilder();
                        for (int i = 0; i < row.size(); i++) {
                            if (i > 0) line.append(",");
                            line.append(escapeCell(row.get(i)));
                        }
                        writer.write(line.toString());
                        writer.newLine();
                    }
                }
            }

            try {
                Files.move(tempPath, absoluteDest,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempPath, absoluteDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupEx) {
                e.addSuppressed(cleanupEx);
                System.err.println("[CampusFix] Failed to remove temporary CSV file " + tempPath + ": " + cleanupEx.getMessage());
            }
            throw new StorageException("Failed to export CSV to: " + destination, e);
        }
    }
}
