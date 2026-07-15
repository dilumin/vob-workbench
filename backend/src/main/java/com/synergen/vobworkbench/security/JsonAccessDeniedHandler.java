package com.synergen.vobworkbench.security;

import com.synergen.vobworkbench.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final AuditService auditService;

    public JsonAccessDeniedHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        auditService.logAccessDenied(request);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"timestamp":"%s","status":403,"code":"ACCESS_DENIED","message":"You are not allowed to perform this action.","path":"%s","details":[]}
                """.formatted(Instant.now(), request.getRequestURI()).trim());
    }
}
