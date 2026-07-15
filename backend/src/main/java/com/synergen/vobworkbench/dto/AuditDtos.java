package com.synergen.vobworkbench.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditEventSearchCriteria(
            String actorUsername,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String patientId,
            String vobRequestId,
            Boolean success,
            String errorCode,
            String httpMethod,
            String path,
            Instant from,
            Instant to
    ) {
    }

    public record AuditEventResponse(
            String id,
            String actorUsername,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String patientId,
            String vobRequestId,
            String correlationId,
            String httpMethod,
            String path,
            Boolean success,
            String errorCode,
            String errorMessage,
            Instant timestamp,
            Map<String, Object> metadata
    ) {
    }

    public record AuditEventPageResponse(
            List<AuditEventResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }
}
