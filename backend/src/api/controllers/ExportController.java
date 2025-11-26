package api.controllers;

import api.dto.ErrorResponse;
import api.dto.SuccessResponse;
import api.dto.UserSession;
import core.DatabaseEngine;
import model.Employee;
import service.ExcelExporter;
import util.JsonUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportController {
    private final DatabaseController databaseController;
    private final ExcelExporter excelExporter;
    
    public ExportController(DatabaseController databaseController) {
        this.databaseController = databaseController;
        this.excelExporter = new ExcelExporter();
    }
    
    public String exportToExcel(UserSession session) {
        try {
            if (!session.canExportToExcel()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для экспорта в Excel. Требуется роль: Администратор или Оператор\"}";
            }
            
            // Проверяем что БД загружена
            if (!databaseController.isDatabaseLoaded()) {
                return "{\"success\":false,\"error\":\"NO_DATABASE_LOADED\",\"message\":\"No database is currently loaded\"}";
            }
            
            DatabaseEngine db = databaseController.getCurrentDatabase();
            List<Employee> employees = db.getAllEmployees();
            
            if (employees.isEmpty()) {
                return "{\"success\":false,\"error\":\"NO_DATA\",\"message\":\"Нет данных для экспорта\"}";
            }
            
            // Генерируем имя файла
            String databaseName = getCurrentDatabaseName();
            String fileName = excelExporter.generateFileName(databaseName);
            
            // Экспортируем в Excel
            String filePath = excelExporter.exportToExcel(employees, fileName);
            
            // Проверяем что файл создан
            File exportedFile = new File(filePath);
            if (!exportedFile.exists()) {
                return "{\"success\":false,\"error\":\"EXPORT_FAILED\",\"message\":\"Не удалось создать файл Excel\"}";
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("filePath", filePath);
            data.put("fileName", fileName + ".xlsx");
            data.put("recordCount", employees.size());
            data.put("fileSize", exportedFile.length());
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Данные успешно экспортированы в Excel",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String downloadExcel(String filePath, UserSession session) {
        try {
            if (!session.canExportToExcel()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для скачивания файлов\"}";
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                return "{\"success\":false,\"error\":\"FILE_NOT_FOUND\",\"message\":\"Файл не найден: " + filePath + "\"}";
            }
            
            // Здесь будет логика для отдачи файла
            // В реальной реализации этот метод будет отдавать файл как бинарные данные
            
            Map<String, Object> data = new HashMap<>();
            data.put("filePath", filePath);
            data.put("fileName", file.getName());
            data.put("fileSize", file.length());
            data.put("downloadUrl", "/download/" + file.getName());
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Файл готов к скачиванию",
                data
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String listExportedFiles(UserSession session) {
        try {
            if (!session.canExportToExcel()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для просмотра экспортированных файлов\"}";
            }
            
            File exportsDir = new File("exports");
            if (!exportsDir.exists() || !exportsDir.isDirectory()) {
                return "{\"success\":true,\"data\":[],\"message\":\"Нет экспортированных файлов\"}";
            }
            
            File[] files = exportsDir.listFiles((dir, name) -> name.endsWith(".xlsx"));
            
            if (files == null || files.length == 0) {
                return "{\"success\":true,\"data\":[],\"message\":\"Нет экспортированных файлов\"}";
            }
            
            java.util.List<Map<String, Object>> fileList = new java.util.ArrayList<>();
            for (File file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getName());
                fileInfo.put("size", file.length());
                fileInfo.put("lastModified", file.lastModified());
                fileInfo.put("path", file.getPath());
                fileList.add(fileInfo);
            }
            
            SuccessResponse<java.util.List<Map<String, Object>>> response = new SuccessResponse<>(
                "Список экспортированных файлов",
                fileList
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    private String getCurrentDatabaseName() {
        try {
            // Получаем имя текущей БД из serverState DatabaseController
            // Это упрощенная реализация - в реальности нужно получить имя БД
            return "database";
        } catch (Exception e) {
            return "employees";
        }
    }
    
    private String handleException(Exception e) {
        try {
            ErrorResponse error = new ErrorResponse("EXPORT_ERROR", 
                "Ошибка при экспорте: " + e.getMessage());
            return JsonUtil.toJson(error);
        } catch (Exception jsonError) {
            return "{\"success\":false,\"error\":\"EXPORT_ERROR\",\"message\":\"Failed to export data\"}";
        }
    }
}
