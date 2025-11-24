package api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ErrorResponse {
    private boolean success;
    private String error;
    private String message;
    private Map<String, Object> details;
    private String timestamp;
    
    public ErrorResponse(String error, String message) {
        this.success = false;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    public ErrorResponse(String error, String message, Map<String, Object> details) {
        this(error, message);
        this.details = details;
    }
    
    // Геттеры
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }
    public String getTimestamp() { return timestamp; }
    
    // Сеттеры
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
