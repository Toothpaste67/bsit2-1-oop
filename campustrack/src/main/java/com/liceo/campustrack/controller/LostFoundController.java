package com.liceo.campustrack.controller;

import com.liceo.campustrack.model.ItemType;
import com.liceo.campustrack.model.LostFoundItem;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.LostFoundService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/lost-found")
public class LostFoundController {

    private final LostFoundService lostFoundService;

    public LostFoundController(LostFoundService lostFoundService) {
        this.lostFoundService = lostFoundService;
    }

    @GetMapping
    public String lostFoundView(@RequestParam(value = "category", required = false) String category,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @AuthenticationPrincipal CustomUserDetails me,
                                Model model) {
        model.addAttribute("user", me.getUser());
        model.addAttribute("items", lostFoundService.search(category, keyword));
        model.addAttribute("categories", LostFoundService.CATEGORIES);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("today", LocalDate.now());
        return "lost-found";
    }

    @PostMapping("/report")
    public String reportItem(@AuthenticationPrincipal CustomUserDetails me,
                             @RequestParam("title") String title,
                             @RequestParam("description") String description,
                             @RequestParam("category") String category,
                             @RequestParam(value = "itemType", required = false) ItemType itemType,
                             @RequestParam("location") String location,
                             @RequestParam(value = "itemDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate itemDate,
                             @RequestParam("verificationQuestion") String verificationQuestion,
                             @RequestParam("verificationAnswer") String verificationAnswer,
                             @RequestParam(value = "image", required = false) MultipartFile image,
                             RedirectAttributes redirect) {
        try {
            lostFoundService.report(me.getUser(), title, description, category, itemType, location, itemDate,
                    verificationQuestion, verificationAnswer, image);
            redirect.addFlashAttribute("success", "Lost & Found entry recorded successfully!");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", "Submission failed: " + e.getMessage());
        }
        return "redirect:/lost-found";
    }

    @PostMapping("/claim")
    public String claimItem(@AuthenticationPrincipal CustomUserDetails me,
                            @RequestParam("itemId") Long itemId,
                            @RequestParam("verificationAnswer") String verificationAnswer,
                            RedirectAttributes redirect) {
        try {
            if (lostFoundService.verifyAndClaim(itemId, verificationAnswer, me.getUser())) {
                redirect.addFlashAttribute("success", "Verification successful! Item status transitioned to CLAIMED.");
            } else {
                redirect.addFlashAttribute("error", "Verification failed: The answer provided does not match our records.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lost-found";
    }

    /** Photos are stored as files in data/photos and served from here. */
    @GetMapping("/photo/{itemId}")
    public ResponseEntity<byte[]> photo(@PathVariable Long itemId) throws IOException {
        LostFoundItem item = lostFoundService.findById(itemId);
        Path path = item == null ? null : lostFoundService.photoPath(item);
        if (path == null || !Files.isRegularFile(path)) return ResponseEntity.notFound().build();

        String name = path.getFileName().toString();
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG
                : name.endsWith(".gif") ? MediaType.IMAGE_GIF
                : name.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(Files.readAllBytes(path));
    }
}
