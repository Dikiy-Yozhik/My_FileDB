package exceptions;

public class ValidationException extends DatabaseException {
    private final String field;
    private final String constraint;
    
    public ValidationException(String field, String constraint, String message) {
        super("VALIDATION_ERROR", message);
        this.field = field;
        this.constraint = constraint;
    }
    
    public String getField() { return field; }
    public String getConstraint() { return constraint; }
}
