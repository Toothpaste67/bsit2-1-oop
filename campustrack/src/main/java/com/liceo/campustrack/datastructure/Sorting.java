package com.liceo.campustrack.datastructure;

import java.util.Comparator;

/** Sorting algorithms. This is what replaces SQL "ORDER BY". */
public final class Sorting {

    private Sorting() {}

    /**
     * Merge sort: O(n log n) in every case and stable (equal items keep their order).
     * Returns a new sorted array; the input is left untouched.
     */
    public static <T> DynamicArray<T> mergeSort(DynamicArray<T> input, Comparator<? super T> order) {
        Object[] items = new Object[input.size()];
        for (int i = 0; i < items.length; i++) items[i] = input.get(i);

        sort(items, new Object[items.length], 0, items.length, order);

        DynamicArray<T> sorted = new DynamicArray<>();
        for (Object item : items) sorted.add(cast(item));
        return sorted;
    }

    /** Sorts items[from, to). */
    private static <T> void sort(Object[] items, Object[] buffer, int from, int to, Comparator<? super T> order) {
        if (to - from < 2) return;
        int middle = (from + to) >>> 1;
        sort(items, buffer, from, middle, order);
        sort(items, buffer, middle, to, order);
        merge(items, buffer, from, middle, to, order);
    }

    private static <T> void merge(Object[] items, Object[] buffer, int from, int middle, int to,
                                  Comparator<? super T> order) {
        int left = from;
        int right = middle;
        int out = from;
        while (left < middle && right < to) {
            // "<=" takes from the left half on ties, which keeps the sort stable
            if (order.compare(Sorting.<T>cast(items[left]), Sorting.<T>cast(items[right])) <= 0) {
                buffer[out++] = items[left++];
            } else {
                buffer[out++] = items[right++];
            }
        }
        while (left < middle) buffer[out++] = items[left++];
        while (right < to) buffer[out++] = items[right++];
        System.arraycopy(buffer, from, items, from, to - from);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object item) {
        return (T) item;
    }
}
