package service;

import api.dto.UserRole;
import api.dto.UserSession;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final Map<String, String> USERS = new HashMap<>();
    private static final Map<String, UserRole> USER_ROLES = new HashMap<>();
    
    static {
        // Предопределенные пользователи (для демонстрации)
        USERS.put("admin", "admin123");
        USERS.put("operator", "operator123"); 
        USERS.put("guest", "guest123");
        
        USER_ROLES.put("admin", UserRole.ADMIN);
        USER_ROLES.put("operator", UserRole.OPERATOR);
        USER_ROLES.put("guest", UserRole.GUEST);
    }
    
    public UserSession authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        
        String storedPassword = USERS.get(username.toLowerCase());
        if (storedPassword != null && storedPassword.equals(password)) {
            UserRole role = USER_ROLES.get(username.toLowerCase());
            return new UserSession(username, role);
        }
        return null;
    }
    
    public UserSession getGuestSession() {
        return new UserSession("guest", UserRole.GUEST);
    }
    
    // Для тестирования - получение сессии по имени пользователя
    public UserSession getSessionForUser(String username) {
        UserRole role = USER_ROLES.get(username.toLowerCase());
        if (role != null) {
            return new UserSession(username, role);
        }
        return getGuestSession();
    }
}
