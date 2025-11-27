package api.controllers;

import api.dto.ErrorResponse;
import api.dto.SuccessResponse;
import api.dto.UserSession;
import service.BackupManager;
import util.JsonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BackupController {
    private final DatabaseController databaseController;
    private final BackupManager backupManager;
    
    public BackupController(DatabaseController databaseController) {
        this.databaseController = databaseController;
        this.backupManager = new BackupManager();
    }
    
    public String createBackup(UserSession session) {
        try {
            if (!session.canCreateBackup()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для создания бэкапа. Требуется роль: Администратор\"}";
            }
            
            if (!databaseController.isDatabaseLoaded()) {
                return "{\"success\":false,\"error\":\"NO_DATABASE_LOADED\",\"message\":\"No database is currently loaded\"}";
            }
            
            String currentDatabasePath = getCurrentDatabasePath();
            if (currentDatabasePath == null) {
                return "{\"success\":false,\"error\":\"DATABASE_NOT_LOADED\",\"message\":\"Cannot determine current database path\"}";
            }
            
            String backupPath = backupManager.createBackup(currentDatabasePath);
            
            Map<String, Object> data = new HashMap<>();
            data.put("backupPath", backupPath);
            data.put("backupName", java.nio.file.Paths.get(backupPath).getFileName().toString());
            data.put("createdAt", java.time.LocalDateTime.now().toString());
            data.put("sourceDatabase", currentDatabasePath);
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Backup created successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e, "create backup");
        }
    }
    
    public String restoreBackup(String requestBody, UserSession session) {
        try {
            if (!session.canRestoreBackup()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для восстановления бэкапа. Требуется роль: Администратор\"}";
            }
            
            Map<String, Object> request = JsonUtil.parseJson(requestBody);
            String backupPath = (String) request.get("backupPath");
            String targetPath = (String) request.get("targetPath");
            
            if (backupPath == null || backupPath.trim().isEmpty()) {
                return "{\"success\":false,\"error\":\"INVALID_BACKUP_PATH\",\"message\":\"Backup path is required\"}";
            }
            
            if (targetPath == null || targetPath.trim().isEmpty()) {
                // Если целевой путь не указан, используем имя backup без суффикса
                String backupName = java.nio.file.Paths.get(backupPath).getFileName().toString();
                targetPath = backupName.replace("_backup_", "_restored_");
            }
            
            String restoredPath = backupManager.restoreBackup(backupPath, targetPath);
            
            Map<String, Object> data = new HashMap<>();
            data.put("restoredPath", restoredPath);
            data.put("backupPath", backupPath);
            data.put("restoredAt", java.time.LocalDateTime.now().toString());
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Backup restored successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e, "restore backup");
        }
    }
    
    public String listBackups(UserSession session) {
        try {
            if (!session.canCreateBackup()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для просмотра бэкапов. Требуется роль: Администратор\"}";
            }
            
            List<BackupManager.BackupInfo> backups = backupManager.listBackups();
            
            List<Map<String, Object>> backupList = backups.stream()
                .map(backup -> {
                    Map<String, Object> backupInfo = new HashMap<>();
                    backupInfo.put("path", backup.getPath());
                    backupInfo.put("name", backup.getName());
                    backupInfo.put("createdAt", backup.getFormattedDate());
                    backupInfo.put("size", backup.getSize());
                    backupInfo.put("formattedSize", backup.getFormattedSize());
                    backupInfo.put("fileCount", backup.getFileCount());
                    return backupInfo;
                })
                .collect(Collectors.toList());
            
            SuccessResponse<List<Map<String, Object>>> response = new SuccessResponse<>(
                "Backups retrieved successfully",
                backupList,
                backupList.size()
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e, "list backups");
        }
    }
    
    public String deleteBackup(String requestBody, UserSession session) {
        try {
            if (!session.canCreateBackup()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для удаления бэкапов. Требуется роль: Администратор\"}";
            }
            
            Map<String, Object> request = JsonUtil.parseJson(requestBody);
            String backupPath = (String) request.get("backupPath");
            
            if (backupPath == null || backupPath.trim().isEmpty()) {
                return "{\"success\":false,\"error\":\"INVALID_BACKUP_PATH\",\"message\":\"Backup path is required\"}";
            }
            
            boolean deleted = backupManager.deleteBackup(backupPath);
            
            Map<String, Object> data = new HashMap<>();
            data.put("deleted", deleted);
            data.put("backupPath", backupPath);
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Backup deleted successfully",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e, "delete backup");
        }
    }
    
    private String getCurrentDatabasePath() {
        try {
            // Получаем путь текущей БД из DatabaseController
            // Это упрощенная реализация - в реальности нужно получить из serverState
            java.lang.reflect.Field serverStateField = databaseController.getClass().getDeclaredField("serverState");
            serverStateField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> serverState = (Map<String, Object>) serverStateField.get(databaseController);
            
            return (String) serverState.get("loadedDatabasePath");
        } catch (Exception e) {
            return "current_database";
        }
    }
    
    private String handleException(Exception e, String operation) {
        try {
            ErrorResponse error = new ErrorResponse("BACKUP_ERROR", 
                "Error during " + operation + ": " + e.getMessage());
            return JsonUtil.toJson(error);
        } catch (Exception jsonError) {
            return "{\"success\":false,\"error\":\"BACKUP_ERROR\",\"message\":\"Failed to " + operation + "\"}";
        }
    }
}
