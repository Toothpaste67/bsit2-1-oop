package com.liceo.campustrack.repository;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.HashTable;
import com.liceo.campustrack.datastructure.Sorting;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Database-free storage. Records live in a {@link HashTable} keyed by id (O(1) lookup)
 * and every change is written to a plain .txt file: one record per line, fields
 * separated by "|". The file is read back into the hash table at startup.
 */
public abstract class TextFileRepository<T> {

    private static final Logger log = LoggerFactory.getLogger(TextFileRepository.class);

    private final HashTable<Long, T> table = new HashTable<>();
    private final Path file;
    private final String header;
    private long nextId = 1;

    protected TextFileRepository(String dataDir, String fileName, String header) {
        this.file = Path.of(dataDir).resolve(fileName);
        this.header = header;
    }

    protected abstract Long getId(T item);
    protected abstract void setId(T item, Long id);
    protected abstract String[] toFields(T item);
    protected abstract T fromFields(String[] fields);

    /** Hook for subclasses that keep extra lookup tables (for example users by email). */
    protected void reindex(DynamicArray<T> all) {}

    @PostConstruct
    public synchronized void load() {
        table.clear();
        if (Files.exists(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.startsWith("﻿")) line = line.substring(1); // Notepad's UTF-8 marker
                    if (line.isBlank() || line.startsWith("#")) continue;
                    try {
                        T item = fromFields(split(line));
                        table.put(getId(item), item);
                        nextId = Math.max(nextId, getId(item) + 1);
                    } catch (RuntimeException e) {
                        log.warn("Skipping unreadable line in {}: {}", file.getFileName(), line);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + file, e);
            }
        }
        reindex(table.values());
    }

    /** Inserts (when the id is null) or updates, then rewrites the text file. */
    public synchronized T save(T item) {
        if (getId(item) == null) setId(item, nextId++);
        else nextId = Math.max(nextId, getId(item) + 1);
        table.put(getId(item), item);
        changed();
        return item;
    }

    public synchronized T findById(Long id) {
        return id == null ? null : table.get(id);
    }

    public synchronized boolean deleteById(Long id) {
        if (id == null || table.remove(id) == null) return false;
        changed();
        return true;
    }

    /** Removes every matching record and rewrites the file once. @return how many were removed */
    public synchronized int deleteWhere(Predicate<T> condition) {
        int removed = 0;
        for (T item : table.values()) {
            if (condition.test(item)) {
                table.remove(getId(item));
                removed++;
            }
        }
        if (removed > 0) changed();
        return removed;
    }

    /** All records ordered by id (merge sort). */
    public synchronized DynamicArray<T> findAll() {
        return Sorting.mergeSort(table.values(), Comparator.comparing(this::getId));
    }

    public synchronized DynamicArray<T> findWhere(Predicate<T> condition) {
        return findAll().filter(condition);
    }

    public synchronized int count() {
        return table.size();
    }

    private void changed() {
        reindex(table.values());
        persist();
    }

    private void persist() {
        StringBuilder text = new StringBuilder("# ").append(header).append(System.lineSeparator());
        for (T item : findAll()) {
            String[] fields = toFields(item);
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) text.append('|');
                text.append(escape(fields[i]));
            }
            text.append(System.lineSeparator());
        }
        writeFile(file, text.toString());
    }

    /** Writes to a temporary file first so a crash cannot leave a half-written file. */
    static void writeFile(Path target, String content) {
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailed) { // e.g. the file is open in another program
                Files.writeString(target, content, StandardCharsets.UTF_8);
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + target, e);
        }
    }

    // ---- line format ------------------------------------------------------

    static String[] split(String line) {
        String[] raw = line.split("\\|", -1);
        for (int i = 0; i < raw.length; i++) raw[i] = unescape(raw[i]);
        return raw;
    }

    static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '|' -> out.append("\\p");
                case '\n' -> out.append("\\n");
                case '\r' -> { }
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    static String unescape(String value) {
        if (value.indexOf('\\') < 0) return value;
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(next == 'p' ? '|' : next == 'n' ? '\n' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // ---- field conversion helpers for subclasses ---------------------------

    protected static String text(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime time) return time.truncatedTo(ChronoUnit.SECONDS).toString();
        if (value instanceof Enum<?> constant) return constant.name();
        return value.toString();
    }

    /** Field at the given position, or null when it is missing or empty. */
    protected static String field(String[] fields, int index) {
        return index < fields.length && !fields[index].isEmpty() ? fields[index] : null;
    }

    protected static Long number(String[] fields, int index) {
        String value = field(fields, index);
        return value == null ? null : Long.valueOf(value.trim());
    }

    protected static Integer integer(String[] fields, int index) {
        String value = field(fields, index);
        return value == null ? null : Integer.valueOf(value.trim());
    }

    protected static LocalDateTime dateTime(String[] fields, int index) {
        String value = field(fields, index);
        return value == null ? null : LocalDateTime.parse(value.trim());
    }

    protected static LocalDate date(String[] fields, int index) {
        String value = field(fields, index);
        return value == null ? null : LocalDate.parse(value.trim());
    }

    protected static LocalTime time(String[] fields, int index) {
        String value = field(fields, index);
        return value == null ? null : LocalTime.parse(value.trim());
    }

    protected static <E extends Enum<E>> E constant(Class<E> type, String[] fields, int index) {
        String value = field(fields, index);
        return value == null ? null : Enum.valueOf(type, value.trim());
    }
}
