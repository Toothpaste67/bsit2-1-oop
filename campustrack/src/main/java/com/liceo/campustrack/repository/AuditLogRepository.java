package com.liceo.campustrack.repository;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.SinglyLinkedList;
import com.liceo.campustrack.model.AuditLog;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Append-only audit trail. Kept in a linked list with the newest entry at the head
 * (O(1) insert), and appended to audit-log.txt one line per event.
 */
@Repository
public class AuditLogRepository {

    private final SinglyLinkedList<AuditLog> entries = new SinglyLinkedList<>();
    private final Path file;

    public AuditLogRepository(@Value("${campustrack.data-dir}") String dataDir) {
        this.file = Path.of(dataDir).resolve("audit-log.txt");
    }

    @PostConstruct
    public synchronized void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = TextFileRepository.split(line);
                if (f.length < 4) continue;
                try {
                    entries.addFirst(new AuditLog(LocalDateTime.parse(f[0]), f[1], f[2], f[3]));
                } catch (RuntimeException ignored) {
                    // unreadable line: leave it in the file, skip it in memory
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    public synchronized void append(AuditLog entry) {
        entries.addFirst(entry);
        String line = entry.getTimestamp().truncatedTo(ChronoUnit.SECONDS) + "|"
                + TextFileRepository.escape(entry.getActor()) + "|"
                + TextFileRepository.escape(entry.getAction()) + "|"
                + TextFileRepository.escape(entry.getDetails()) + System.lineSeparator();
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, "# timestamp|actor|action|details" + System.lineSeparator(), StandardCharsets.UTF_8);
            }
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    /** Newest entries first. */
    public synchronized DynamicArray<AuditLog> newest(int max) {
        return entries.first(max);
    }

    public synchronized int count() {
        return entries.size();
    }
}
