package model;

public class DatabaseMeta {
    private short version;
    private int recordCount;
    private long firstFreeOffset;
    private int recordSize;
    
    public DatabaseMeta(short version, int recordCount, long firstFreeOffset, int recordSize) {
        this.version = version;
        this.recordCount = recordCount;
        this.firstFreeOffset = firstFreeOffset;
        this.recordSize = recordSize;
    }
    
    // Геттеры и сеттеры
    public short getVersion() { return version; }
    public int getRecordCount() { return recordCount; }
    public long getFirstFreeOffset() { return firstFreeOffset; }
    public int getRecordSize() { return recordSize; }
    
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    public void setFirstFreeOffset(long firstFreeOffset) { this.firstFreeOffset = firstFreeOffset; }
    
    // Бизнес-методы
    public void incrementRecordCount() { recordCount++; }
    public void decrementRecordCount() { recordCount--; }
}
