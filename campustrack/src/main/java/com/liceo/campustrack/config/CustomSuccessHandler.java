package com.liceo.campustrack.config;

import com.liceo.campustrack.model.Role;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** After login, sends each role to its own dashboard. */
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditService audit;

    public CustomSuccessHandler(AuditService audit) {
        this.audit = audit;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        audit.log(user, "LOGIN", user.getRole().getLabel() + " signed in");
        response.sendRedirect(request.getContextPath() + homeFor(user.getRole()));
    }

    public static String homeFor(Role role) {
        return switch (role) {
            case ADMIN -> "/admin/dashboard";
            case FACULTY -> "/faculty/dashboard";
            case STUDENT -> "/student/dashboard";
        };
    }
}
