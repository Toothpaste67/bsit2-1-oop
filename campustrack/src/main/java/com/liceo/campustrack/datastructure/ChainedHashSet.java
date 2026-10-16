package com.liceo.campustrack.datastructure;

/** Set of unique values built on top of {@link HashTable}. O(1) average add / contains. */
public class ChainedHashSet<T> {

    private final HashTable<T, Boolean> table = new HashTable<>();

    /** @return false when the value was already in the set */
    public boolean add(T value) {
        return table.put(value, Boolean.TRUE) == null;
    }

    public boolean contains(T value) { return table.containsKey(value); }
    public boolean remove(T value) { return table.remove(value) != null; }
    public int size() { return table.size(); }
    public boolean isEmpty() { return table.isEmpty(); }
    public DynamicArray<T> values() { return table.keys(); }
}
