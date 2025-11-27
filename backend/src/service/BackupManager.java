package service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BackupManager {
    
    public String createBackup(String databasePath) throws IOException {
        // Проверяем что исходная БД существует
        if (!Files.exists(Paths.get(databasePath))) {
            throw new IOException("Database path does not exist: " + databasePath);
        }
        
        // Создаем имя backup с timestamp
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = Paths.get(databasePath).getFileName() + "_backup_" + timestamp;
        Path backupPath = Paths.get("backups", backupName);
        
        // Создаем директорию backups если её нет
        Files.createDirectories(backupPath);
        
        System.out.println("Creating backup from: " + databasePath);
        System.out.println("Backup destination: " + backupPath);
        
        // Копируем все файлы БД
        String[] dbFiles = {"meta.db", "data.db", "index.db"};
        int copiedFiles = 0;
        
        for (String file : dbFiles) {
            Path sourceFile = Paths.get(databasePath, file);
            Path targetFile = backupPath.resolve(file);
            
            if (Files.exists(sourceFile)) {
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                copiedFiles++;
                System.out.println("Copied: " + sourceFile + " -> " + targetFile);
            } else {
                System.out.println("Warning: File not found: " + sourceFile);
            }
        }
        
        if (copiedFiles == 0) {
            throw new IOException("No database files found to backup");
        }
        
        System.out.println("Backup created successfully: " + backupPath);
        return backupPath.toString();
    }
    
    public String restoreBackup(String backupPath, String targetDatabasePath) throws IOException {
        // Проверяем что backup существует
        if (!Files.exists(Paths.get(backupPath))) {
            throw new IOException("Backup path does not exist: " + backupPath);
        }
        
        // Создаем целевую директорию если её нет
        Files.createDirectories(Paths.get(targetDatabasePath));
        
        System.out.println("Restoring backup from: " + backupPath);
        System.out.println("Restore destination: " + targetDatabasePath);
        
        // Копируем файлы из backup в целевую БД
        String[] dbFiles = {"meta.db", "data.db", "index.db"};
        int restoredFiles = 0;
        
        for (String file : dbFiles) {
            Path sourceFile = Paths.get(backupPath, file);
            Path targetFile = Paths.get(targetDatabasePath, file);
            
            if (Files.exists(sourceFile)) {
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                restoredFiles++;
                System.out.println("Restored: " + sourceFile + " -> " + targetFile);
            } else {
                System.out.println("Warning: Backup file not found: " + sourceFile);
            }
        }
        
        if (restoredFiles == 0) {
            throw new IOException("No valid backup files found to restore");
        }
        
        System.out.println("Backup restored successfully to: " + targetDatabasePath);
        return targetDatabasePath;
    }
    
    public List<BackupInfo> listBackups() throws IOException {
        Path backupsDir = Paths.get("backups");
        List<BackupInfo> backups = new ArrayList<>();
        
        if (!Files.exists(backupsDir) || !Files.isDirectory(backupsDir)) {
            return backups;
        }
        
        try (var paths = Files.list(backupsDir)) {
            paths.filter(Files::isDirectory)
                 .forEach(backupPath -> {
                     try {
                         BackupInfo info = getBackupInfo(backupPath);
                         if (info != null) {
                             backups.add(info);
                         }
                     } catch (IOException e) {
                         System.err.println("Error reading backup info: " + backupPath + " - " + e.getMessage());
                     }
                 });
        }
        
        // Сортируем по дате создания (новые сверху)
        backups.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));
        
        return backups;
    }
    
    public boolean deleteBackup(String backupPath) throws IOException {
        Path path = Paths.get(backupPath);
        
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IOException("Backup not found: " + backupPath);
        }
        
        // Удаляем все файлы в директории backup
        try (var paths = Files.list(path)) {
            paths.forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                }
            });
        }
        
        // Удаляем саму директорию backup
        Files.delete(path);
        
        System.out.println("Backup deleted: " + backupPath);
        return true;
    }
    
    private BackupInfo getBackupInfo(Path backupPath) throws IOException {
        String backupName = backupPath.getFileName().toString();
        
        // Парсим timestamp из имени backup
        LocalDateTime createdAt = parseTimestampFromName(backupName);
        
        // Считаем размер backup
        long size = 0;
        try (var paths = Files.list(backupPath)) {
            size = paths.mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        }
        
        // Считаем количество файлов
        int fileCount = 0;
        try (var paths = Files.list(backupPath)) {
            fileCount = (int) paths.count();
        }
        
        return new BackupInfo(
            backupPath.toString(),
            backupName,
            createdAt,
            size,
            fileCount
        );
    }
    
    private LocalDateTime parseTimestampFromName(String backupName) {
        try {
            // Ищем timestamp в формате yyyyMMdd_HHmmss
            String[] parts = backupName.split("_");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (parts[i].length() == 15 && parts[i].matches("\\d{8}_\\d{6}")) {
                    return LocalDateTime.parse(parts[i], 
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing timestamp from backup name: " + backupName);
        }
        
        // Если не удалось распарсить, используем время изменения директории
        try {
            return LocalDateTime.ofInstant(
                Files.getLastModifiedTime(Paths.get("backups", backupName)).toInstant(),
                java.time.ZoneId.systemDefault()
            );
        } catch (IOException e) {
            return LocalDateTime.now();
        }
    }
    
    // Вложенный класс для информации о backup
    public static class BackupInfo {
        private final String path;
        private final String name;
        private final LocalDateTime createdAt;
        private final long size;
        private final int fileCount;
        
        public BackupInfo(String path, String name, LocalDateTime createdAt, long size, int fileCount) {
            this.path = path;
            this.name = name;
            this.createdAt = createdAt;
            this.size = size;
            this.fileCount = fileCount;
        }
        
        // Геттеры
        public String getPath() { return path; }
        public String getName() { return name; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public long getSize() { return size; }
        public int getFileCount() { return fileCount; }
        
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            else return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
        
        public String getFormattedDate() {
            return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        }
    }
}
