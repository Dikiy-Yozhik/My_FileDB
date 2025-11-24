package exceptions;

public class DatabaseException extends RuntimeException {
    private final String errorCode;
    
    public DatabaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DatabaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}