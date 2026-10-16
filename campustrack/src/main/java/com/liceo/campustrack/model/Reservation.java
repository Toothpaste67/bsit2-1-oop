package com.liceo.campustrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
    private Long id;
    private Long facilityId;
    private Long userId;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private ReservationStatus status = ReservationStatus.PENDING;
    private String adminRemarks;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Reservation() {}

    /** Pending and approved requests are the ones that occupy a time slot. */
    public boolean holdsSlot() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.APPROVED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
