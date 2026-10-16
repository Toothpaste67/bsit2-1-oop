package com.liceo.campustrack.controller;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.AttendanceService;
import com.liceo.campustrack.service.AttendanceService.SubjectStat;
import com.liceo.campustrack.service.FacilityService;
import com.liceo.campustrack.service.QrCodeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final AttendanceService attendanceService;
    private final FacilityService facilityService;
    private final QrCodeService qrCodeService;

    public StudentController(AttendanceService attendanceService, FacilityService facilityService,
                             QrCodeService qrCodeService) {
        this.attendanceService = attendanceService;
        this.facilityService = facilityService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails me, Model model) {
        User student = me.getUser();
        DynamicArray<SubjectStat> stats = attendanceService.subjectStats(student.getId());
        int attended = 0;
        int total = 0;
        for (SubjectStat stat : stats) {
            attended += stat.present() + stat.late();
            total += stat.total();
        }

        // Personal institutional QR: identifies the student to faculty (ID number only, no private data)
        String qrPayload = "LICEO-ID:" + student.getIdNumber();

        model.addAttribute("user", student);
        model.addAttribute("stats", stats);
        model.addAttribute("sessionsTotal", total);
        model.addAttribute("sessionsAttended", attended);
        model.addAttribute("overallPercent", total == 0 ? 0 : Math.round(attended * 100f / total));
        model.addAttribute("history", attendanceService.historyOf(student.getId()).limit(8));
        model.addAttribute("qrBase64", qrCodeService.toPngBase64(qrPayload, 220));
        model.addAttribute("qrPayload", qrPayload);
        model.addAttribute("facilities", facilityService.facilities(true));
        return "student/student-dashboard";
    }
}
