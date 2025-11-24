package model;

import exceptions.ValidationException;
import java.time.LocalDate;
import java.util.Objects;

public class Employee {
    private int id;
    private String name;
    private String department;
    private String position;
    private float salary;
    private LocalDate hireDate;
    private boolean isDeleted;
    
    // Конструкторы
    public Employee() {}
    
    public Employee(int id, String name, String department, String position, 
                   float salary, LocalDate hireDate) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.position = position;
        this.salary = salary;
        this.hireDate = hireDate;
        this.isDeleted = false;
        validate();
    }
    
    // Валидация
    public void validate() {
        // ID validation
        if (id <= 0) {
            throw new ValidationException("id", "positive", "ID must be positive integer");
        }
        if (id > 999999) {
            throw new ValidationException("id", "max_value", "ID cannot exceed 999999");
        }
        
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "required", "Name is required");
        }
        if (name.length() > 100) {
            throw new ValidationException("name", "max_length", "Name cannot exceed 100 characters");
        }
        if (!name.matches("^[A-Za-zА-Яа-яёЁ\\s\\-']+$")) {
            throw new ValidationException("name", "pattern", "Name can only contain letters, spaces, hyphens and apostrophes");
        }
        
        // Department validation
        if (department == null || department.trim().isEmpty()) {
            throw new ValidationException("department", "required", "Department is required");
        }
        if (department.length() > 50) {
            throw new ValidationException("department", "max_length", "Department cannot exceed 50 characters");
        }
        
        // Position validation
        if (position == null || position.trim().isEmpty()) {
            throw new ValidationException("position", "required", "Position is required");
        }
        if (position.length() > 50) {
            throw new ValidationException("position", "max_length", "Position cannot exceed 50 characters");
        }
        
        // Salary validation
        if (salary < 0) {
            throw new ValidationException("salary", "positive", "Salary cannot be negative");
        }
        if (salary > 999999.99f) {
            throw new ValidationException("salary", "max_value", "Salary cannot exceed 999999.99");
        }
        if (Math.round(salary * 100) != salary * 100) {
            throw new ValidationException("salary", "decimal_places", "Salary can have maximum 2 decimal places");
        }
        
        // Hire date validation
        if (hireDate == null) {
            throw new ValidationException("hireDate", "required", "Hire date is required");
        }
        if (hireDate.isAfter(LocalDate.now())) {
            throw new ValidationException("hireDate", "past_date", "Hire date cannot be in the future");
        }
    }
    
    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public float getSalary() { return salary; }
    public void setSalary(float salary) { this.salary = salary; }
    
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return id == employee.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', department='%s', position='%s', salary=%.2f, hireDate=%s}",
                id, name, department, position, salary, hireDate);
    }
}
