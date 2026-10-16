package com.liceo.campustrack.controller;

import com.liceo.campustrack.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Values every page needs for its dropdowns, plus a fallback for "record not found". */
@ControllerAdvice
public class GlobalModel {

    @ModelAttribute("roles")
    public Role[] roles() {
        return Role.values();
    }

    /** A lookup that a controller did not handle itself (e.g. someone else's session id in the URL). */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(IllegalArgumentException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error/404";
    }
}
