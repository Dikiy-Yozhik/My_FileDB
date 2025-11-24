package util;

import exceptions.DatabaseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IOUtil {
    
    public static void createDirectoriesIfNotExist(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    public static void safeDelete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // Логируем, но не прерываем выполнение
            System.err.println("Warning: Could not delete file " + filePath + ": " + e.getMessage());
        }
    }
    
    public static void validateFilePermissions(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            if (!Files.isReadable(path)) {
                throw new DatabaseException("FILE_READ_PERMISSION", "No read permission for file: " + filePath);
            }
            if (!Files.isWritable(path)) {
                throw new DatabaseException("FILE_WRITE_PERMISSION", "No write permission for file: " + filePath);
            }
        }
    }
    
    public static long getFileSize(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return Files.size(path);
        }
        return 0;
    }
    
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
