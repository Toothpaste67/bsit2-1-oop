package com.liceo.campustrack.datastructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Resizable array (our own ArrayList).
 * Append is O(1) amortized, index access is O(1), removal is O(n).
 */
public class DynamicArray<T> implements Iterable<T> {

    private Object[] items = new Object[8];
    private int size;

    public void add(T item) {
        if (size == items.length) {
            Object[] bigger = new Object[items.length * 2];
            System.arraycopy(items, 0, bigger, 0, size);
            items = bigger;
        }
        items[size++] = item;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndex(index);
        return (T) items[index];
    }

    public void set(int index, T item) {
        checkIndex(index);
        items[index] = item;
    }

    public T removeAt(int index) {
        T removed = get(index);
        System.arraycopy(items, index + 1, items, index, size - index - 1);
        items[--size] = null;
        return removed;
    }

    public boolean remove(T item) {
        int index = indexOf(item);
        if (index < 0) return false;
        removeAt(index);
        return true;
    }

    /** Linear search, O(n). */
    public int indexOf(T item) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(items[i], item)) return i;
        }
        return -1;
    }

    public boolean contains(T item) { return indexOf(item) >= 0; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    /** New array holding only the items that match. */
    public DynamicArray<T> filter(Predicate<T> condition) {
        DynamicArray<T> result = new DynamicArray<>();
        for (T item : this) {
            if (condition.test(item)) result.add(item);
        }
        return result;
    }

    /** New array holding at most the first {@code max} items. */
    public DynamicArray<T> limit(int max) {
        DynamicArray<T> result = new DynamicArray<>();
        for (int i = 0; i < size && i < max; i++) result.add(get(i));
        return result;
    }

    /** Copy for framework code that insists on java.util.List (JSON output). */
    public List<T> toList() {
        List<T> list = new ArrayList<>(size);
        for (T item : this) list.add(item);
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int next;

            @Override
            public boolean hasNext() { return next < size; }

            @Override
            public T next() {
                if (next >= size) throw new NoSuchElementException();
                return get(next++);
            }
        };
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
        }
    }
}
