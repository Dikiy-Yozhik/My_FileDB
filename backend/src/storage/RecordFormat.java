package storage;

public class RecordFormat {
    // Размер записи
    public static final int RECORD_SIZE = 256;
    
    // Максимальные длины строковых полей
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DEPARTMENT_LENGTH = 50;
    public static final int MAX_POSITION_LENGTH = 50;
    
    // Смещения полей в записи
    public static final int ID_OFFSET = 0;           // 4 bytes
    public static final int NAME_OFFSET = 4;         // 100 bytes
    public static final int DEPARTMENT_OFFSET = 104; // 50 bytes  
    public static final int POSITION_OFFSET = 154;   // 50 bytes
    public static final int SALARY_OFFSET = 204;     // 4 bytes
    public static final int HIREDATE_OFFSET = 208;   // 8 bytes (long timestamp)
    public static final int IS_DELETED_OFFSET = 216; // 1 byte
    // Остальные 39 байт - padding
    
    // Кодировки
    public static final String STRING_ENCODING = "UTF-8";
    
    // Формат файла meta.db
    public static final int META_FILE_SIZE = 22;
    public static final String DATABASE_SIGNATURE = "MFDB";
    public static final short DATABASE_VERSION = 1;
    
    // Формат файла index.db
    public static final int INDEX_HEADER_SIZE = 16;
    public static final int INDEX_SLOT_SIZE = 12; // 4 bytes key + 8 bytes offset
    
    private RecordFormat() {
        // Utility class
    }
}
