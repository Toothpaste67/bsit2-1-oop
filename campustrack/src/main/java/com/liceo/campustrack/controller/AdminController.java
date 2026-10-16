package com.liceo.campustrack.controller;

import com.liceo.campustrack.model.AttendanceStatus;
import com.liceo.campustrack.model.ItemStatus;
import com.liceo.campustrack.model.ReservationStatus;
import com.liceo.campustrack.model.Role;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.AttendanceService;
import com.liceo.campustrack.service.AuditService;
import com.liceo.campustrack.service.FacilityService;
import com.liceo.campustrack.service.LostFoundService;
import com.liceo.campustrack.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final FacilityService facilityService;
    private final LostFoundService lostFoundService;
    private final AttendanceService attendanceService;
    private final AuditService auditService;

    public AdminController(UserService userService, FacilityService facilityService, LostFoundService lostFoundService,
                           AttendanceService attendanceService, AuditService auditService) {
        this.userService = userService;
        this.facilityService = facilityService;
        this.lostFoundService = lostFoundService;
        this.attendanceService = attendanceService;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- dashboard

    @GetMapping("/dashboard")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetails me, Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("userCount", userService.count());
        model.addAttribute("studentCount", userService.count(Role.STUDENT));
        model.addAttribute("activeSessions", attendanceService.activeSessionCount());
        model.addAttribute("attendanceCount", attendanceService.recordCount());
        model.addAttribute("pendingCount", facilityService.pendingCount());
        model.addAttribute("openItemCount", lostFoundService.openCount());
        model.addAttribute("reservations", facilityService.allReservations().limit(30));
        model.addAttribute("lostFoundItems", lostFoundService.search(null, null).limit(30));
        model.addAttribute("itemStatuses", ItemStatus.values());
        model.addAttribute("recentAudit", auditService.newest(8));
        return "admin/admin-dashboard";
    }

    @PostMapping("/reservations/update")
    public String updateReservationStatus(@AuthenticationPrincipal CustomUserDetails me,
                                          @RequestParam("reservationId") Long reservationId,
                                          @RequestParam("status") ReservationStatus status,
                                          @RequestParam(value = "adminRemarks", required = false) String adminRemarks,
                                          RedirectAttributes redirect) {
        try {
            facilityService.updateStatus(me.getUser(), reservationId, status, adminRemarks);
            redirect.addFlashAttribute("success", "Reservation status updated to " + status);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/lost-found/status")
    public String updateItemStatus(@AuthenticationPrincipal CustomUserDetails me,
                                   @RequestParam("itemId") Long itemId,
                                   @RequestParam("status") ItemStatus status,
                                   RedirectAttributes redirect) {
        try {
            lostFoundService.updateStatus(me.getUser(), itemId, status);
            redirect.addFlashAttribute("success", "Item lifecycle updated to " + status);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // -------------------------------------------------------------------- users

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal CustomUserDetails me,
                        @RequestParam(required = false) Role role,
                        @RequestParam(required = false) String q,
                        Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("users", userService.search(role, q));
        model.addAttribute("selectedRole", role);
        model.addAttribute("q", q);
        return "admin/users";
    }

    @PostMapping("/users/save")
    public String saveUser(@AuthenticationPrincipal CustomUserDetails me,
                           @RequestParam(required = false) Long id,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String idNumber,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) Role role,
                           RedirectAttributes redirect) {
        try {
            userService.saveByAdmin(me.getUser(), id, fullName, email, idNumber, password, role);
            redirect.addFlashAttribute("success", id == null ? "User account created." : "User account updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id,
                             RedirectAttributes redirect) {
        try {
            userService.delete(me.getUser(), id);
            redirect.addFlashAttribute("success", "User account deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // --------------------------------------------------------------- facilities

    @GetMapping("/facilities")
    public String facilities(@AuthenticationPrincipal CustomUserDetails me,
                             Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("facilities", facilityService.facilities(false));
        model.addAttribute("usage", facilityService.usageReport());
        return "admin/facilities";
    }

    @PostMapping("/facilities/save")
    public String saveFacility(@AuthenticationPrincipal CustomUserDetails me,
                               @RequestParam(required = false) Long id,
                               @RequestParam String name,
                               @RequestParam String code,
                               @RequestParam String location,
                               @RequestParam(required = false) Integer capacity,
                               @RequestParam(required = false) String description,
                               @RequestParam(defaultValue = "false") boolean active,
                               RedirectAttributes redirect) {
        try {
            facilityService.saveFacility(me.getUser(), id, name, code, location, capacity, description, active);
            redirect.addFlashAttribute("success", "Facility saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/facilities";
    }

    @PostMapping("/facilities/{id}/delete")
    public String deleteFacility(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id,
                                 RedirectAttributes redirect) {
        try {
            facilityService.deleteFacility(me.getUser(), id);
            redirect.addFlashAttribute("success", "Facility deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/facilities";
    }

    // --------------------------------------------------- master attendance logs

    @GetMapping("/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails me,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) Long classId,
                             @RequestParam(required = false) Role role,
                             Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("records", attendanceService.filter(date, classId, role).limit(500));
        model.addAttribute("classes", attendanceService.allClasses());
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("selectedRole", role);
        return "admin/attendance";
    }

    @PostMapping("/attendance/{id}/edit")
    public String editAttendance(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id,
                                 @RequestParam AttendanceStatus status,
                                 @RequestParam(required = false) String note,
                                 RedirectAttributes redirect) {
        try {
            attendanceService.edit(me.getUser(), id, status, note);
            redirect.addFlashAttribute("success", "Attendance record updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/attendance";
    }

    @GetMapping("/attendance/export")
    public ResponseEntity<byte[]> exportAttendance(
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                   @RequestParam(required = false) Long classId,
                                                   @RequestParam(required = false) Role role) {
        return FacultyController.csv(attendanceService.toCsv(attendanceService.filter(date, classId, role)),
                "Liceo_Attendance_Report.csv");
    }

    // --------------------------------------------------------------- audit logs

    @GetMapping("/audit")
    public String audit(@AuthenticationPrincipal CustomUserDetails me, Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("entries", auditService.newest(300));
        return "admin/audit";
    }
}
