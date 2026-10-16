package com.liceo.campustrack.datastructure;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStructureTest {

    @Test
    void dynamicArrayGrowsAndRemoves() {
        DynamicArray<Integer> array = new DynamicArray<>();
        for (int i = 0; i < 100; i++) array.add(i);
        assertEquals(100, array.size());
        assertEquals(42, array.get(42));
        assertTrue(array.remove(Integer.valueOf(42)));
        assertEquals(99, array.size());
        assertEquals(43, array.get(42));
        assertFalse(array.contains(42));
        assertEquals(49, array.filter(n -> n % 2 == 0).size()); // 50 even numbers minus the removed 42
    }

    @Test
    void hashTablePutGetRemoveAcrossResizes() {
        HashTable<String, Integer> table = new HashTable<>();
        for (int i = 0; i < 1000; i++) table.put("key" + i, i);
        assertEquals(1000, table.size());
        assertEquals(777, table.get("key777"));
        assertEquals(777, table.put("key777", -1)); // update returns the old value
        assertEquals(-1, table.get("key777"));
        assertEquals(1000, table.size());
        assertEquals(-1, table.remove("key777"));
        assertNull(table.get("key777"));
        assertFalse(table.containsKey("key777"));
        assertEquals(999, table.size());
        assertEquals(999, table.values().size());
    }

    @Test
    void hashSetRejectsDuplicates() {
        ChainedHashSet<Long> set = new ChainedHashSet<>();
        assertTrue(set.add(5L));
        assertFalse(set.add(5L));
        assertTrue(set.contains(5L));
        assertTrue(set.remove(5L));
        assertTrue(set.isEmpty());
    }

    @Test
    void queueIsFirstInFirstOut() {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");
        assertEquals("a", queue.peek());
        assertEquals("a", queue.dequeue());
        assertEquals("b", queue.dequeue());
        assertEquals("c", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void linkedListAddFirstKeepsNewestAtHead() {
        SinglyLinkedList<Integer> list = new SinglyLinkedList<>();
        list.addFirst(1);
        list.addFirst(2);
        list.addLast(0);
        DynamicArray<Integer> firstTwo = list.first(2);
        assertEquals(2, firstTwo.get(0));
        assertEquals(1, firstTwo.get(1));
        assertEquals(3, list.size());
    }

    @Test
    void mergeSortSortsAndIsStable() {
        DynamicArray<String> words = new DynamicArray<>();
        for (String word : new String[] { "pear", "fig", "apple", "kiwi", "plum", "date" }) words.add(word);

        DynamicArray<String> byLength = Sorting.mergeSort(words, Comparator.comparing(String::length));
        // equal lengths keep their original order: fig; pear, kiwi, plum, date; apple
        assertEquals("fig", byLength.get(0));
        assertEquals("pear", byLength.get(1));
        assertEquals("kiwi", byLength.get(2));
        assertEquals("plum", byLength.get(3));
        assertEquals("date", byLength.get(4));
        assertEquals("apple", byLength.get(5));
        assertEquals("pear", words.get(0)); // input untouched
    }

    @Test
    void binarySearchFindsItemsAndLowerBound() {
        DynamicArray<Integer> sorted = new DynamicArray<>();
        for (int i = 0; i < 50; i++) sorted.add(i * 2); // 0, 2, 4, ..., 98

        assertEquals(10, Searching.binarySearch(sorted, 20, Comparator.naturalOrder()));
        assertEquals(-1, Searching.binarySearch(sorted, 21, Comparator.naturalOrder()));
        assertEquals(11, Searching.firstIndexWhere(sorted, n -> n > 20));
        assertEquals(0, Searching.firstIndexWhere(sorted, n -> n >= 0));
        assertEquals(50, Searching.firstIndexWhere(sorted, n -> n > 1000));
    }
}
