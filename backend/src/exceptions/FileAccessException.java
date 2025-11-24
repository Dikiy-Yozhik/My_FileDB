package exceptions;

public class FileAccessException extends DatabaseException {
    private final String filePath;
    
    public FileAccessException(String message, String filePath) {
        super("FILE_ACCESS_ERROR", message);
        this.filePath = filePath;
    }
    
    public FileAccessException(String message, String filePath, Throwable cause) {
        super("FILE_ACCESS_ERROR", message, cause);
        this.filePath = filePath;
    }
    
    public String getFilePath() {
        return filePath;
    }
}
