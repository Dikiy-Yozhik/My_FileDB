package service;

import model.Employee;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.List;

public class ExcelExporter {
    
    public String exportToExcel(List<Employee> employees, String fileName) throws IOException {
        // Создаем рабочую книгу Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Создаем лист
            Sheet sheet = workbook.createSheet("Сотрудники");
            
            // Создаем стили для ячеек
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Создаем строку заголовков
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "ФИО", "Отдел", "Должность", "Зарплата", "Дата приема"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Заполняем данными
            int rowNum = 1;
            for (Employee employee : employees) {
                Row row = sheet.createRow(rowNum++);
                
                // ID
                Cell idCell = row.createCell(0);
                idCell.setCellValue(employee.getId());
                idCell.setCellStyle(dataStyle);
                
                // ФИО
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(employee.getName());
                nameCell.setCellStyle(dataStyle);
                
                // Отдел
                Cell deptCell = row.createCell(2);
                deptCell.setCellValue(employee.getDepartment());
                deptCell.setCellStyle(dataStyle);
                
                // Должность
                Cell positionCell = row.createCell(3);
                positionCell.setCellValue(employee.getPosition());
                positionCell.setCellStyle(dataStyle);
                
                // Зарплата
                Cell salaryCell = row.createCell(4);
                salaryCell.setCellValue(employee.getSalary());
                salaryCell.setCellStyle(dataStyle);
                
                // Дата приема
                Cell dateCell = row.createCell(5);
                dateCell.setCellValue(employee.getHireDate().toString());
                dateCell.setCellStyle(dataStyle);
            }
            
            // Автоматически подгоняем ширину колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Добавляем фильтр к заголовкам
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
            
            // Создаем директорию exports если её нет
            File exportsDir = new File("exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            
            // Сохраняем файл
            String filePath = "exports/" + fileName + ".xlsx";
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
            
            return filePath;
        }
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Шрифт для заголовков
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        
        // Заливка фона
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Границы
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // Выравнивание
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Шрифт для данных
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        // Границы
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // Формат для чисел (зарплата)
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(style);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        
        return style;
    }
    
    public String generateFileName(String databaseName) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return databaseName + "_employees_" + timestamp;
    }
}
