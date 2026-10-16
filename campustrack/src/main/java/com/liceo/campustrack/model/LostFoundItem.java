package com.liceo.campustrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LostFoundItem {
    private Long id;
    private String title;
    private String description;
    private String category;
    private ItemType itemType;
    private String location;
    private LocalDate itemDate;
    private String photoFile; // file name inside data/photos, or null
    private ItemStatus status = ItemStatus.REPORTED;
    private Long reporterId;
    private String verificationQuestion;
    private String verificationAnswerHash; // BCrypt hash of the normalized answer
    private Long claimedById;
    private LocalDateTime createdAt = LocalDateTime.now();

    public LostFoundItem() {}

    public boolean isOpen() { return status == ItemStatus.REPORTED || status == ItemStatus.MATCHED; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getItemDate() { return itemDate; }
    public void setItemDate(LocalDate itemDate) { this.itemDate = itemDate; }
    public String getPhotoFile() { return photoFile; }
    public void setPhotoFile(String photoFile) { this.photoFile = photoFile; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public String getVerificationQuestion() { return verificationQuestion; }
    public void setVerificationQuestion(String verificationQuestion) { this.verificationQuestion = verificationQuestion; }
    public String getVerificationAnswerHash() { return verificationAnswerHash; }
    public void setVerificationAnswerHash(String verificationAnswerHash) { this.verificationAnswerHash = verificationAnswerHash; }
    public Long getClaimedById() { return claimedById; }
    public void setClaimedById(Long claimedById) { this.claimedById = claimedById; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
