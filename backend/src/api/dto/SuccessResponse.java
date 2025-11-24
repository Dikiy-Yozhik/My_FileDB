package api.dto;

public class SuccessResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer total;
    private Integer deletedCount;
    
    public SuccessResponse() {
        this.success = true;
    }
    
    // Конструкторы
    public SuccessResponse(String message) {
        this();
        this.message = message;
    }
    
    public SuccessResponse(String message, T data) {
        this();
        this.message = message;
        this.data = data;
    }
    
    public SuccessResponse(String message, T data, Integer total) {
        this(message, data);
        this.total = total;
    }
    
    // Геттеры и сеттеры
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Integer getTotal() { return total; }
    public Integer getDeletedCount() { return deletedCount; }
    
    public void setMessage(String message) { this.message = message; }
    public void setData(T data) { this.data = data; }
    public void setTotal(Integer total) { this.total = total; }
    public void setDeletedCount(Integer deletedCount) { this.deletedCount = deletedCount; }
}
