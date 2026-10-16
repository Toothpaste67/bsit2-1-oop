package com.liceo.campustrack.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/** Shows our own 403 / 404 / 500 pages instead of the default error screen. */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status instanceof Integer value ? value : 500;
        model.addAttribute("code", code);
        return switch (code) {
            case 403 -> "error/403";
            case 404 -> "error/404";
            default -> "error/500";
        };
    }
}
