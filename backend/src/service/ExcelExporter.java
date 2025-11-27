package service;

import model.Employee;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ExcelExporter {
    
    public String exportToExcel(List<Employee> employees, String fileName) throws Exception {
        // Создаем CSV вместо Excel (проще и не требует библиотек)
        String csvFileName = fileName + ".csv";  // Просто добавляем .csv
        String filePath = "exports/" + csvFileName;
        
        // Создаем директорию если не существует
        Files.createDirectories(Paths.get("exports"));
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Заголовки
            writer.println("ID,Name,Department,Position,Salary,HireDate");
            
            // Данные
            for (Employee emp : employees) {
                writer.printf("%d,%s,%s,%s,%.2f,%s%n",
                    emp.getId(),
                    escapeCsv(emp.getName()),
                    escapeCsv(emp.getDepartment()),
                    escapeCsv(emp.getPosition()),
                    emp.getSalary(),
                    emp.getHireDate()
                );
            }
        }
        
        return filePath;
    }
    
    public String generateFileName(String databaseName) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return databaseName + "_export_" + timestamp;
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}