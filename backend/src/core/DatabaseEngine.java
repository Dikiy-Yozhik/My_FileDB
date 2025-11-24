package core;

import exceptions.DatabaseException;
import model.Employee;
import java.io.IOException;
import java.util.List;

public class DatabaseEngine implements AutoCloseable {
    private DatabaseSession session;
    private boolean isOpen;
    
    public DatabaseEngine(String databasePath) {
        this.session = new DatabaseSession(databasePath);
        this.isOpen = false;
    }
    
    public void open(boolean createIfNotExists) throws IOException {
        if (isOpen) return;
        
        session.open(createIfNotExists);
        isOpen = true;
    }
    
    // CREATE - Добавление сотрудника
    public void addEmployee(Employee employee) throws IOException {
        checkOpen();
        session.beginWrite();
        
        try {
            // Валидация данных
            employee.validate();
            
            // Проверяем уникальность ID
            if (session.getIndexManager().contains(employee.getId())) {
                throw new DatabaseException("DUPLICATE_ID", 
                    "Employee with ID " + employee.getId() + " already exists");
            }
            
            // Ищем свободное место
            long offset = session.getDataHandler().findFreeSpace();
            
            // Записываем данные
            session.getDataHandler().writeEmployee(offset, employee);
            
            // Добавляем в индекс
            session.getIndexManager().add(employee.getId(), offset);
            
            // Обновляем метаданные
            session.getMetaHandler().incrementRecordCount();
            
        } finally {
            session.endWrite();
        }
    }
    
    // READ - Поиск по ID (ключевое поле)
    public Employee findEmployeeById(int id) throws IOException {
        checkOpen();
        session.beginRead();
        
        try {
            Long offset = session.getIndexManager().find(id);
            if (offset == null) {
                return null;
            }
            
            Employee employee = session.getDataHandler().readEmployee(offset);
            return employee.isDeleted() ? null : employee;
            
        } finally {
            session.endRead();
        }
    }
    
    // READ - Поиск по неключевым полям
    public List<Employee> findEmployeesByDepartment(String department) throws IOException {
        checkOpen();
        return session.getDataHandler().scanEmployees(
            emp -> department.equals(emp.getDepartment()) && !emp.isDeleted());
    }
    
    public List<Employee> findEmployeesByName(String name) throws IOException {
        checkOpen();
        return session.getDataHandler().scanEmployees(
            emp -> emp.getName().toLowerCase().contains(name.toLowerCase()) && !emp.isDeleted());
    }
    
    public List<Employee> findEmployeesByPosition(String position) throws IOException {
        checkOpen();
        return session.getDataHandler().scanEmployees(
            emp -> position.equals(emp.getPosition()) && !emp.isDeleted());
    }
    
    // READ - Получить всех сотрудников
    public List<Employee> getAllEmployees() throws IOException {
        checkOpen();
        return session.getDataHandler().scanEmployees(emp -> !emp.isDeleted());
    }
    
    // UPDATE - Редактирование сотрудника
    public void updateEmployee(Employee updatedEmployee) throws IOException {
        checkOpen();
        session.beginWrite();
        
        try {
            updatedEmployee.validate();
            
            // Находим существующую запись
            Long offset = session.getIndexManager().find(updatedEmployee.getId());
            if (offset == null) {
                throw new DatabaseException("EMPLOYEE_NOT_FOUND", 
                    "Employee with ID " + updatedEmployee.getId() + " not found");
            }
            
            // Перезаписываем данные
            session.getDataHandler().updateEmployee(offset, updatedEmployee);
            
        } finally {
            session.endWrite();
        }
    }
    
    // DELETE - Удаление по ID
    public boolean deleteEmployeeById(int id) throws IOException {
        checkOpen();
        session.beginWrite();
        
        try {
            Long offset = session.getIndexManager().find(id);
            if (offset == null) {
                return false;
            }
            
            // Логическое удаление
            Employee employee = session.getDataHandler().readEmployee(offset);
            employee.setDeleted(true);
            session.getDataHandler().updateEmployee(offset, employee);
            
            // Удаляем из индекса
            session.getIndexManager().remove(id);
            
            // Обновляем метаданные
            session.getMetaHandler().decrementRecordCount();
            
            return true;
            
        } finally {
            session.endWrite();
        }
    }
    
    // DELETE - Удаление по неключевому полю
    public int deleteEmployeesByDepartment(String department) throws IOException {
        checkOpen();
        
        List<Employee> employeesToDelete = findEmployeesByDepartment(department);
        
        for (Employee emp : employeesToDelete) {
            deleteEmployeeById(emp.getId());
        }
        
        return employeesToDelete.size();
    }
    
    // Статистика
    public int getEmployeeCount() throws IOException {
        checkOpen();
        return session.getMetaHandler().getMeta().getRecordCount();
    }
    
    public long getDatabaseSize() throws IOException {
        checkOpen();
        return session.getDataHandler().getFileSize();
    }
    
    public String getDatabasePath() {
        return session.getDatabasePath();
    }
    
    @Override
    public void close() throws IOException {
        if (isOpen) {
            session.close();
            isOpen = false;
        }
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    private void checkOpen() {
        if (!isOpen) {
            throw new DatabaseException("DATABASE_NOT_OPEN", "Database is not open");
        }
    }
}
