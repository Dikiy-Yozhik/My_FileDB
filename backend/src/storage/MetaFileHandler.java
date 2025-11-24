package storage;

import exceptions.DatabaseException;
import model.DatabaseMeta;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MetaFileHandler implements AutoCloseable {
    private FileManager fileManager;
    private DatabaseMeta meta;
    private boolean isOpen;
    
    private static final int META_FILE_SIZE = 22; // Размер meta.db файла
    
    public MetaFileHandler(String filePath) {
        this.fileManager = new FileManager(filePath);
        this.isOpen = false;
    }
    
    public void open(boolean createIfNotExists) throws IOException {
        if (isOpen) return;
        
        fileManager.open(createIfNotExists);
        
        if (createIfNotExists || fileManager.getFileSize() == 0) {
            initializeNewMeta();
        } else {
            loadMetaFromFile();
        }
        
        isOpen = true;
    }
    
    private void initializeNewMeta() throws IOException {
        this.meta = new DatabaseMeta((short)1, 0, 0, RecordFormat.RECORD_SIZE);
        writeMetaToFile();
    }
    
    private void loadMetaFromFile() throws IOException {
        byte[] metaData = fileManager.read(0, META_FILE_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(metaData);
        
        // Сигнатура формата
        byte[] signature = new byte[4];
        buffer.get(signature);
        if (!"MFDB".equals(new String(signature))) {
            throw new DatabaseException("INVALID_DATABASE_FORMAT", "Invalid database signature");
        }
        
        short version = buffer.getShort();
        int recordCount = buffer.getInt();
        long firstFreeOffset = buffer.getLong();
        int recordSize = buffer.getInt();
        
        this.meta = new DatabaseMeta(version, recordCount, firstFreeOffset, recordSize);
    }
    
    private void writeMetaToFile() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(META_FILE_SIZE);
        
        // Сигнатура формата
        buffer.put("MFDB".getBytes());
        // Версия формата
        buffer.putShort(meta.getVersion());
        // Количество записей
        buffer.putInt(meta.getRecordCount());
        // Смещение первой свободной записи
        buffer.putLong(meta.getFirstFreeOffset());
        // Размер записи
        buffer.putInt(meta.getRecordSize());
        
        fileManager.write(0, buffer.array());
    }
    
    public DatabaseMeta getMeta() {
        checkOpen();
        return meta;
    }
    
    public void updateMeta(DatabaseMeta newMeta) throws IOException {
        checkOpen();
        this.meta = newMeta;
        writeMetaToFile();
    }
    
    public void incrementRecordCount() throws IOException {
        checkOpen();
        meta.incrementRecordCount();
        writeMetaToFile();
    }
    
    public void decrementRecordCount() throws IOException {
        checkOpen();
        meta.decrementRecordCount();
        writeMetaToFile();
    }
    
    @Override
    public void close() throws IOException {
        if (isOpen) {
            fileManager.close();
            isOpen = false;
        }
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    private void checkOpen() {
        if (!isOpen) {
            throw new DatabaseException("META_FILE_NOT_OPEN", "Meta file is not open");
        }
    }
}
