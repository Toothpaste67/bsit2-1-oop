package com.liceo.campustrack.datastructure;

import java.util.Comparator;
import java.util.function.Predicate;

/** Binary search helpers, O(log n). They only work on arrays that are already sorted. */
public final class Searching {

    private Searching() {}

    /** @return index of an item equal to {@code key}, or -1 when there is none */
    public static <T> int binarySearch(DynamicArray<T> sorted, T key, Comparator<? super T> order) {
        int low = 0;
        int high = sorted.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int comparison = order.compare(sorted.get(middle), key);
            if (comparison == 0) return middle;
            if (comparison < 0) low = middle + 1;
            else high = middle - 1;
        }
        return -1;
    }

    /**
     * Lower-bound search. The condition must be false for a prefix of the array and
     * true for the rest (false, false, ..., true, true).
     *
     * @return index of the first item where the condition is true, or size() if none
     */
    public static <T> int firstIndexWhere(DynamicArray<T> sorted, Predicate<T> condition) {
        int low = 0;
        int high = sorted.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (condition.test(sorted.get(middle))) high = middle;
            else low = middle + 1;
        }
        return low;
    }
}
