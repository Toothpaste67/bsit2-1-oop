package com.liceo.campustrack.controller;

import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.FacilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

/** Facility browsing and reservations, open to every signed-in role. */
@Controller
@RequestMapping("/reservations")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public String reservationView(
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @AuthenticationPrincipal CustomUserDetails me,
                                  Model model) {
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("user", me.getUser());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("facilities", facilityService.facilities(true));
        model.addAttribute("dailySchedule", facilityService.schedule(selectedDate));
        model.addAttribute("myReservations", facilityService.reservationsOf(me.getUser().getId()));
        return "reservations";
    }

    @PostMapping("/book")
    public String bookFacility(@AuthenticationPrincipal CustomUserDetails me,
                               @RequestParam(required = false) Long facilityId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reservationDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                               @RequestParam(required = false) String purpose,
                               RedirectAttributes redirect) {
        try {
            facilityService.submit(me.getUser(), facilityId, reservationDate, startTime, endTime, purpose);
            redirect.addFlashAttribute("success", "Facility booking request submitted for administrative approval.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        if (reservationDate != null) redirect.addAttribute("date", reservationDate.toString());
        return "redirect:/reservations";
    }

    @PostMapping("/{reservationId}/cancel")
    public String cancel(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long reservationId,
                         RedirectAttributes redirect) {
        try {
            facilityService.cancel(me.getUser(), reservationId);
            redirect.addFlashAttribute("success", "Reservation cancelled.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservations";
    }
}
