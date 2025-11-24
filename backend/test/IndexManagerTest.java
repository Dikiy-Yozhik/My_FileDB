package backend.test;

import exceptions.DatabaseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import storage.IndexManager;

public class IndexManagerTest {
    
    public static void main(String[] args) {
        testIndexCreation();
        testAddAndFind();
        testUpdateAndRemove();
        testRehashing();
        testErrorHandling();
        testPerformance();
        System.out.println("✅ Все тесты IndexManager прошли успешно!");
    }
    
    private static void safeDelete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Warning: Could not delete file " + filePath);
        }
    }
    
    static void testIndexCreation() {
        String testFile = "test_data/index_test.db";
        
        try {
            safeDelete(testFile);
            
            IndexManager manager = new IndexManager(testFile);
            manager.open(true);
            
            assert manager.getCapacity() == 16 : "Начальная емкость должна быть 16";
            assert manager.getSize() == 0 : "Начальный размер должен быть 0";
            assert manager.getLoadFactor() == 0.0f : "Начальный load factor должен быть 0";
            
            manager.close();
            safeDelete(testFile);
            
            System.out.println("✅ testIndexCreation - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testIndexCreation - FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void testAddAndFind() {
        String testFile = "test_data/index_add_find.db";
        
        try (IndexManager manager = new IndexManager(testFile)) {
            manager.open(true);
            
            // Добавляем несколько ключей
            manager.add(1, 1000L);
            manager.add(2, 2000L);
            manager.add(3, 3000L);
            
            assert manager.getSize() == 3 : "Размер должен быть 3";
            assert manager.find(1) == 1000L : "Ключ 1 должен возвращать 1000";
            assert manager.find(2) == 2000L : "Ключ 2 должен возвращать 2000";
            assert manager.find(3) == 3000L : "Ключ 3 должен возвращать 3000";
            assert manager.find(999) == null : "Несуществующий ключ должен возвращать null";
            
            System.out.println("✅ testAddAndFind - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testAddAndFind - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testFile);
        }
    }
    
    static void testUpdateAndRemove() {
        String testFile = "test_data/index_update_remove.db";
        
        try (IndexManager manager = new IndexManager(testFile)) {
            manager.open(true);
            
            manager.add(1, 1000L);
            manager.add(2, 2000L);
            
            // Тест обновления
            manager.update(1, 1500L);
            assert manager.find(1) == 1500L : "Ключ 1 должен быть обновлен на 1500";
            
            // Тест удаления
            manager.remove(2);
            assert manager.find(2) == null : "Ключ 2 должен быть удален";
            assert manager.getSize() == 1 : "Размер должен быть 1 после удаления";
            
            // Удаление несуществующего ключа
            manager.remove(999); // Не должно бросать исключение
            
            System.out.println("✅ testUpdateAndRemove - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testUpdateAndRemove - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testFile);
        }
    }
    
    static void testRehashing() {
        String testFile = "test_data/index_rehash.db";
        
        try (IndexManager manager = new IndexManager(testFile)) {
            manager.open(true);
            
            int initialCapacity = manager.getCapacity();
            
            // Добавляем элементы чтобы вызвать рехэширование
            // Порог 0.75, так что добавляем 12 элементов (16 * 0.75 = 12)
            for (int i = 1; i <= 12; i++) {
                manager.add(i, i * 1000L);
            }
            
            assert manager.getCapacity() == initialCapacity * 2 : "Емкость должна удвоиться";
            assert manager.getSize() == 12 : "Размер должен быть 12";
            assert manager.getLoadFactor() < 0.75f : "Load factor должен быть меньше порога";
            
            // Проверяем что все данные доступны после рехэширования
            for (int i = 1; i <= 12; i++) {
                assert manager.find(i) == i * 1000L : "Данные должны быть доступны после рехэширования: key=" + i;
            }
            
            System.out.println("✅ testRehashing - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testRehashing - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testFile);
        }
    }
    
    static void testErrorHandling() {
        String testFile = "test_data/index_errors.db";
        
        try {
            safeDelete(testFile);
            
            // Тест дубликатов
            try (IndexManager manager = new IndexManager(testFile)) {
                manager.open(true);
                manager.add(1, 1000L);
                
                try {
                    manager.add(1, 2000L);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка дубликата");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            // Тест обновления несуществующего ключа
            try (IndexManager manager = new IndexManager(testFile)) {
                manager.open(true);
                
                try {
                    manager.update(999, 1000L);
                    System.out.println("❌ testErrorHandling - FAILED: Должна быть ошибка при обновлении несуществующего ключа");
                    return;
                } catch (DatabaseException e) {
                    // Ожидаемое поведение
                }
            }
            
            System.out.println("✅ testErrorHandling - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testErrorHandling - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testFile);
        }
    }
    
    static void testPerformance() {
        String testFile = "test_data/index_perf.db";
        
        try (IndexManager manager = new IndexManager(testFile)) {
            manager.open(true);
            
            int testSize = 1000;
            long startTime, endTime;
            
            // Тест скорости добавления
            startTime = System.currentTimeMillis();
            for (int i = 0; i < testSize; i++) {
                manager.add(i, i * 100L);
            }
            endTime = System.currentTimeMillis();
            long addTime = endTime - startTime;
            
            // Тест скорости поиска
            startTime = System.currentTimeMillis();
            for (int i = 0; i < testSize; i++) {
                manager.find(i);
            }
            endTime = System.currentTimeMillis();
            long findTime = endTime - startTime;
            
            System.out.println("Производительность для " + testSize + " записей:");
            System.out.println("  Добавление: " + addTime + "ms");
            System.out.println("  Поиск: " + findTime + "ms");
            System.out.println("  Среднее время на операцию: " + (float)(addTime + findTime) / (testSize * 2) + "ms");
            
            assert addTime < 1000 : "Добавление 1000 записей должно занимать < 1 секунды";
            assert findTime < 100 : "Поиск 1000 записей должен занимать < 100ms";
            
            System.out.println("✅ testPerformance - PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ testPerformance - FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            safeDelete(testFile);
        }
    }
}
