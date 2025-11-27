package api.controllers;

import api.dto.EmployeeRequest;
import api.dto.EmployeeResponse;
import api.dto.ErrorResponse;
import api.dto.SuccessResponse;
import api.dto.UserSession;
import core.DatabaseEngine;
import exceptions.DatabaseException;
import model.Employee;
import util.JsonUtil;
import util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmployeeController {
    private final DatabaseController databaseController;
    
    public EmployeeController(DatabaseController databaseController) {
        this.databaseController = databaseController;
    }
    
    public String getAllEmployees(UserSession session) {
        try {
            if (!session.canViewEmployees()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для просмотра сотрудников\"}";
            }
            
            checkDatabaseLoaded();
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            List<Employee> employees = db.getAllEmployees();
            List<EmployeeResponse> responseData = employees.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            SuccessResponse<List<EmployeeResponse>> response = new SuccessResponse<>(
                "Employees retrieved successfully",
                responseData,
                responseData.size()
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String getEmployeeById(String idParam, UserSession session) {
        try {
            if (!session.canViewEmployees()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для просмотра сотрудников\"}";
            }
            
            checkDatabaseLoaded();
            
            int id = parseId(idParam);
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            Employee employee = db.findEmployeeById(id);
            if (employee == null) {
                ErrorResponse error = new ErrorResponse("EMPLOYEE_NOT_FOUND",
                    "Employee with ID " + id + " not found");
                return JsonUtil.toJson(error);
            }
            
            EmployeeResponse responseData = convertToResponse(employee);
            SuccessResponse<EmployeeResponse> response = new SuccessResponse<>(
                "Employee retrieved successfully",
                responseData
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String createEmployee(String requestBody, UserSession session) {
        try {
            if (!session.canCreateEmployee()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для добавления сотрудников. Требуется роль: Администратор или Оператор\"}";
            }
            checkDatabaseLoaded();
            
            // Простой парсинг JSON вручную
            EmployeeRequest request = parseEmployeeRequest(requestBody);
            validateEmployeeRequest(request);
            
            Employee employee = convertToEntity(request);
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            db.addEmployee(employee);
            
            EmployeeResponse responseData = convertToResponse(employee);
            SuccessResponse<EmployeeResponse> response = new SuccessResponse<>(
                "Employee created successfully",
                responseData
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String updateEmployee(String idParam, String requestBody, UserSession session) {
        try {
            if (!session.canUpdateEmployee()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для добавления сотрудников. Требуется роль: Администратор или Оператор\"}";
            }
            checkDatabaseLoaded();
            
            int id = parseId(idParam);
            EmployeeRequest request = parseEmployeeRequest(requestBody);
            
            // Проверяем что ID в пути и теле запроса совпадают
            if (request.getId() != null && request.getId() != id) {
                ErrorResponse error = new ErrorResponse("ID_MISMATCH",
                    "ID in path (" + id + ") does not match ID in body (" + request.getId() + ")");
                return JsonUtil.toJson(error);
            }
            
            request.setId(id); // Устанавливаем ID из пути
            validateEmployeeRequest(request);
            
            Employee employee = convertToEntity(request);
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            db.updateEmployee(employee);
            
            EmployeeResponse responseData = convertToResponse(employee);
            SuccessResponse<EmployeeResponse> response = new SuccessResponse<>(
                "Employee updated successfully",
                responseData
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String deleteEmployee(String idParam, UserSession session) {
        try {
            if (!session.canDeleteEmployee()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для добавления сотрудников. Требуется роль: Администратор или Оператор\"}";
            }
            checkDatabaseLoaded();
            
            int id = parseId(idParam);
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            boolean deleted = db.deleteEmployeeById(id);
            
            if (deleted) {
                SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                    "Employee deleted successfully",
                    Map.of("id", id)
                );
                return JsonUtil.toJson(response);
            } else {
                ErrorResponse error = new ErrorResponse("EMPLOYEE_NOT_FOUND",
                    "Employee with ID " + id + " not found");
                return JsonUtil.toJson(error);
            }
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String searchEmployees(Map<String, String> queryParams, UserSession session) {
        try {
            if (!session.canSearchEmployees()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для добавления сотрудников. Требуется роль: Администратор или Оператор\"}";
            }
            checkDatabaseLoaded();
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            List<Employee> employees;
            
            if (queryParams.containsKey("department")) {
                employees = db.findEmployeesByDepartment(queryParams.get("department"));
            } else if (queryParams.containsKey("position")) {
                employees = db.findEmployeesByPosition(queryParams.get("position"));
            } else if (queryParams.containsKey("name")) {
                employees = db.findEmployeesByName(queryParams.get("name"));
            } else {
                // Если нет параметров, возвращаем всех
                employees = db.getAllEmployees();
            }
            
            List<EmployeeResponse> responseData = employees.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            SuccessResponse<List<EmployeeResponse>> response = new SuccessResponse<>(
                "Search completed successfully",
                responseData,
                responseData.size()
            );
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    public String deleteEmployeesByCriteria(Map<String, String> queryParams, UserSession session) {
        try {
            if (!session.canDeleteEmployee()) {
                return "{\"success\":false,\"error\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для удаления сотрудников. Требуется роль: Администратор или Оператор\"}";
            }
            
            checkDatabaseLoaded();
            DatabaseEngine db = databaseController.getCurrentDatabase();
            
            int deletedCount = 0;
            
            if (queryParams.containsKey("department")) {
                deletedCount = db.deleteEmployeesByDepartment(queryParams.get("department"));
            } else if (queryParams.containsKey("position")) {
                // Для позиции нужно сначала найти, потом удалить по одному
                List<Employee> employees = db.findEmployeesByPosition(queryParams.get("position"));
                for (Employee emp : employees) {
                    db.deleteEmployeeById(emp.getId());
                    deletedCount++;
                }
            } else {
                ErrorResponse error = new ErrorResponse("INVALID_CRITERIA",
                    "No valid criteria provided for deletion");
                return JsonUtil.toJson(error);
            }
            
            SuccessResponse<Map<String, Object>> response = new SuccessResponse<>(
                "Employees deleted successfully"
            );
            response.setDeletedCount(deletedCount);
            response.setData(Map.of("deletedCount", deletedCount));
            
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            return handleException(e);
        }
    }
    
    private EmployeeRequest parseEmployeeRequest(String json) {
        Map<String, Object> map = JsonUtil.parseJson(json);
        EmployeeRequest request = new EmployeeRequest();
        
        System.out.println("=== PARSING EMPLOYEE REQUEST ===");
        System.out.println("Raw map: " + map);
        
        // 🔥 ИСПРАВЛЕНИЕ: проверяем на null перед преобразованием
        if (map.containsKey("id") && map.get("id") != null) {
            Object idObj = map.get("id");
            System.out.println("ID object: " + idObj + " (type: " + idObj.getClass() + ")");
            if (idObj instanceof Number) {
                request.setId(((Number) idObj).intValue());
            } else if (idObj instanceof String) {
                try {
                    request.setId(Integer.parseInt((String) idObj));
                } catch (NumberFormatException e) {
                    System.out.println("Warning: Invalid ID format: " + idObj);
                }
            }
        } else {
            System.out.println("ID is null or missing - this is a new employee");
            // Для новых сотрудников ID будет null
            request.setId(null);
        }
        
        if (map.containsKey("name")) request.setName((String) map.get("name"));
        if (map.containsKey("department")) request.setDepartment((String) map.get("department"));
        if (map.containsKey("position")) request.setPosition((String) map.get("position"));
        
        // 🔥 ИСПРАВЛЕНИЕ для salary
        if (map.containsKey("salary") && map.get("salary") != null) {
            Object salaryObj = map.get("salary");
            System.out.println("Salary object: " + salaryObj + " (type: " + salaryObj.getClass() + ")");
            if (salaryObj instanceof Number) {
                request.setSalary(((Number) salaryObj).floatValue());
            } else if (salaryObj instanceof String) {
                try {
                    request.setSalary(Float.parseFloat((String) salaryObj));
                } catch (NumberFormatException e) {
                    throw new DatabaseException("INVALID_SALARY", "Invalid salary format: " + salaryObj);
                }
            }
        }
        
        if (map.containsKey("hireDate")) request.setHireDate((String) map.get("hireDate"));
        
        System.out.println("Parsed request: " + request);
        System.out.println("ID: " + request.getId());
        System.out.println("Name: " + request.getName());
        System.out.println("Salary: " + request.getSalary());
        
        return request;
    }
    
    private void checkDatabaseLoaded() {
        if (!databaseController.isDatabaseLoaded()) {
            throw new DatabaseException("NO_DATABASE_LOADED", "No database is currently loaded");
        }
    }
    
    private int parseId(String idParam) {
        try {
            return Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            throw new DatabaseException("INVALID_ID", "Invalid employee ID: " + idParam);
        }
    }
    
    private void validateEmployeeRequest(EmployeeRequest request) {
        System.out.println("=== VALIDATING EMPLOYEE REQUEST ===");
        System.out.println("Request to validate: " + request);
        
        // 🔥 ИСПРАВЛЕНИЕ: для новых сотрудников ID может быть null
        // if (request.getId() == null) {  // ← УБЕРИТЕ ЭТУ ПРОВЕРКУ
        //     throw new DatabaseException("MISSING_REQUIRED_FIELD", "ID is required");
        // }
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new DatabaseException("MISSING_REQUIRED_FIELD", "Name is required");
        }
        if (request.getDepartment() == null || request.getDepartment().trim().isEmpty()) {
            throw new DatabaseException("MISSING_REQUIRED_FIELD", "Department is required");
        }
        if (request.getPosition() == null || request.getPosition().trim().isEmpty()) {
            throw new DatabaseException("MISSING_REQUIRED_FIELD", "Position is required");
        }
        if (request.getSalary() == null) {
            throw new DatabaseException("MISSING_REQUIRED_FIELD", "Salary is required");
        }
        if (request.getHireDate() == null || request.getHireDate().trim().isEmpty()) {
            throw new DatabaseException("MISSING_REQUIRED_FIELD", "Hire date is required");
        }
        
        // Используем ValidationUtil для детальной валидации
        // 🔥 ИСПРАВЛЕНИЕ: пропускаем валидацию ID если он null (новый сотрудник)
        if (request.getId() != null) {
            ValidationUtil.validateEmployeeId(request.getId());
        }
        ValidationUtil.validateName(request.getName());
        ValidationUtil.validateDepartment(request.getDepartment());
        ValidationUtil.validatePosition(request.getPosition());
        ValidationUtil.validateSalary(request.getSalary());
        
        System.out.println("✅ Validation passed");
    }
    
    private Employee convertToEntity(EmployeeRequest request) {
        System.out.println("=== CONVERTING TO ENTITY ===");
        System.out.println("Request: " + request);
        
        LocalDate hireDate = ValidationUtil.parseDate(request.getHireDate());
        
        // 🔥 ИСПРАВЛЕНИЕ: для новых сотрудников создаем с ID = -1
        int employeeId = (request.getId() != null) ? request.getId() : -1;
        
        Employee employee = new Employee(
            employeeId,  // Используем -1 для новых сотрудников
            request.getName(),
            request.getDepartment(),
            request.getPosition(),
            request.getSalary(),
            hireDate
        );
        
        System.out.println("Created employee: " + employee);
        return employee;
    }
    
    private EmployeeResponse convertToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setName(employee.getName());
        response.setDepartment(employee.getDepartment());
        response.setPosition(employee.getPosition());
        response.setSalary(employee.getSalary());
        response.setHireDate(employee.getHireDate().toString());
        return response;
    }
    
    private String handleException(Exception e) {
        try {
            if (e instanceof DatabaseException) {
                DatabaseException de = (DatabaseException) e;
                ErrorResponse error = new ErrorResponse(de.getErrorCode(), de.getMessage());
                return JsonUtil.toJson(error);
            } else {
                ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", 
                    "Internal server error: " + e.getMessage());
                return JsonUtil.toJson(error);
            }
        } catch (Exception jsonError) {
            return "{\"success\":false,\"error\":\"JSON_SERIALIZATION_ERROR\",\"message\":\"Failed to serialize error response\"}";
        }
    }
}
