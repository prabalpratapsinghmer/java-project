package edu.nie.campusfix.util;

import edu.nie.campusfix.exception.StorageException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Robust file persistence utility implementing atomic writes, UTF-8 property streams,
 * directory auto-creation, and corrupted file isolation.
 */
public final class FileStorageUtil {

    private FileStorageUtil() {
        // Utility
    }

    /**
     * Determines whether the current working directory is an active CampusFix project or
     * portable bundle directory (rather than a generic OS directory like $HOME or /opt/campusfix).
     */
    private static boolean isLocalWorkspaceWritable() {
        try {
            Path cwd = Path.of(".").toAbsolutePath().normalize();
            if (!Files.isWritable(cwd)) {
                return false;
            }
            Path dataRoot = cwd.resolve("data");
            if (Files.isDirectory(dataRoot) && Files.isWritable(dataRoot)) {
                return true;
            }
            return Files.exists(cwd.resolve("compile.sh"))
                    || Files.exists(cwd.resolve("CampusFix-1.0.0.jar"))
                    || Files.isDirectory(cwd.resolve("src"));
        } catch (SecurityException e) {
            System.err.println("[CampusFix] Local workspace check failed, using user home fallback: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resolves a writable default data directory: uses local ./data/<subDir> when running
     * inside a writable project/portable workspace, or falls back to <user.home>/.campusfix/data/<subDir>
     * when launched from a system-wide installation (such as /opt/campusfix or desktop shortcut).
     */
    public static Path resolveDefaultDataDir(String subDir) {
        if (isLocalWorkspaceWritable()) {
            return Path.of("data", subDir);
        }
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, ".campusfix", "data", subDir);
    }

    /**
     * Resolves a writable default reports directory for CSV exports and guarantees it exists.
     */
    public static Path resolveDefaultReportsDir() {
        Path dir;
        if (isLocalWorkspaceWritable()) {
            dir = Path.of("reports");
        } else {
            String userHome = System.getProperty("user.home", ".");
            dir = Path.of(userHome, ".campusfix", "reports");
        }
        return ensureDirectory(dir);
    }

    /**
     * Guarantees target directory exists.
     */
    public static Path ensureDirectory(Path dir) {
        if (dir == null) {
            return null;
        }
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return dir;
        } catch (IOException e) {
            throw new StorageException("Failed to create storage directory: " + dir, e);
        }
    }
    /**
     * Safely resolves a record file path within the specified directory, preventing path traversal attacks.
     */
    public static Path resolveSafePath(Path baseDir, String id, String extension) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\") || id.contains("..")) {
            throw new StorageException("Invalid record identifier: " + id);
        }
        Path normalizedBase = baseDir.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(id + extension).normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new StorageException("Path traversal attempt detected: " + id);
        }
        return resolved;
    }


    /**
     * Atomically writes properties to disk by staging via a temporary file in the same directory.
     */
    public static void savePropertiesAtomically(Path targetPath, Properties props, String comments) {
        ensureDirectory(targetPath.getParent());
        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp." + System.nanoTime());

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                props.store(writer, comments);
            }

            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupEx) {
                e.addSuppressed(cleanupEx);
                System.err.println("[CampusFix] Failed to remove temporary file " + tempPath + ": " + cleanupEx.getMessage());
            }
            throw new StorageException("Failed to atomically save properties to: " + targetPath, e);
        }
    }

    /**
     * Safely loads properties from a path.
     */
    public static Properties loadProperties(Path targetPath) {
        if (!Files.exists(targetPath)) {
            throw new StorageException("Properties file does not exist: " + targetPath);
        }
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(targetPath, StandardCharsets.UTF_8)) {
            props.load(reader);
            return props;
        } catch (IOException | IllegalArgumentException e) {
            quarantineCorruptFile(targetPath);
            throw new StorageException("Corrupted properties file encountered and isolated: " + targetPath, e);
        }
    }

    /**
     * Quarantines a malformed or unreadable file by renaming it with .corrupt suffix.
     */
    public static void quarantineCorruptFile(Path file) {
        try {
            if (Files.exists(file)) {
                Path corruptPath = file.resolveSibling(file.getFileName().toString() + ".corrupt." + System.currentTimeMillis());
                Files.move(file, corruptPath, StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[CampusFix] Isolated corrupted record to: " + corruptPath);
            }
        } catch (IOException e) {
            System.err.println("[CampusFix] Failed to isolate corrupted record: " + file + " (" + e.getMessage() + ")");
        }
    }

    /**
     * Returns all .properties files located in a directory.
     */
    public static List<Path> listPropertiesFiles(Path dir) {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().endsWith(".properties"))
                  .forEach(files::add);
            return files;
        } catch (IOException e) {
            throw new StorageException("Failed to list files in directory: " + dir, e);
        }
    }
}
