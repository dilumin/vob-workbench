package com.synergen.vobworkbench.security;

import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    public String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }

    public String role() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .findFirst()
                .orElse("SYSTEM");
    }

    public boolean hasRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    public void requireAny(Role... roles) {
        for (Role role : roles) {
            if (hasRole(role)) {
                return;
            }
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not allowed to perform this action.");
    }
}
