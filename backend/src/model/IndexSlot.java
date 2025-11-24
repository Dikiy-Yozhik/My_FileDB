package model;

public class IndexSlot {
    private final int key;
    private final long offset;
    
    public IndexSlot(int key, long offset) {
        this.key = key;
        this.offset = offset;
    }
    
    public int getKey() { return key; }
    public long getOffset() { return offset; }
    
    public boolean isEmpty() {
        return key == -1;
    }
    
    public static IndexSlot emptySlot() {
        return new IndexSlot(-1, 0);
    }
}
