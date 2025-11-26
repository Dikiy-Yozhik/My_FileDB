package api.dto;

public class UserSession {
    private String username;
    private UserRole role;
    private boolean authenticated;
    private long loginTime;
    
    public UserSession(String username, UserRole role) {
        this.username = username;
        this.role = role;
        this.authenticated = true;
        this.loginTime = System.currentTimeMillis();
    }
    
    // Геттеры
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public boolean isAuthenticated() { return authenticated; }
    public long getLoginTime() { return loginTime; }
    
    // Проверки прав доступа
    public boolean canCreateDatabase() { 
        return role == UserRole.ADMIN; 
    }
    
    public boolean canLoadDatabase() { 
        return role == UserRole.ADMIN || role == UserRole.OPERATOR || role == UserRole.GUEST;
    }
    
    public boolean canDeleteDatabase() { 
        return role == UserRole.ADMIN; 
    }
    
    public boolean canClearDatabase() { 
        return role == UserRole.ADMIN; 
    }
    
    public boolean canCreateEmployee() { 
        return role == UserRole.ADMIN || role == UserRole.OPERATOR; 
    }
    
    public boolean canUpdateEmployee() { 
        return role == UserRole.ADMIN || role == UserRole.OPERATOR; 
    }
    
    public boolean canDeleteEmployee() { 
        return role == UserRole.ADMIN || role == UserRole.OPERATOR; 
    }
    
    public boolean canViewEmployees() { 
        return true; // Все роли могут просматривать
    }
    
    public boolean canSearchEmployees() { 
        return true; // Все роли могут искать
    }
    
    public boolean canCreateBackup() { 
        return role == UserRole.ADMIN; 
    }
    
    public boolean canRestoreBackup() { 
        return role == UserRole.ADMIN; 
    }
    
    public boolean canExportToExcel() { 
        return role == UserRole.ADMIN || role == UserRole.OPERATOR; 
    }
}
