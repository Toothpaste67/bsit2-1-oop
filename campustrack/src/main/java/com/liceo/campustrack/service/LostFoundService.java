package com.liceo.campustrack.service;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.HashTable;
import com.liceo.campustrack.datastructure.Sorting;
import com.liceo.campustrack.model.ItemStatus;
import com.liceo.campustrack.model.ItemType;
import com.liceo.campustrack.model.LostFoundItem;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.LostFoundRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

@Service
public class LostFoundService {

    public static final String[] CATEGORIES = { "Electronics", "Documents", "Personal Belongings", "Others" };

    private static final int MAX_CLAIM_ATTEMPTS = 5;

    private final LostFoundRepository items;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final Path photoDir;

    // "itemId:userId" -> wrong answers so far. Stops people guessing the answer.
    private final HashTable<String, Integer> failedAttempts = new HashTable<>();

    public LostFoundService(LostFoundRepository items, PasswordEncoder passwordEncoder, AuditService audit,
                            @Value("${campustrack.data-dir}") String dataDir) {
        this.items = items;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.photoDir = Path.of(dataDir).resolve("photos");
    }

    public synchronized LostFoundItem report(User reporter, String title, String description, String category,
                                             ItemType itemType, String location, LocalDate itemDate,
                                             String question, String answer, MultipartFile image) {
        title = clean(title);
        description = clean(description);
        location = clean(location);
        question = clean(question);
        require(!title.isEmpty() && title.length() <= 120, "Item name is required (up to 120 characters)");
        require(!description.isEmpty() && description.length() <= 2000, "Description is required (up to 2000 characters)");
        require(isCategory(category), "Select a category");
        require(itemType != null, "Select whether the item was lost or found");
        require(!location.isEmpty() && location.length() <= 150, "Location is required (up to 150 characters)");
        require(itemDate != null && !itemDate.isAfter(LocalDate.now()), "The date cannot be in the future");
        require(!question.isEmpty() && question.length() <= 255, "A verification question is required");
        require(!normalize(answer).isEmpty(), "A verification answer is required");
        String extension = imageExtension(image);

        LostFoundItem item = new LostFoundItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setItemType(itemType);
        item.setLocation(location);
        item.setItemDate(itemDate);
        item.setReporterId(reporter.getId());
        item.setVerificationQuestion(question);
        item.setVerificationAnswerHash(passwordEncoder.encode(normalize(answer)));
        item.setStatus(ItemStatus.REPORTED);
        items.save(item);

        if (extension != null) {
            try {
                Files.createDirectories(photoDir);
                String fileName = "item-" + item.getId() + "." + extension;
                Files.write(photoDir.resolve(fileName), image.getBytes());
                item.setPhotoFile(fileName);
                items.save(item);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not save the photo", e);
            }
        }
        return item;
    }

    /** Newest first. Any filter may be null or blank, meaning "all". */
    public DynamicArray<LostFoundItem> search(String category, String keyword) {
        String needle = clean(keyword).toLowerCase();
        String wantedCategory = clean(category);
        DynamicArray<LostFoundItem> matches = items.findWhere(i ->
                (wantedCategory.isEmpty() || i.getCategory().equals(wantedCategory))
                        && (needle.isEmpty()
                        || i.getTitle().toLowerCase().contains(needle)
                        || i.getDescription().toLowerCase().contains(needle)));
        return Sorting.mergeSort(matches, Comparator.comparing(LostFoundItem::getCreatedAt).reversed());
    }

    /** @return true when the answer was right and the item is now CLAIMED */
    public synchronized boolean verifyAndClaim(Long itemId, String answer, User claimant) {
        LostFoundItem item = items.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item not found");
        require(item.isOpen(), "This item is no longer open for claims");
        require(!item.getReporterId().equals(claimant.getId()), "You reported this item, so you cannot claim it");

        String attemptKey = itemId + ":" + claimant.getId();
        Integer failures = failedAttempts.get(attemptKey);
        if (failures != null && failures >= MAX_CLAIM_ATTEMPTS) {
            throw new IllegalStateException("Too many wrong answers. Please see the administrator to claim this item.");
        }

        if (item.getVerificationAnswerHash() != null
                && passwordEncoder.matches(normalize(answer), item.getVerificationAnswerHash())) {
            item.setStatus(ItemStatus.CLAIMED);
            item.setClaimedById(claimant.getId());
            items.save(item);
            failedAttempts.remove(attemptKey);
            audit.log(claimant, "ITEM_CLAIM", "Item " + itemId + " (" + item.getTitle() + ") claimed");
            return true;
        }
        failedAttempts.put(attemptKey, failures == null ? 1 : failures + 1);
        return false;
    }

    public synchronized LostFoundItem updateStatus(User admin, Long itemId, ItemStatus status) {
        LostFoundItem item = items.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item not found");
        require(status != null, "Select a status");
        item.setStatus(status);
        if (status == ItemStatus.REPORTED || status == ItemStatus.MATCHED) item.setClaimedById(null);
        items.save(item);
        audit.log(admin, "ITEM_STATUS", "Item " + itemId + " (" + item.getTitle() + ") set to " + status);
        return item;
    }

    public LostFoundItem findById(Long id) { return items.findById(id); }

    public Path photoPath(LostFoundItem item) {
        return item.getPhotoFile() == null ? null : photoDir.resolve(item.getPhotoFile());
    }

    public int openCount() {
        return items.findWhere(LostFoundItem::isOpen).size();
    }

    /** @return file extension for an accepted image, or null when no image was uploaded */
    private static String imageExtension(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;
        String type = image.getContentType() == null ? "" : image.getContentType().toLowerCase();
        return switch (type) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("The photo must be a PNG, JPG, GIF or WEBP image");
        };
    }

    private static boolean isCategory(String category) {
        for (String known : CATEGORIES) {
            if (known.equals(category)) return true;
        }
        return false;
    }

    /** Answers are compared without regard to case or surrounding spaces. */
    private static String normalize(String answer) {
        return clean(answer).toLowerCase();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
