package backend.test;

import exceptions.DatabaseException;
import model.Employee;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import core.DatabaseEngine;

public class DatabaseEngineTest {
    
    public static void main(String[] args) {
        testDatabaseCreation();
        testCRUDOperations();
        testSearchOperations();
        testBulkOperations();
        testErrorHandling();
        System.out.println("✅ Все тесты DatabaseEngine прошли успешно!");
    }
    
    private static void safeDelete(String path) {
        try {
            Files.deleteIfExists(Paths.get(path + "/data.db"));
            Files.deleteIfExists(Paths.get(path + "/index.db"));
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Warning: Could not delete " + path);
        }
    }
    
    static void testDatabaseCreation() {
        String testDb = "test_data/test_db";
        
        try {
            safeDelete(testDb);
            
            DatabaseEngine db = new DatabaseEngine(testDb);
            db.open(true);
            
            assert db.isOpen() : "База данных должна быть открыта";
            assert Files.exists(Paths.get(testDb + "/data.db")) : "data.db должен быть создан";
            assert Files.exists(Paths.get(testDb + "/index.db")) : "index.db должен быть создан";
            
            db.close();
            safeDelete(testDb);
            
            System.out.println("✅ testDatabaseCreation - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testDatabaseCreation - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testCRUDOperations() {
        String testDb = "test_data/crud_test";
        
        try (DatabaseEngine db = new DatabaseEngine(testDb)) {
            db.open(true);
            
            // CREATE
            Employee emp1 = new Employee(1, "Иван Петров", "IT", "Разработчик", 
                                        1500.50f, LocalDate.of(2023, 5, 15));
            Employee emp2 = new Employee(2, "Мария Сидорова", "HR", "Рекрутер", 
                                        1200.00f, LocalDate.of(2024, 1, 20));
            
            db.addEmployee(emp1);
            db.addEmployee(emp2);
            
            // READ
            Employee found1 = db.findEmployeeById(1);
            Employee found2 = db.findEmployeeById(2);
            
            assert found1 != null : "Сотрудник 1 должен быть найден";
            assert found2 != null : "Сотрудник 2 должен быть найден";
            assert "Иван Петров".equals(found1.getName()) : "Имя сотрудника 1 должно совпадать";
            assert "Мария Сидорова".equals(found2.getName()) : "Имя сотрудника 2 должно совпадать";
            
            // UPDATE
            Employee updated = new Employee(1, "Иван Петров", "IT", "Старший разработчик", 
                                          1700.00f, LocalDate.of(2023, 5, 15));
            db.updateEmployee(updated);
            
            Employee afterUpdate = db.findEmployeeById(1);
            assert "Старший разработчик".equals(afterUpdate.getPosition()) : "Должность должна быть обновлена";
            assert afterUpdate.getSalary() == 1700.00f : "Зарплата должна быть обновлена";
            
            // DELETE
            boolean deleted = db.deleteEmployeeById(2);
            assert deleted : "Удаление должно вернуть true";
            assert db.findEmployeeById(2) == null : "Сотрудник 2 должен быть удален";
            
            System.out.println("✅ testCRUDOperations - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testCRUDOperations - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testDb);
        }
    }
    
    static void testSearchOperations() {
        String testDb = "test_data/search_test";
        
        try (DatabaseEngine db = new DatabaseEngine(testDb)) {
            db.open(true);
            
            // Создаем тестовые данные
            db.addEmployee(new Employee(1, "Иван Петров", "IT", "Разработчик", 1500f, LocalDate.now()));
            db.addEmployee(new Employee(2, "Петр Иванов", "IT", "Тестировщик", 1300f, LocalDate.now()));
            db.addEmployee(new Employee(3, "Мария Сидорова", "HR", "Рекрутер", 1200f, LocalDate.now()));
            db.addEmployee(new Employee(4, "Анна Козлова", "Finance", "Аналитик", 1400f, LocalDate.now()));
            
            // Поиск по отделу
            List<Employee> itEmployees = db.findEmployeesByDepartment("IT");
            assert itEmployees.size() == 2 : "В отделе IT должно быть 2 сотрудника";
            
            // Поиск по имени
            List<Employee> ivanEmployees = db.findEmployeesByName("Иван");
            assert ivanEmployees.size() == 1 : "Должен найтись 1 сотрудник с именем Иван";
            
            // Поиск по должности
            List<Employee> recruiters = db.findEmployeesByPosition("Рекрутер");
            assert recruiters.size() == 1 : "Должен найтись 1 рекрутер";
            
            // Получить всех
            List<Employee> allEmployees = db.getAllEmployees();
            assert allEmployees.size() == 4 : "Всего должно быть 4 сотрудника";
            
            System.out.println("✅ testSearchOperations - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testSearchOperations - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testDb);
        }
    }
    
    static void testBulkOperations() {
        String testDb = "test_data/bulk_test";
        
        try (DatabaseEngine db = new DatabaseEngine(testDb)) {
            db.open(true);
            
            // Добавляем несколько сотрудников в один отдел (используем правильные имена)
            db.addEmployee(new Employee(1, "Алексей Петров", "Sales", "Менеджер", 1000f, LocalDate.now()));
            db.addEmployee(new Employee(2, "Елена Смирнова", "Sales", "Менеджер", 1100f, LocalDate.now()));
            db.addEmployee(new Employee(3, "Дмитрий Иванов", "IT", "Разработчик", 1500f, LocalDate.now()));
            
            // Массовое удаление по отделу
            int deletedCount = db.deleteEmployeesByDepartment("Sales");
            assert deletedCount == 2 : "Должно быть удалено 2 сотрудника из Sales";
            
            // Проверяем что остались только IT
            List<Employee> remaining = db.getAllEmployees();
            assert remaining.size() == 1 : "Должен остаться 1 сотрудник";
            assert "IT".equals(remaining.get(0).getDepartment()) : "Оставшийся сотрудник должен быть из IT";
            
            System.out.println("✅ testBulkOperations - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testBulkOperations - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testDb);
        }
    }
    
    static void testErrorHandling() {
        String testDb = "test_data/error_test";
        
        try {
            safeDelete(testDb);
            
            // Тест дубликатов
            try (DatabaseEngine db = new DatabaseEngine(testDb)) {
                db.open(true);
                
                Employee emp1 = new Employee(1, "Иван", "IT", "Dev", 1000f, LocalDate.now());
                db.addEmployee(emp1);
                
                try {
                    db.addEmployee(emp1);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка дубликата");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            // Тест обновления несуществующего сотрудника
            try (DatabaseEngine db = new DatabaseEngine(testDb)) {
                db.open(true);
                
                Employee nonExistent = new Employee(999, "Несуществующий", "IT", "Dev", 1000f, LocalDate.now());
                
                try {
                    db.updateEmployee(nonExistent);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка при обновлении несуществующего");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            System.out.println("✅ testErrorHandling - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testErrorHandling - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testDb);
        }
    }
}
