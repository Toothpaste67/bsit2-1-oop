package com.liceo.campustrack.controller;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.dto.ApiResponse;
import com.liceo.campustrack.dto.CameraScanRequest;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.AttendanceService;
import com.liceo.campustrack.service.AttendanceService.CheckInResult;
import com.liceo.campustrack.service.QrCodeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Student check-in: camera scanner, manual code, QR image upload and offline sync. STUDENT role only. */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final QrCodeService qrCodeService;

    public AttendanceController(AttendanceService attendanceService, QrCodeService qrCodeService) {
        this.attendanceService = attendanceService;
        this.qrCodeService = qrCodeService;
    }

    // --- 1. Scanner page with the fallbacks and personal history ---
    @GetMapping
    public String attendanceView(@AuthenticationPrincipal CustomUserDetails me, Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("records", attendanceService.historyOf(me.getUser().getId()).limit(30));
        return "attendance";
    }

    // --- 2. Live camera scan (fetch) ---
    @PostMapping("/api/camera-scan")
    @ResponseBody
    public ApiResponse processLiveCameraScan(@AuthenticationPrincipal CustomUserDetails me,
                                             @RequestBody CameraScanRequest request) {
        try {
            CheckInResult result = attendanceService.checkIn(me.getUser(), request.qrPayload(), "CAMERA_SCAN", false);
            return ApiResponse.ok("Attendance recorded: " + result.status(), result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    // --- 3. Offline queue sync (fetch) ---
    @PostMapping("/api/sync")
    @ResponseBody
    public ApiResponse syncOfflineRecords(@AuthenticationPrincipal CustomUserDetails me,
                                          @RequestBody List<CameraScanRequest> queued) {
        DynamicArray<String> payloads = new DynamicArray<>();
        for (CameraScanRequest item : queued) payloads.add(item.qrPayload());

        var results = attendanceService.syncOffline(me.getUser(), payloads);
        int synced = results.filter(AttendanceService.SyncResult::success).size();
        return ApiResponse.ok(synced + " of " + results.size() + " queued scan(s) recorded.", results.toList());
    }

    // --- 4. Fallbacks: typed code or uploaded QR picture (regular form post) ---
    @PostMapping("/scan")
    public String processScan(@AuthenticationPrincipal CustomUserDetails me,
                              @RequestParam(value = "qrCode", required = false) String qrCode,
                              @RequestParam(value = "qrFile", required = false) MultipartFile qrFile,
                              RedirectAttributes redirect) {
        User student = me.getUser();
        try {
            String code = qrCode;
            String method = "MANUAL_CODE";
            if (qrFile != null && !qrFile.isEmpty()) {
                code = qrCodeService.decodeImage(qrFile.getInputStream());
                method = "IMAGE_UPLOAD";
                if (code == null) throw new IllegalArgumentException("No QR code could be read from that image.");
            }
            CheckInResult result = attendanceService.checkIn(student, code, method, false);
            redirect.addFlashAttribute("checkIn", result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (java.io.IOException e) {
            redirect.addFlashAttribute("error", "The uploaded file could not be read.");
        }
        return "redirect:/attendance";
    }
}
