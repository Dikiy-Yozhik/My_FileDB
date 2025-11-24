package storage;

import exceptions.DatabaseException;
import exceptions.FileAccessException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager implements AutoCloseable {
    private RandomAccessFile file;
    private FileChannel channel;
    private final String filePath;
    private boolean isOpen;
    
    // Буфер для оптимизации операций
    private byte[] readBuffer;
    private long readBufferOffset;
    private boolean readBufferValid;
    
    private static final int BUFFER_SIZE = 8192; // 8KB
    
    public FileManager(String filePath) {
        this.filePath = filePath;
        this.readBuffer = new byte[BUFFER_SIZE];
        this.readBufferOffset = -1;
        this.readBufferValid = false;
        this.isOpen = false;
    }
    
    public void open(boolean createIfNotExists) throws IOException {
        if (isOpen) {
            return;
        }
        
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            if (createIfNotExists) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } else {
                throw new FileAccessException("File not found", filePath);
            }
        }
        
        // Проверяем права доступа
        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            throw new FileAccessException("Insufficient permissions", filePath);
        }
        
        try {
            this.file = new RandomAccessFile(filePath, "rw");
            this.channel = file.getChannel();
            this.isOpen = true;
        } catch (IOException e) {
            throw new FileAccessException("Cannot open file", filePath, e);
        }
    }
    
    public void write(long offset, byte[] data) throws IOException {
        checkOpen();
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative: " + offset);
        }
        
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        try {
            file.seek(offset);
            file.write(data);
            
            // Инвалидируем буфер если он перекрывается с записью
            if (isBufferAffected(offset, data.length)) {
                readBufferValid = false;
            }
            
        } catch (IOException e) {
            throw new FileAccessException("Failed to write data", filePath, e);
        }
    }
    
    public byte[] read(long offset, int length) throws IOException {
        checkOpen();
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative: " + offset);
        }
        
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive: " + length);
        }
        
        // Пытаемся прочитать из буфера если возможно
        if (readBufferValid && isInBuffer(offset, length)) {
            return readFromBuffer(offset, length);
        }
        
        // Читаем напрямую с диска
        try {
            file.seek(offset);
            byte[] data = new byte[length];
            int bytesRead = file.read(data);
            
            if (bytesRead != length) {
                throw new FileAccessException(
                    "Expected to read " + length + " bytes but got " + bytesRead, filePath);
            }
            
            return data;
            
        } catch (IOException e) {
            throw new FileAccessException("Failed to read data", filePath, e);
        }
    }
    
    public byte[] readBuffered(long offset, int length) throws IOException {
        checkOpen();
        
        if (length > BUFFER_SIZE) {
            // Для больших блоков читаем напрямую
            return read(offset, length);
        }
        
        // Определяем блок для буферизации
        long blockOffset = offset - (offset % BUFFER_SIZE);
        
        // Если нужный блок не в буфере, загружаем его
        if (!readBufferValid || readBufferOffset != blockOffset) {
            loadBuffer(blockOffset);
        }
        
        return readFromBuffer(offset, length);
    }
    
    private void loadBuffer(long blockOffset) throws IOException {
        try {
            file.seek(blockOffset);
            int bytesRead = file.read(readBuffer);
            
            if (bytesRead < BUFFER_SIZE) {
                // Достигнут конец файла - заполняем остаток нулями
                for (int i = bytesRead; i < BUFFER_SIZE; i++) {
                    readBuffer[i] = 0;
                }
            }
            
            readBufferOffset = blockOffset;
            readBufferValid = true;
            
        } catch (IOException e) {
            readBufferValid = false;
            throw new FileAccessException("Failed to load buffer", filePath, e);
        }
    }
    
    private boolean isInBuffer(long offset, int length) {
        return offset >= readBufferOffset && 
               (offset + length) <= (readBufferOffset + BUFFER_SIZE);
    }
    
    private boolean isBufferAffected(long offset, int length) {
        return readBufferValid && 
               offset < (readBufferOffset + BUFFER_SIZE) && 
               (offset + length) > readBufferOffset;
    }
    
    private byte[] readFromBuffer(long offset, int length) {
        int bufferPos = (int)(offset - readBufferOffset);
        byte[] result = new byte[length];
        System.arraycopy(readBuffer, bufferPos, result, 0, length);
        return result;
    }
    
    public long getFileSize() throws IOException {
        checkOpen();
        try {
            return file.length();
        } catch (IOException e) {
            throw new FileAccessException("Cannot get file size", filePath, e);
        }
    }
    
    public void setFileSize(long newSize) throws IOException {
        checkOpen();
        try {
            file.setLength(newSize);
            // Инвалидируем буфер так как размер файла изменился
            readBufferValid = false;
        } catch (IOException e) {
            throw new FileAccessException("Cannot resize file", filePath, e);
        }
    }
    
    public void flush() throws IOException {
        if (isOpen) {
            try {
                file.getFD().sync(); // Принудительная запись на диск
            } catch (IOException e) {
                throw new FileAccessException("Cannot flush file", filePath, e);
            }
        }
    }
    
    @Override
    public void close() {
        if (isOpen) {
            try {
                // Сначала закрываем канал, потом файл
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                if (file != null) {
                    file.close();
                }
                isOpen = false;
                readBufferValid = false;
            } catch (IOException e) {
                // Логируем ошибку но не бросаем исключение в close()
                System.err.println("Warning: Error closing file " + filePath + ": " + e.getMessage());
            }
        }
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    private void checkOpen() {
        if (!isOpen) {
            throw new DatabaseException("FILE_NOT_OPEN", "File is not open: " + filePath);
        }
    }
}
