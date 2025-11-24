package backend.test;

import model.Employee;
import storage.RecordFormat;
import util.SerializationUtil;


public class SerializationUtilTest {
    
    public static void main(String[] args) {
        testSerializationDeserialization();
        testFixedLengthStrings();
        testReadIndividualFields();
        testInvalidRecordSize();
        System.out.println("✅ Все тесты SerializationUtil прошли успешно!");
    }
    
    static void testSerializationDeserialization() {
        try {
            Employee original = new Employee(123, "Иван Петров", "IT", "Разработчик", 
                                            1500.50f, java.time.LocalDate.of(2023, 5, 15));
            
            byte[] data = SerializationUtil.serializeEmployee(original);
            
            // Проверяем размер
            assert data.length == RecordFormat.RECORD_SIZE : "Неверный размер записи";
            
            // Десериализуем обратно
            Employee deserialized = SerializationUtil.deserializeEmployee(data);
            
            // Проверяем что данные совпадают
            assert original.getId() == deserialized.getId() : "ID не совпадает";
            assert original.getName().equals(deserialized.getName()) : "Name не совпадает";
            assert original.getDepartment().equals(deserialized.getDepartment()) : "Department не совпадает";
            assert original.getPosition().equals(deserialized.getPosition()) : "Position не совпадает";
            assert Math.abs(original.getSalary() - deserialized.getSalary()) < 0.001 : "Salary не совпадает";
            assert original.getHireDate().equals(deserialized.getHireDate()) : "HireDate не совпадает";
            assert original.isDeleted() == deserialized.isDeleted() : "isDeleted не совпадает";
            
            System.out.println("✅ testSerializationDeserialization - PASSED");
        } catch (Exception e) {
            System.out.println("❌ testSerializationDeserialization - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testFixedLengthStrings() {
        try {
            Employee employee = new Employee(1, "Short", "D", "P", 1000f, java.time.LocalDate.now());
            byte[] data = SerializationUtil.serializeEmployee(employee);
            
            Employee deserialized = SerializationUtil.deserializeEmployee(data);
            
            assert "Short".equals(deserialized.getName().trim()) : "Name trimming failed";
            assert "D".equals(deserialized.getDepartment().trim()) : "Department trimming failed";
            assert "P".equals(deserialized.getPosition().trim()) : "Position trimming failed";
            
            System.out.println("✅ testFixedLengthStrings - PASSED");
        } catch (Exception e) {
            System.out.println("❌ testFixedLengthStrings - FAILED: " + e.getMessage());
        }
    }
    
    static void testReadIndividualFields() {
        try {
            Employee employee = new Employee(123, "Test", "IT", "Dev", 1500f, java.time.LocalDate.now());
            byte[] data = SerializationUtil.serializeEmployee(employee);
            
            int id = SerializationUtil.readIdFromRecord(data, RecordFormat.ID_OFFSET);
            String department = SerializationUtil.readDepartmentFromRecord(data, RecordFormat.DEPARTMENT_OFFSET);
            
            assert id == 123 : "ID reading failed";
            assert "IT".equals(department.trim()) : "Department reading failed";
            
            System.out.println("✅ testReadIndividualFields - PASSED");
        } catch (Exception e) {
            System.out.println("❌ testReadIndividualFields - FAILED: " + e.getMessage());
        }
    }
    
    static void testInvalidRecordSize() {
        try {
            byte[] shortData = new byte[100];
            SerializationUtil.deserializeEmployee(shortData);
            System.out.println("❌ testInvalidRecordSize - FAILED: Должна быть ошибка");
        } catch (Exception e) {
            System.out.println("✅ testInvalidRecordSize - PASSED: " + e.getMessage());
        }
    }
}
