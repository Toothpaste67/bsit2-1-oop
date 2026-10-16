package com.liceo.campustrack.datastructure;

import java.util.Objects;

/**
 * Hash table with separate chaining (our own HashMap).
 * put / get / remove are O(1) on average. This is what replaces the
 * database primary-key and unique-column lookups.
 */
public class HashTable<K, V> {

    private static final double MAX_LOAD_FACTOR = 0.75;

    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Node<K, V>[] buckets = newBuckets(16);
    private int size;

    /** Stores the value and returns the previous one for that key (or null). */
    public V put(K key, V value) {
        int index = indexFor(key, buckets.length);
        for (Node<K, V> node = buckets[index]; node != null; node = node.next) {
            if (Objects.equals(node.key, key)) {
                V old = node.value;
                node.value = value;
                return old;
            }
        }
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;
        if (size > buckets.length * MAX_LOAD_FACTOR) resize();
        return null;
    }

    public V get(K key) {
        for (Node<K, V> node = buckets[indexFor(key, buckets.length)]; node != null; node = node.next) {
            if (Objects.equals(node.key, key)) return node.value;
        }
        return null;
    }

    public boolean containsKey(K key) {
        for (Node<K, V> node = buckets[indexFor(key, buckets.length)]; node != null; node = node.next) {
            if (Objects.equals(node.key, key)) return true;
        }
        return false;
    }

    public V remove(K key) {
        int index = indexFor(key, buckets.length);
        Node<K, V> previous = null;
        for (Node<K, V> node = buckets[index]; node != null; previous = node, node = node.next) {
            if (Objects.equals(node.key, key)) {
                if (previous == null) buckets[index] = node.next;
                else previous.next = node.next;
                size--;
                return node.value;
            }
        }
        return null;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        buckets = newBuckets(16);
        size = 0;
    }

    public DynamicArray<K> keys() {
        DynamicArray<K> result = new DynamicArray<>();
        for (Node<K, V> bucket : buckets) {
            for (Node<K, V> node = bucket; node != null; node = node.next) result.add(node.key);
        }
        return result;
    }

    public DynamicArray<V> values() {
        DynamicArray<V> result = new DynamicArray<>();
        for (Node<K, V> bucket : buckets) {
            for (Node<K, V> node = bucket; node != null; node = node.next) result.add(node.value);
        }
        return result;
    }

    private void resize() {
        Node<K, V>[] bigger = newBuckets(buckets.length * 2);
        for (Node<K, V> bucket : buckets) {
            Node<K, V> node = bucket;
            while (node != null) {
                Node<K, V> next = node.next;
                int index = indexFor(node.key, bigger.length);
                node.next = bigger[index];
                bigger[index] = node;
                node = next;
            }
        }
        buckets = bigger;
    }

    private static int indexFor(Object key, int capacity) {
        int hash = key == null ? 0 : key.hashCode();
        hash ^= (hash >>> 16); // spread the high bits so they affect the bucket index
        return (hash & 0x7fffffff) % capacity;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Node<K, V>[] newBuckets(int capacity) {
        return (Node<K, V>[]) new Node[capacity];
    }
}
