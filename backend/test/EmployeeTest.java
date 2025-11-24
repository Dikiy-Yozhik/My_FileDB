package backend.test;

import model.Employee;

public class EmployeeTest {
    
    public static void main(String[] args) {
        testValidEmployeeCreation();
        testInvalidId();
        testInvalidName();
        testInvalidSalary();
        testFutureHireDate();
        testEqualsAndHashCode();
        System.out.println("✅ Все тесты Employee прошли успешно!");
    }
    
    static void testValidEmployeeCreation() {
        try {
            Employee employee = new Employee(1, "Иван Петров", "IT", "Разработчик", 
                                            1500.50f, java.time.LocalDate.of(2023, 5, 15));
            
            assert employee.getId() == 1 : "ID должен быть 1";
            assert "Иван Петров".equals(employee.getName()) : "Неверное имя";
            assert "IT".equals(employee.getDepartment()) : "Неверный отдел";
            assert Math.abs(employee.getSalary() - 1500.50f) < 0.001 : "Неверная зарплата";
            assert !employee.isDeleted() : "isDeleted должен быть false";
            
            System.out.println("✅ testValidEmployeeCreation - PASSED");
        } catch (Exception e) {
            System.out.println("❌ testValidEmployeeCreation - FAILED: " + e.getMessage());
        }
    }
    
    static void testInvalidId() {
        try {
            new Employee(0, "Иван Петров", "IT", "Разработчик", 1500f, java.time.LocalDate.now());
            System.out.println("❌ testInvalidId - FAILED: Должна быть ошибка валидации");
        } catch (Exception e) {
            System.out.println("✅ testInvalidId - PASSED: " + e.getMessage());
        }
    }
    
    static void testInvalidName() {
        try {
            new Employee(1, "", "IT", "Разработчик", 1500f, java.time.LocalDate.now());
            System.out.println("❌ testInvalidName - FAILED: Должна быть ошибка валидации");
        } catch (Exception e) {
            System.out.println("✅ testInvalidName - PASSED: " + e.getMessage());
        }
    }
    
    static void testInvalidSalary() {
        try {
            new Employee(1, "Иван Петров", "IT", "Разработчик", -100f, java.time.LocalDate.now());
            System.out.println("❌ testInvalidSalary - FAILED: Должна быть ошибка валидации");
        } catch (Exception e) {
            System.out.println("✅ testInvalidSalary - PASSED: " + e.getMessage());
        }
    }
    
    static void testFutureHireDate() {
        try {
            new Employee(1, "Иван Петров", "IT", "Разработчик", 1500f, java.time.LocalDate.now().plusDays(1));
            System.out.println("❌ testFutureHireDate - FAILED: Должна быть ошибка валидации");
        } catch (Exception e) {
            System.out.println("✅ testFutureHireDate - PASSED: " + e.getMessage());
        }
    }
    
    static void testEqualsAndHashCode() {
        try {
            Employee emp1 = new Employee(1, "Иван Петров", "IT", "Разработчик", 1500f, java.time.LocalDate.now());
            Employee emp2 = new Employee(1, "Мария Сидорова", "HR", "Рекрутер", 1200f, java.time.LocalDate.now());
            Employee emp3 = new Employee(2, "Петр Иванов", "Finance", "Аналитик", 1300f, java.time.LocalDate.now());
            
            assert emp1.equals(emp2) : "Employee с одинаковым ID должны быть равны";
            assert !emp1.equals(emp3) : "Employee с разным ID не должны быть равны";
            assert emp1.hashCode() == emp2.hashCode() : "HashCode должен быть одинаковым для одинакового ID";
            
            System.out.println("✅ testEqualsAndHashCode - PASSED");
        } catch (Exception e) {
            System.out.println("❌ testEqualsAndHashCode - FAILED: " + e.getMessage());
        }
    }
}
