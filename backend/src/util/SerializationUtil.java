package util;

import model.Employee;
import storage.RecordFormat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class SerializationUtil {
    
    public static byte[] serializeEmployee(Employee employee) {
        ByteBuffer buffer = ByteBuffer.allocate(RecordFormat.RECORD_SIZE);
        
        // ID (4 bytes)
        buffer.putInt(employee.getId());
        
        // Name (100 bytes) - fixed length, padded with spaces
        putFixedLengthString(buffer, employee.getName(), RecordFormat.MAX_NAME_LENGTH);
        
        // Department (50 bytes)
        putFixedLengthString(buffer, employee.getDepartment(), RecordFormat.MAX_DEPARTMENT_LENGTH);
        
        // Position (50 bytes)  
        putFixedLengthString(buffer, employee.getPosition(), RecordFormat.MAX_POSITION_LENGTH);
        
        // Salary (4 bytes)
        buffer.putFloat(employee.getSalary());
        
        // Hire date as timestamp (8 bytes)
        long hireTimestamp = employee.getHireDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        buffer.putLong(hireTimestamp);
        
        // Deleted flag (1 byte)
        buffer.put((byte) (employee.isDeleted() ? 1 : 0));
        
        // Padding (оставшиеся 39 байт заполняем нулями)
        while (buffer.position() < RecordFormat.RECORD_SIZE) {
            buffer.put((byte) 0);
        }
        
        return buffer.array();
    }
    
    public static Employee deserializeEmployee(byte[] data) {
        if (data.length != RecordFormat.RECORD_SIZE) {
            throw new IllegalArgumentException("Invalid record size: " + data.length);
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        // ID
        int id = buffer.getInt();
        
        // Name
        String name = getFixedLengthString(buffer, RecordFormat.MAX_NAME_LENGTH);
        
        // Department
        String department = getFixedLengthString(buffer, RecordFormat.MAX_DEPARTMENT_LENGTH);
        
        // Position
        String position = getFixedLengthString(buffer, RecordFormat.MAX_POSITION_LENGTH);
        
        // Salary
        float salary = buffer.getFloat();
        
        // Hire date
        long hireTimestamp = buffer.getLong();
        LocalDate hireDate = LocalDate.ofEpochDay(hireTimestamp / (24 * 60 * 60 * 1000));
        
        // Deleted flag
        boolean isDeleted = buffer.get() != 0;
        
        Employee employee = new Employee(id, name.trim(), department.trim(), position.trim(), salary, hireDate);
        employee.setDeleted(isDeleted);
        
        return employee;
    }
    
    private static void putFixedLengthString(ByteBuffer buffer, String value, int length) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int bytesToWrite = Math.min(bytes.length, length);
        buffer.put(bytes, 0, bytesToWrite);
        
        // Padding with spaces
        for (int i = bytesToWrite; i < length; i++) {
            buffer.put((byte) ' ');
        }
    }
    
    private static String getFixedLengthString(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    // Вспомогательные методы для работы с отдельными полями
    public static int readIdFromRecord(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4);
        return buffer.getInt();
    }
    
    public static String readDepartmentFromRecord(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, RecordFormat.MAX_DEPARTMENT_LENGTH);
        byte[] bytes = new byte[RecordFormat.MAX_DEPARTMENT_LENGTH];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }
}
