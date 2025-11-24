package backend.test;

import exceptions.DatabaseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import storage.FileManager;

public class FileManagerTest {
    
    public static void main(String[] args) {
        testFileCreation();
        testReadWriteOperations();
        testBufferedOperations();
        testErrorHandling();
        testFileSizeOperations();
        System.out.println("✅ Все тесты FileManager прошли успешно!");
    }
    
    // Вспомогательный метод для безопасного удаления файлов
    private static void safeDelete(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                // Несколько попыток удаления с задержкой
                for (int i = 0; i < 3; i++) {
                    try {
                        Files.delete(path);
                        break;
                    } catch (IOException e) {
                        if (i == 2) throw e; // Последняя попытка
                        try { Thread.sleep(10); } catch (InterruptedException ie) {}
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not delete file " + filePath + ": " + e.getMessage());
        }
    }
    
    static void testFileCreation() {
        String testFile = "test_data/file_manager_test.db";
        
        try {
            safeDelete(testFile);
            
            FileManager manager = new FileManager(testFile);
            manager.open(true);
            
            assert manager.isOpen() : "FileManager должен быть открыт";
            assert Files.exists(Paths.get(testFile)) : "Файл должен быть создан";
            
            manager.close();
            assert !manager.isOpen() : "FileManager должен быть закрыт";
            
            safeDelete(testFile);
            System.out.println("✅ testFileCreation - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testFileCreation - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testReadWriteOperations() {
        String testFile = "test_data/read_write_test.db";
        
        try {
            safeDelete(testFile);
            
            try (FileManager manager = new FileManager(testFile)) {
                manager.open(true);
                
                // Тест записи и чтения
                byte[] testData = "Hello, FileManager!".getBytes();
                manager.write(0, testData);
                
                byte[] readData = manager.read(0, testData.length);
                assert java.util.Arrays.equals(testData, readData) : "Прочитанные данные должны совпадать с записанными";
                
                // Тест записи в середину файла
                byte[] moreData = "More data".getBytes();
                manager.write(100, moreData);
                
                byte[] readMoreData = manager.read(100, moreData.length);
                assert java.util.Arrays.equals(moreData, readMoreData) : "Данные в середине файла должны читаться корректно";
            } // Автоматическое закрытие
            
            safeDelete(testFile);
            System.out.println("✅ testReadWriteOperations - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testReadWriteOperations - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testBufferedOperations() {
        String testFile = "test_data/buffered_test.db";
        
        try {
            safeDelete(testFile);
            
            try (FileManager manager = new FileManager(testFile)) {
                manager.open(true);
                
                // Записываем данные в разных местах буфера
                byte[] data1 = "Data block 1".getBytes();
                byte[] data2 = "Data block 2".getBytes();
                byte[] data3 = "Data block 3".getBytes();
                
                manager.write(0, data1);
                manager.write(100, data2);
                manager.write(200, data3);
                
                // Читаем через буферизованные операции
                byte[] read1 = manager.readBuffered(0, data1.length);
                byte[] read2 = manager.readBuffered(100, data2.length);
                byte[] read3 = manager.readBuffered(200, data3.length);
                
                assert java.util.Arrays.equals(data1, read1) : "Буферизованное чтение 1 должно работать";
                assert java.util.Arrays.equals(data2, read2) : "Буферизованное чтение 2 должно работать";
                assert java.util.Arrays.equals(data3, read3) : "Буферизованное чтение 3 должно работать";
            }
            
            safeDelete(testFile);
            System.out.println("✅ testBufferedOperations - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testBufferedOperations - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testErrorHandling() {
        String testFile = "test_data/error_test.db";
        
        try {
            safeDelete(testFile);
            
            // Тест 1: Попытка чтения/записи без открытия файла
            try (FileManager manager1 = new FileManager(testFile)) {
                try {
                    manager1.read(0, 10);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка при чтении без открытия");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            // Тест 2: Попытка открытия несуществующего файла без создания
            try (FileManager manager2 = new FileManager(testFile)) {
                try {
                    manager2.open(false);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка при открытии несуществующего файла");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            // Тест 3: Попытка чтения за пределами файла
            try (FileManager manager3 = new FileManager(testFile)) {
                manager3.open(true);
                
                try {
                    manager3.read(1000, 10);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка при чтении за пределами файла");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            safeDelete(testFile);
            System.out.println("✅ testErrorHandling - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testErrorHandling - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testFileSizeOperations() {
        String testFile = "test_data/size_test.db";
        
        try {
            safeDelete(testFile);
            
            try (FileManager manager = new FileManager(testFile)) {
                manager.open(true);
                
                // Проверяем начальный размер
                long initialSize = manager.getFileSize();
                assert initialSize == 0 : "Начальный размер должен быть 0";
                
                // Увеличиваем размер файла
                manager.setFileSize(1000);
                long newSize = manager.getFileSize();
                assert newSize == 1000 : "Размер файла должен быть 1000";
                
                // Записываем данные и проверяем что размер автоматически увеличивается
                byte[] data = new byte[500];
                manager.write(1500, data);
                long finalSize = manager.getFileSize();
                assert finalSize >= 2000 : "Размер файла должен увеличиться после записи";
            }
            
            safeDelete(testFile);
            System.out.println("✅ testFileSizeOperations - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testFileSizeOperations - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
