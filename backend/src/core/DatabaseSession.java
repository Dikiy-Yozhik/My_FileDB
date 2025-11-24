package core;

import exceptions.DatabaseException;
import storage.DataFileHandler;
import storage.IndexManager;
import storage.MetaFileHandler;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseSession implements AutoCloseable {
    private final String databasePath;
    private DataFileHandler dataHandler;
    private IndexManager indexManager;
    private MetaFileHandler metaHandler;
    private boolean isOpen;
    private final ReadWriteLock lock;
    
    public DatabaseSession(String databasePath) {
        this.databasePath = databasePath;
        this.lock = new ReentrantReadWriteLock();
        this.isOpen = false;
    }
    
    public void open(boolean createIfNotExists) throws IOException {
        if (isOpen) return;
        
        lock.writeLock().lock();
        try {
            this.dataHandler = new DataFileHandler(databasePath + "/data.db");
            this.indexManager = new IndexManager(databasePath + "/index.db");
            this.metaHandler = new MetaFileHandler(databasePath + "/meta.db");
            
            dataHandler.open(createIfNotExists);
            indexManager.open(createIfNotExists);
            metaHandler.open(createIfNotExists);
            
            isOpen = true;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public DataFileHandler getDataHandler() {
        checkOpen();
        return dataHandler;
    }
    
    public IndexManager getIndexManager() {
        checkOpen();
        return indexManager;
    }
    
    public MetaFileHandler getMetaHandler() {
        checkOpen();
        return metaHandler;
    }
    
    public void beginRead() {
        lock.readLock().lock();
    }
    
    public void endRead() {
        lock.readLock().unlock();
    }
    
    public void beginWrite() {
        lock.writeLock().lock();
    }
    
    public void endWrite() {
        lock.writeLock().unlock();
    }
    
    @Override
    public void close() throws IOException {
        if (isOpen) {
            lock.writeLock().lock();
            try {
                if (dataHandler != null) dataHandler.close();
                if (indexManager != null) indexManager.close();
                if (metaHandler != null) metaHandler.close();
                isOpen = false;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public String getDatabasePath() {
        return databasePath;
    }
    
    private void checkOpen() {
        if (!isOpen) {
            throw new DatabaseException("SESSION_NOT_OPEN", "Database session is not open");
        }
    }
}
