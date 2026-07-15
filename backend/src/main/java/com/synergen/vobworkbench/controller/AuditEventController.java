package com.synergen.vobworkbench.controller;

import com.synergen.vobworkbench.dto.AuditDtos.AuditEventPageResponse;
import com.synergen.vobworkbench.dto.AuditDtos.AuditEventSearchCriteria;
import com.synergen.vobworkbench.service.AuditService;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {
    private final AuditService auditService;

    public AuditEventController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERVISOR_ADMIN')")
    public AuditEventPageResponse search(
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String vobRequestId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(actorUsername, actorRole, action, entityType,
                entityId, patientId, vobRequestId, success, errorCode, httpMethod, path, from, to);
        return auditService.searchAuditEvents(criteria, page, size, sortBy, sortDirection);
    }
}
