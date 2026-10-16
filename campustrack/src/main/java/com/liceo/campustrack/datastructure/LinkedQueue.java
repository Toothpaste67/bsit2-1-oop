package com.liceo.campustrack.datastructure;

import java.util.NoSuchElementException;

/** FIFO queue on a singly linked list. enqueue and dequeue are both O(1). */
public class LinkedQueue<T> {

    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) { this.value = value; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void enqueue(T value) {
        Node<T> node = new Node<>(value);
        if (tail == null) head = node;
        else tail.next = node;
        tail = node;
        size++;
    }

    public T dequeue() {
        if (head == null) throw new NoSuchElementException("Queue is empty");
        T value = head.value;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return value;
    }

    public T peek() { return head == null ? null : head.value; }
    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }
}
