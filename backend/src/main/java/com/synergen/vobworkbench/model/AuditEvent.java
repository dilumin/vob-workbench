package com.synergen.vobworkbench.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "auditEvents")
public class AuditEvent {
    @Id
    private String id;

    @Indexed
    private String requestId;

    private String eventType;
    private String fieldName;
    private String oldValue;
    private String newValue;

    @Indexed
    private String actorUsername;

    @Indexed
    private String actorRole;

    @Indexed
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    @Indexed
    private String patientId;

    @Indexed
    private String vobRequestId;

    @Indexed
    private String correlationId;

    private String httpMethod;
    private String path;

    @Indexed
    private Boolean success;

    @Indexed
    private String errorCode;

    private String errorMessage;

    @Indexed
    private Instant timestamp;

    private Map<String, Object> metadata = new LinkedHashMap<>();
}
