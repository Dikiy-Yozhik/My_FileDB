package storage;

import exceptions.DatabaseException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class IndexManager implements AutoCloseable {
    private FileManager fileManager;
    private Map<Integer, Long> ramIndex; // In-memory копия для скорости
    private boolean isOpen;
    
    // Формат файла индекса
    private static final int HEADER_SIZE = 16;
    private static final int SLOT_SIZE = 12; // 4 байта key + 8 байт offset
    
    // Заголовок индекса
    private int capacity;
    private int size;
    private float loadFactorThreshold;
    
    public IndexManager(String filePath) {
        this.fileManager = new FileManager(filePath);
        this.ramIndex = new HashMap<>();
        this.isOpen = false;
        this.capacity = 16; // Начальная емкость по умолчанию
        this.size = 0;
        this.loadFactorThreshold = 0.75f;
    }
    
    public void open(boolean createIfNotExists) throws IOException {
        if (isOpen) return;
        
        fileManager.open(createIfNotExists);
        
        if (createIfNotExists || fileManager.getFileSize() == 0) {
            initializeNewIndex();
        } else {
            loadIndexFromFile();
        }
        
        isOpen = true;
    }
    
    private void initializeNewIndex() throws IOException {
        // Записываем заголовок
        writeHeader();
        
        // Инициализируем пустые слоты
        for (int i = 0; i < capacity; i++) {
            writeSlot(i, -1, 0);
        }
        
        ramIndex.clear();
        size = 0;
    }
    
    private void loadIndexFromFile() throws IOException {
        // Читаем заголовок
        byte[] headerData = fileManager.read(0, HEADER_SIZE);
        ByteBuffer header = ByteBuffer.wrap(headerData);
        
        // Пропускаем сигнатуру и версию
        header.getShort(); // version
        header.getShort(); // reserved
        
        capacity = header.getInt();
        size = header.getInt();
        loadFactorThreshold = header.getFloat();
        
        // Загружаем данные в RAM
        ramIndex.clear();
        for (int i = 0; i < capacity; i++) {
            IndexSlot slot = readSlot(i);
            if (slot.key != -1) {
                ramIndex.put(slot.key, slot.offset);
            }
        }
    }
    
    public void add(int key, long offset) throws IOException {
        checkOpen();
        
        if (ramIndex.containsKey(key)) {
            throw new DatabaseException("DUPLICATE_KEY", 
                "Index already contains key: " + key);
        }
        
        // Проверяем是否需要 рехэширование
        if ((float) size / capacity >= loadFactorThreshold) {
            rehash();
        }
        
        int hash = hashFunction(key);
        int attempts = 0;
        
        // Линейное пробирование
        while (attempts < capacity) {
            IndexSlot slot = readSlot(hash);
            
            if (slot.key == -1) {
                // Нашли свободный слот
                writeSlot(hash, key, offset);
                ramIndex.put(key, offset);
                size++;
                return;
            }
            
            hash = (hash + 1) % capacity;
            attempts++;
        }
        
        throw new DatabaseException("INDEX_FULL", "Index is full, cannot add key: " + key);
    }
    
    public Long find(int key) {
        checkOpen();
        return ramIndex.get(key);
    }
    
    public boolean contains(int key) {
        checkOpen();
        return ramIndex.containsKey(key);
    }
    
    public void remove(int key) throws IOException {
        checkOpen();
        
        if (!ramIndex.containsKey(key)) {
            return;
        }
        
        int hash = hashFunction(key);
        int attempts = 0;
        
        // Находим слот для удаления
        while (attempts < capacity) {
            IndexSlot slot = readSlot(hash);
            
            if (slot.key == key) {
                // Удаляем запись
                writeSlot(hash, -1, 0);
                ramIndex.remove(key);
                size--;
                return;
            }
            
            hash = (hash + 1) % capacity;
            attempts++;
        }
    }
    
    public void update(int key, long newOffset) throws IOException {
        checkOpen();
        
        if (!ramIndex.containsKey(key)) {
            throw new DatabaseException("KEY_NOT_FOUND", 
                "Cannot update, key not found: " + key);
        }
        
        int hash = hashFunction(key);
        int attempts = 0;
        
        // Находим слот для обновления
        while (attempts < capacity) {
            IndexSlot slot = readSlot(hash);
            
            if (slot.key == key) {
                writeSlot(hash, key, newOffset);
                ramIndex.put(key, newOffset);
                return;
            }
            
            hash = (hash + 1) % capacity;
            attempts++;
        }
    }
    
    private void rehash() throws IOException {
        int newCapacity = capacity * 2;
        
        // Сохраняем текущие данные
        Map<Integer, Long> oldData = new HashMap<>(ramIndex);
        
        // Обновляем емкость
        capacity = newCapacity;
        size = 0;
        ramIndex.clear();
        
        // Перезаписываем заголовок
        writeHeader();
        
        // Инициализируем новые пустые слоты
        for (int i = 0; i < capacity; i++) {
            writeSlot(i, -1, 0);
        }
        
        // Перестраиваем индекс с новой емкостью
        for (Map.Entry<Integer, Long> entry : oldData.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }
    
    private int hashFunction(int key) {
        // Самая простая - модуль
        return key % capacity;
    }
    
    private IndexSlot readSlot(int slotIndex) throws IOException {
        long offset = HEADER_SIZE + (long) slotIndex * SLOT_SIZE;
        byte[] slotData = fileManager.read(offset, SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(slotData);
        
        int key = buffer.getInt();
        long value = buffer.getLong();
        
        return new IndexSlot(key, value);
    }
    
    private void writeSlot(int slotIndex, int key, long offset) throws IOException {
        long fileOffset = HEADER_SIZE + (long) slotIndex * SLOT_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(SLOT_SIZE);
        buffer.putInt(key);
        buffer.putLong(offset);
        
        fileManager.write(fileOffset, buffer.array());
    }
    
    private void writeHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.putShort((short) 1); // version
        header.putShort((short) 0); // reserved
        header.putInt(capacity);
        header.putInt(size);
        header.putFloat(loadFactorThreshold);
        
        fileManager.write(0, header.array());
    }
    
    public int getSize() {
        return size;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public float getLoadFactor() {
        return (float) size / capacity;
    }
    
    @Override
    public void close() throws IOException {
        if (isOpen) {
            fileManager.close();
            isOpen = false;
        }
    }
    
    private void checkOpen() {
        if (!isOpen) {
            throw new DatabaseException("INDEX_NOT_OPEN", "Index is not open");
        }
    }
    
    // Вспомогательный класс для представления слота индекса
    private static class IndexSlot {
        final int key;
        final long offset;
        
        IndexSlot(int key, long offset) {
            this.key = key;
            this.offset = offset;
        }
    }
}
