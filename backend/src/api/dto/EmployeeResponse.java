package api.dto;

public class EmployeeResponse {
    private Integer id;
    private String name;
    private String department;
    private String position;
    private Float salary;
    private String hireDate;
    
    public EmployeeResponse() {}
    
    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public Float getSalary() { return salary; }
    public void setSalary(Float salary) { this.salary = salary; }
    
    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
}
