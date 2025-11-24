package storage;

import exceptions.DatabaseException;
import model.IndexSlot;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class IndexManager implements AutoCloseable {
    private FileManager fileManager;
    private Map<Integer, Long> ramIndex; // In-memory копия для скорости
    private boolean isOpen;
    
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
            writeSlot(i, IndexSlot.emptySlot());
        }
        
        ramIndex.clear();
        size = 0;
    }
    
    private void loadIndexFromFile() throws IOException {
        // Читаем заголовок
        byte[] headerData = fileManager.read(0, RecordFormat.INDEX_HEADER_SIZE);
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
            if (!slot.isEmpty()) {
                ramIndex.put(slot.getKey(), slot.getOffset());
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
            
            if (slot.isEmpty()) {
                // Нашли свободный слот
                writeSlot(hash, new IndexSlot(key, offset));
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
            
            if (slot.getKey() == key) {
                // Удаляем запись
                writeSlot(hash, IndexSlot.emptySlot());
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
            
            if (slot.getKey() == key) {
                writeSlot(hash, new IndexSlot(key, newOffset));
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
            writeSlot(i, IndexSlot.emptySlot());
        }
        
        // Перестраиваем индекс с новой емкостью
        for (Map.Entry<Integer, Long> entry : oldData.entrySet()) {
            addWithoutRehash(entry.getKey(), entry.getValue());
        }
    }
    
    // Внутренний метод для добавления без проверки рехэширования
    private void addWithoutRehash(int key, long offset) throws IOException {
        int hash = hashFunction(key);
        int attempts = 0;
        
        while (attempts < capacity) {
            IndexSlot slot = readSlot(hash);
            
            if (slot.isEmpty()) {
                writeSlot(hash, new IndexSlot(key, offset));
                ramIndex.put(key, offset);
                size++;
                return;
            }
            
            hash = (hash + 1) % capacity;
            attempts++;
        }
        
        throw new DatabaseException("INDEX_FULL", "Index is full during rehash");
    }
    
    private int hashFunction(int key) {
        // Простая хэш-функция с умножением на простое число
        return (key * 31) % capacity;
    }
    
    private IndexSlot readSlot(int slotIndex) throws IOException {
        long offset = RecordFormat.INDEX_HEADER_SIZE + (long) slotIndex * RecordFormat.INDEX_SLOT_SIZE;
        byte[] slotData = fileManager.read(offset, RecordFormat.INDEX_SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(slotData);
        
        int key = buffer.getInt();
        long value = buffer.getLong();
        
        return new IndexSlot(key, value);
    }
    
    private void writeSlot(int slotIndex, IndexSlot slot) throws IOException {
        long fileOffset = RecordFormat.INDEX_HEADER_SIZE + (long) slotIndex * RecordFormat.INDEX_SLOT_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(RecordFormat.INDEX_SLOT_SIZE);
        buffer.putInt(slot.getKey());
        buffer.putLong(slot.getOffset());
        
        fileManager.write(fileOffset, buffer.array());
    }
    
    private void writeHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(RecordFormat.INDEX_HEADER_SIZE);
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
}
