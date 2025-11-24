package storage;

import exceptions.DatabaseException;
import model.Employee;
import util.SerializationUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DataFileHandler implements AutoCloseable {
    private FileManager fileManager;
    private boolean isOpen;
    
    public DataFileHandler(String filePath) {
        this.fileManager = new FileManager(filePath);
        this.isOpen = false;
    }
    
    public void open(boolean createIfNotExists) throws IOException {
        if (isOpen) return;
        fileManager.open(createIfNotExists);
        isOpen = true;
    }
    
    public long writeEmployee(long offset, Employee employee) throws IOException {
        checkOpen();
        
        byte[] recordData = SerializationUtil.serializeEmployee(employee);
        
        if (offset < 0) {
            // Пишем в конец файла
            offset = fileManager.getFileSize();
        }
        
        fileManager.write(offset, recordData);
        return offset;
    }
    
    public Employee readEmployee(long offset) throws IOException {
        checkOpen();
        
        byte[] recordData = fileManager.read(offset, RecordFormat.RECORD_SIZE);
        return SerializationUtil.deserializeEmployee(recordData);
    }
    
    public void updateEmployee(long offset, Employee employee) throws IOException {
        checkOpen();
        
        byte[] recordData = SerializationUtil.serializeEmployee(employee);
        fileManager.write(offset, recordData);
    }
    
    public List<Employee> scanEmployees(Predicate<Employee> filter) throws IOException {
        checkOpen();
        
        List<Employee> results = new ArrayList<>();
        long fileSize = fileManager.getFileSize();
        long currentOffset = 0;
        
        while (currentOffset < fileSize) {
            try {
                Employee employee = readEmployee(currentOffset);
                if (filter.test(employee)) {
                    results.add(employee);
                }
            } catch (Exception e) {
                // Пропускаем битые записи, но логируем
                System.err.println("Warning: Corrupted record at offset " + currentOffset);
            }
            
            currentOffset += RecordFormat.RECORD_SIZE;
        }
        
        return results;
    }
    
    public long findFreeSpace() throws IOException {
        // Пока всегда возвращаем конец файла
        // В будущем можно реализовать поиск в списке свободных блоков
        return fileManager.getFileSize();
    }
    
    public long getFileSize() throws IOException {
        checkOpen();
        return fileManager.getFileSize();
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
            throw new DatabaseException("DATA_FILE_NOT_OPEN", "Data file is not open");
        }
    }
}
