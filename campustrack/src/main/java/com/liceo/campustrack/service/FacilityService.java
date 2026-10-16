package com.liceo.campustrack.service;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.Searching;
import com.liceo.campustrack.datastructure.Sorting;
import com.liceo.campustrack.model.Facility;
import com.liceo.campustrack.model.Reservation;
import com.liceo.campustrack.model.ReservationStatus;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.FacilityRepository;
import com.liceo.campustrack.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

/** Facilities and facility reservations (Main Campus). */
@Service
public class FacilityService {

    /** One line of the facility usage report. */
    public record UsageRow(String facility, int approved, int pending, String hours) {}

    private final FacilityRepository facilities;
    private final ReservationRepository reservations;
    private final AuditService audit;

    public FacilityService(FacilityRepository facilities, ReservationRepository reservations, AuditService audit) {
        this.facilities = facilities;
        this.reservations = reservations;
        this.audit = audit;
    }

    // ------------------------------------------------------------- facilities

    /** Facilities sorted by name. */
    public DynamicArray<Facility> facilities(boolean activeOnly) {
        DynamicArray<Facility> matches = facilities.findWhere(f -> !activeOnly || f.getActive());
        return Sorting.mergeSort(matches, Comparator.comparing(Facility::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public synchronized Facility saveFacility(User admin, Long id, String name, String code,
                                              String location, Integer capacity, String description, boolean active) {
        Facility facility = new Facility();
        if (id != null) {
            facility = facilities.findById(id);
            if (facility == null) throw new IllegalArgumentException("Facility not found");
        }
        name = clean(name);
        code = clean(code).toUpperCase();
        location = clean(location);
        require(!name.isEmpty() && name.length() <= 100, "Facility name is required (up to 100 characters)");
        require(!code.isEmpty() && code.length() <= 30, "Facility code is required (up to 30 characters)");
        require(!location.isEmpty(), "Location is required");
        require(capacity != null && capacity > 0, "Capacity must be a positive number");
        Long currentId = facility.getId();
        String wantedCode = code;
        require(facilities.findWhere(f -> f.getCode().equalsIgnoreCase(wantedCode) && !f.getId().equals(currentId)).isEmpty(),
                "Another facility already uses the code " + code);

        facility.setName(name);
        facility.setCode(code);
        facility.setLocation(location);
        facility.setCapacity(capacity);
        facility.setDescription(clean(description));
        facility.setActive(active);
        facilities.save(facility);
        audit.log(admin, id == null ? "FACILITY_CREATE" : "FACILITY_UPDATE", name);
        return facility;
    }

    public synchronized void deleteFacility(User admin, Long id) {
        Facility facility = facilities.findById(id);
        if (facility == null) throw new IllegalArgumentException("Facility not found");
        LocalDate today = LocalDate.now();
        boolean booked = !reservations.findWhere(r -> r.getFacilityId().equals(id) && r.holdsSlot()
                && !r.getReservationDate().isBefore(today)).isEmpty();
        require(!booked, facility.getName() + " still has upcoming reservations. Mark it inactive instead.");
        facilities.deleteById(id);
        audit.log(admin, "FACILITY_DELETE", facility.getName());
    }

    // ----------------------------------------------------------- reservations

    public synchronized Reservation submit(User user, Long facilityId, LocalDate date, LocalTime start, LocalTime end,
                                           String purpose) {
        Facility facility = facilities.findById(facilityId);
        require(facility != null && facility.getActive(), "Invalid facility selection");
        require(date != null && start != null && end != null, "Date, start time and end time are required");
        require(!date.isBefore(LocalDate.now()), "You cannot reserve a date in the past");
        require(start.isBefore(end), "Start time must be before end time");
        purpose = clean(purpose);
        require(!purpose.isEmpty() && purpose.length() <= 255, "Purpose is required (up to 255 characters)");
        if (hasConflict(facilityId, date, start, end, null)) {
            throw new IllegalStateException("Double-booking blocked: " + facility.getName()
                    + " is already reserved for that date and time.");
        }

        Reservation reservation = new Reservation();
        reservation.setFacilityId(facilityId);
        reservation.setUserId(user.getId());
        reservation.setReservationDate(date);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setPurpose(purpose);
        reservation.setStatus(ReservationStatus.PENDING);
        return reservations.save(reservation);
    }

    /**
     * Double-booking check. The slots that already hold this facility on this date never
     * overlap each other, so after sorting them by start time they are also sorted by end
     * time. Binary search finds the first slot that ends after the requested start; only
     * that slot can collide with the request.
     */
    private boolean hasConflict(Long facilityId, LocalDate date, LocalTime start, LocalTime end, Long ignoreId) {
        DynamicArray<Reservation> slots = Sorting.mergeSort(
                reservations.findWhere(r -> r.getFacilityId().equals(facilityId)
                        && r.getReservationDate().equals(date)
                        && r.holdsSlot()
                        && !r.getId().equals(ignoreId)),
                Comparator.comparing(Reservation::getStartTime));
        int index = Searching.firstIndexWhere(slots, slot -> slot.getEndTime().isAfter(start));
        return index < slots.size() && slots.get(index).getStartTime().isBefore(end);
    }

    public synchronized void cancel(User user, Long reservationId) {
        Reservation reservation = reservations.findById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Reservation not found");
        }
        require(reservation.holdsSlot(), "This reservation is already " + reservation.getStatus());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservations.save(reservation);
    }

    public synchronized void updateStatus(User admin, Long reservationId, ReservationStatus status, String remarks) {
        Reservation reservation = reservations.findById(reservationId);
        if (reservation == null) throw new IllegalArgumentException("Reservation record not found");
        require(status != null, "Select a status");
        boolean willHoldSlot = status == ReservationStatus.PENDING || status == ReservationStatus.APPROVED;
        // Bringing a rejected or cancelled request back must not collide with a newer booking
        if (willHoldSlot && !reservation.holdsSlot() && hasConflict(reservation.getFacilityId(),
                reservation.getReservationDate(), reservation.getStartTime(), reservation.getEndTime(), reservation.getId())) {
            throw new IllegalStateException("Double-booking blocked: that time slot has since been reserved by someone else.");
        }
        reservation.setStatus(status);
        reservation.setAdminRemarks(clean(remarks));
        reservations.save(reservation);
        audit.log(admin, "RESERVATION_" + status, "Reservation " + reservationId + " for "
                + reservation.getReservationDate() + " " + reservation.getStartTime() + "-" + reservation.getEndTime());
    }

    public DynamicArray<Reservation> reservationsOf(Long userId) {
        return Sorting.mergeSort(reservations.findWhere(r -> r.getUserId().equals(userId)),
                Comparator.comparing(Reservation::getReservationDate).thenComparing(Reservation::getStartTime).reversed());
    }

    /** Slots taken on a date (pending or approved), earliest first. */
    public DynamicArray<Reservation> schedule(LocalDate date) {
        DynamicArray<Reservation> taken = reservations.findWhere(r ->
                r.getReservationDate().equals(date) && r.holdsSlot() && facilities.findById(r.getFacilityId()) != null);
        return Sorting.mergeSort(taken, Comparator.comparing(Reservation::getStartTime));
    }

    /** Everything admins can act on: pending first, then the rest by date. */
    public DynamicArray<Reservation> allReservations() {
        return Sorting.mergeSort(reservations.findAll(),
                Comparator.comparing((Reservation r) -> r.getStatus() != ReservationStatus.PENDING)
                        .thenComparing(Reservation::getReservationDate, Comparator.reverseOrder())
                        .thenComparing(Reservation::getStartTime));
    }

    public int pendingCount() {
        return reservations.findWhere(r -> r.getStatus() == ReservationStatus.PENDING).size();
    }

    /** Usage per facility: approved bookings, pending requests and total approved hours. */
    public DynamicArray<UsageRow> usageReport() {
        DynamicArray<UsageRow> report = new DynamicArray<>();
        for (Facility facility : facilities(false)) {
            int approved = 0;
            int pending = 0;
            long minutes = 0;
            for (Reservation r : reservations.findWhere(booking -> booking.getFacilityId().equals(facility.getId()))) {
                if (r.getStatus() == ReservationStatus.APPROVED) {
                    approved++;
                    minutes += Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
                } else if (r.getStatus() == ReservationStatus.PENDING) {
                    pending++;
                }
            }
            report.add(new UsageRow(facility.getName(), approved, pending, String.format("%.1f", minutes / 60.0)));
        }
        return report;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
