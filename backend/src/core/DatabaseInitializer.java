package core;

import exceptions.DatabaseException;
import storage.DataFileHandler;
import storage.IndexManager;
import storage.MetaFileHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseInitializer {
    
    public static void createDatabase(String databasePath) throws IOException {
        // Создаем директорию если нужно
        Path path = Paths.get(databasePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        // Инициализируем все файлы БД
        try (MetaFileHandler metaHandler = new MetaFileHandler(databasePath + "/meta.db");
             DataFileHandler dataHandler = new DataFileHandler(databasePath + "/data.db");
             IndexManager indexManager = new IndexManager(databasePath + "/index.db")) {
            
            metaHandler.open(true);
            dataHandler.open(true);
            indexManager.open(true);
            
            // Файлы автоматически инициализируются при открытии с create=true
            
        } catch (Exception e) {
            throw new DatabaseException("DATABASE_CREATION_FAILED", 
                "Failed to create database at: " + databasePath, e);
        }
    }
    
    public static boolean validateDatabase(String databasePath) {
        String[] requiredFiles = {
            databasePath + "/meta.db",
            databasePath + "/data.db", 
            databasePath + "/index.db"
        };
        
        // Проверяем существование всех файлов
        for (String filePath : requiredFiles) {
            if (!Files.exists(Paths.get(filePath))) {
                return false;
            }
        }
        
        // Проверяем целостность meta.db
        try (MetaFileHandler metaHandler = new MetaFileHandler(databasePath + "/meta.db")) {
            metaHandler.open(false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void deleteDatabase(String databasePath) throws IOException {
        String[] filesToDelete = {
            databasePath + "/meta.db",
            databasePath + "/data.db",
            databasePath + "/index.db"
        };
        
        for (String filePath : filesToDelete) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                throw new DatabaseException("DATABASE_DELETION_FAILED",
                    "Failed to delete database file: " + filePath, e);
            }
        }
        
        // Пытаемся удалить пустую директорию
        try {
            Files.deleteIfExists(Paths.get(databasePath));
        } catch (IOException e) {
            // Игнорируем - директория может быть не пустой
        }
    }
}
