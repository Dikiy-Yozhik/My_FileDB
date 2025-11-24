package util;

import exceptions.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidationUtil {
    
    public static void validateEmployeeId(int id) {
        if (id <= 0) {
            throw new ValidationException("id", "positive", "ID must be positive integer");
        }
        if (id > 999999) {
            throw new ValidationException("id", "max_value", "ID cannot exceed 999999");
        }
    }
    
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "required", "Name is required");
        }
        if (name.length() > 100) {
            throw new ValidationException("name", "max_length", "Name cannot exceed 100 characters");
        }
        if (!name.matches("^[A-Za-zА-Яа-яёЁ\\s\\-']+$")) {
            throw new ValidationException("name", "pattern", "Name can only contain letters, spaces, hyphens and apostrophes");
        }
    }
    
    public static void validateDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            throw new ValidationException("department", "required", "Department is required");
        }
        if (department.length() > 50) {
            throw new ValidationException("department", "max_length", "Department cannot exceed 50 characters");
        }
    }
    
    public static void validatePosition(String position) {
        if (position == null || position.trim().isEmpty()) {
            throw new ValidationException("position", "required", "Position is required");
        }
        if (position.length() > 50) {
            throw new ValidationException("position", "max_length", "Position cannot exceed 50 characters");
        }
    }
    
    public static void validateSalary(float salary) {
        if (salary < 0) {
            throw new ValidationException("salary", "positive", "Salary cannot be negative");
        }
        if (salary > 999999.99f) {
            throw new ValidationException("salary", "max_value", "Salary cannot exceed 999999.99");
        }
        if (Math.round(salary * 100) != salary * 100) {
            throw new ValidationException("salary", "decimal_places", "Salary can have maximum 2 decimal places");
        }
    }
    
    public static void validateHireDate(LocalDate hireDate) {
        if (hireDate == null) {
            throw new ValidationException("hireDate", "required", "Hire date is required");
        }
        if (hireDate.isAfter(LocalDate.now())) {
            throw new ValidationException("hireDate", "past_date", "Hire date cannot be in the future");
        }
    }
    
    public static LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("hireDate", "format", "Invalid date format. Use YYYY-MM-DD");
        }
    }
}
