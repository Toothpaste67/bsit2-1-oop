package com.liceo.campustrack.controller;

import com.liceo.campustrack.config.CustomSuccessHandler;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal CustomUserDetails me) {
        return me == null ? "login" : "redirect:/dashboard";
    }

    /** "/" and "/dashboard" send each role to its own dashboard. */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails me) {
        return "redirect:" + CustomSuccessHandler.homeFor(me.getUser().getRole());
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam String idNumber,
                               @RequestParam String password,
                               RedirectAttributes redirect) {
        try {
            userService.register(fullName, email, idNumber, password);
            redirect.addFlashAttribute("success", "Account created successfully. Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("fullName", fullName);
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("idNumber", idNumber);
            return "redirect:/register";
        }
    }
}
