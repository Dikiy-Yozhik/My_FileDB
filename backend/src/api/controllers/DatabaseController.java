package api.controllers;

import api.dto.ErrorResponse;
import api.dto.SuccessResponse;
import api.dto.UserSession;
import core.DatabaseEngine;
import core.DatabaseInitializer;
import exceptions.DatabaseException;
import util.JsonUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseController {
    private final Map<String, Object> serverState;
    private DatabaseEngine currentDatabase;
    
    public DatabaseController() {
        this.serverState = new ConcurrentHashMap<>();
        // Initialize with safe values
        this.serverState.put("isDatabaseLoaded", Boolean.FALSE);
        this.serverState.put("loadedDatabasePath", "");
        this.serverState.put("loadedAt", getCurrentTimestamp());
    }
    
    public String createDatabase(String requestBody, UserSession session) {
        try {
            if (!session.canCreateDatabase()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для создания БД. Требуется роль: Администратор\"}";
            }
            System.out.println("=== DATABASE CREATE REQUEST ==="); //------------
            System.out.println("Request body: " + requestBody); // --------------------

            Map<String, Object> request = JsonUtil.parseJson(requestBody);
            String databasePath = (String) request.get("databasePath");
            Boolean overwrite = (Boolean) request.getOrDefault("overwrite", false);

            System.out.println("Database path: " + databasePath); // ------------
            System.out.println("Overwrite: " + overwrite); // ----------------
            
            if (databasePath == null || databasePath.trim().isEmpty()) {
                ErrorResponse error = new ErrorResponse("INVALID_PATH", "Database path is required");
                return JsonUtil.toJson(error);
            }
            
            // Check if database exists if overwrite=false
            if (!overwrite && DatabaseInitializer.validateDatabase(databasePath)) {
                Map<String, Object> details = new HashMap<>();
                details.put("suggestion", "Use overwrite: true to replace");
                ErrorResponse error = new ErrorResponse("DATABASE_ALREADY_EXISTS", 
                    "Database already exists: " + databasePath, details);
                return JsonUtil.toJson(error);
            }
            
            // Create database
            System.out.println("Creating database at: " + databasePath); // ---------------
            DatabaseInitializer.createDatabase(databasePath);
            System.out.println("Database created successfully"); // -------------
            
            Map<String, Object> data = new HashMap<>();
            data.put("databasePath", databasePath);
            data.put("createdAt", getCurrentTimestamp());
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Database created successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String loadDatabase(String requestBody, UserSession session) {
        try {
            if (!session.canLoadDatabase()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для загрузки БД\"}";
            }
            Map<String, Object> request = JsonUtil.parseJson(requestBody);
            String databasePath = (String) request.get("databasePath");
            
            if (databasePath == null || databasePath.trim().isEmpty()) {
                ErrorResponse error = new ErrorResponse("INVALID_PATH", "Database path is required");
                return JsonUtil.toJson(error);
            }
            
            // Check if database exists
            if (!DatabaseInitializer.validateDatabase(databasePath)) {
                ErrorResponse error = new ErrorResponse("DATABASE_NOT_FOUND", 
                    "Database not found: " + databasePath);
                return JsonUtil.toJson(error);
            }
            
            // Close previous database if exists
            if (currentDatabase != null && currentDatabase.isOpen()) {
                currentDatabase.close();
            }
            
            // Load new database
            currentDatabase = new DatabaseEngine(databasePath);
            currentDatabase.open(false);
            
            // Update server state
            serverState.put("isDatabaseLoaded", true);
            serverState.put("loadedDatabasePath", databasePath);
            serverState.put("loadedAt", getCurrentTimestamp());
            
            Map<String, Object> data = new HashMap<>();
            data.put("databasePath", databasePath);
            try {
                data.put("recordCount", currentDatabase.getEmployeeCount());
            } catch (Exception e) {
                data.put("recordCount", 0);
            }
            data.put("loadedAt", serverState.get("loadedAt"));
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Database loaded successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String getDatabaseInfo(UserSession session) {
        try {
            if (!session.canViewEmployees()) { // Для информации о БД используем то же право что и для просмотра
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для просмотра информации о БД\"}";
            }
            
            checkDatabaseLoaded();
            
            Map<String, Object> info = new HashMap<>();
            info.put("databasePath", serverState.get("loadedDatabasePath"));
            try {
                info.put("recordCount", currentDatabase.getEmployeeCount());
                info.put("databaseSize", currentDatabase.getDatabaseSize());
            } catch (Exception e) {
                info.put("recordCount", 0);
                info.put("databaseSize", 0);
            }
            info.put("loadedAt", serverState.get("loadedAt"));
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Database information retrieved successfully",
                info
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String clearDatabase(UserSession session)  {
        try {
            if (!session.canClearDatabase()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для очистки БД. Требуется роль: Администратор\"}";
            }
            checkDatabaseLoaded();
            
            // Close current database
            String databasePath = (String) serverState.get("loadedDatabasePath");
            currentDatabase.close();
            
            // Delete and recreate database
            DatabaseInitializer.deleteDatabase(databasePath);
            DatabaseInitializer.createDatabase(databasePath);
            
            // Reload database
            currentDatabase = new DatabaseEngine(databasePath);
            currentDatabase.open(false);
            
            Map<String, Object> data = new HashMap<>();
            data.put("databasePath", databasePath);
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Database cleared successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String backupDatabase(UserSession session) {
        try {
            if (!session.canCreateBackup()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для создания бэкапа. Требуется роль: Администратор\"}";
            }
            checkDatabaseLoaded();
            
            String databasePath = (String) serverState.get("loadedDatabasePath");
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupPath = "backups/" + java.nio.file.Paths.get(databasePath).getFileName() + "_" + timestamp;
            
            // Create backup directory
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(backupPath));
            
            // Copy database files
            String[] dbFiles = {"meta.db", "data.db", "index.db"};
            for (String file : dbFiles) {
                java.nio.file.Path source = java.nio.file.Paths.get(databasePath, file);
                java.nio.file.Path target = java.nio.file.Paths.get(backupPath, file);
                java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("backupPath", backupPath);
            data.put("createdAt", getCurrentTimestamp());
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Backup created successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    private void checkDatabaseLoaded() {
        Boolean isLoaded = (Boolean) serverState.get("isDatabaseLoaded");
        if (isLoaded == null || !isLoaded || currentDatabase == null || !currentDatabase.isOpen()) {
            throw new DatabaseException("NO_DATABASE_LOADED", "No database is currently loaded");
        }
    }
    
    private String handleException(Exception e) {
        try {
            if (e instanceof DatabaseException) {
                DatabaseException de = (DatabaseException) e;
                ErrorResponse error = new ErrorResponse(de.getErrorCode(), de.getMessage());
                return JsonUtil.toJson(error);
            } else {
                ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", 
                    "Internal server error: " + e.getMessage());
                return JsonUtil.toJson(error);
            }
        } catch (Exception jsonError) {
            return "{\"success\":false,\"error\":\"JSON_SERIALIZATION_ERROR\",\"message\":\"Failed to serialize error response\"}";
        }
    }
    
    // Safe timestamp method
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    public DatabaseEngine getCurrentDatabase() {
        return currentDatabase;
    }
    
    public boolean isDatabaseLoaded() {
        Boolean isLoaded = (Boolean) serverState.get("isDatabaseLoaded");
        return isLoaded != null && isLoaded;
    }
}
