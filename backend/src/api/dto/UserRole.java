package api.dto;

public enum UserRole {
    ADMIN("Администратор"),
    OPERATOR("Оператор"), 
    GUEST("Гость");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
