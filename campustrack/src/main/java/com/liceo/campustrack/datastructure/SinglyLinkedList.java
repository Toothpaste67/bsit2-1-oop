package com.liceo.campustrack.datastructure;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Singly linked list with head and tail pointers.
 * addFirst / addLast are O(1), which suits append-only logs.
 */
public class SinglyLinkedList<T> implements Iterable<T> {

    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) { this.value = value; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void addFirst(T value) {
        Node<T> node = new Node<>(value);
        node.next = head;
        head = node;
        if (tail == null) tail = node;
        size++;
    }

    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        if (tail == null) head = node;
        else tail.next = node;
        tail = node;
        size++;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    /** The first {@code max} items, starting from the head. */
    public DynamicArray<T> first(int max) {
        DynamicArray<T> result = new DynamicArray<>();
        for (Node<T> node = head; node != null && result.size() < max; node = node.next) {
            result.add(node.value);
        }
        return result;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() { return current != null; }

            @Override
            public T next() {
                if (current == null) throw new NoSuchElementException();
                T value = current.value;
                current = current.next;
                return value;
            }
        };
    }
}
