package com.liceo.campustrack.repository;

import com.liceo.campustrack.model.Reservation;
import com.liceo.campustrack.model.ReservationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationRepository extends TextFileRepository<Reservation> {

    public ReservationRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "reservations.txt",
                "id|facilityId|userId|date|startTime|endTime|purpose|status|adminRemarks|createdAt");
    }

    @Override protected Long getId(Reservation r) { return r.getId(); }
    @Override protected void setId(Reservation r, Long id) { r.setId(id); }

    @Override
    protected String[] toFields(Reservation r) {
        return new String[] {
                text(r.getId()), text(r.getFacilityId()), text(r.getUserId()), text(r.getReservationDate()),
                text(r.getStartTime()), text(r.getEndTime()), text(r.getPurpose()), text(r.getStatus()),
                text(r.getAdminRemarks()), text(r.getCreatedAt())
        };
    }

    @Override
    protected Reservation fromFields(String[] f) {
        Reservation r = new Reservation();
        r.setId(number(f, 0));
        r.setFacilityId(number(f, 1));
        r.setUserId(number(f, 2));
        r.setReservationDate(date(f, 3));
        r.setStartTime(time(f, 4));
        r.setEndTime(time(f, 5));
        r.setPurpose(field(f, 6));
        r.setStatus(constant(ReservationStatus.class, f, 7));
        r.setAdminRemarks(field(f, 8));
        r.setCreatedAt(dateTime(f, 9));
        return r;
    }
}
