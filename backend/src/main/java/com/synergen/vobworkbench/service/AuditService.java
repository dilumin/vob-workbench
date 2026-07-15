package com.synergen.vobworkbench.service;

import com.synergen.vobworkbench.dto.AuditDtos.AuditEventPageResponse;
import com.synergen.vobworkbench.dto.AuditDtos.AuditEventResponse;
import com.synergen.vobworkbench.dto.AuditDtos.AuditEventSearchCriteria;
import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.AuditEvent;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.repository.AuditEventRepository;
import com.synergen.vobworkbench.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private static final Set<String> SAFE_SORT_FIELDS = Set.of("timestamp", "actorUsername", "action", "entityType", "success");
    private static final Set<String> PHI_FIELDS = Set.of(
            "mrn", "firstName", "lastName", "dateOfBirth", "phone", "address", "memberId", "insuranceCardNumber",
            "note", "insurancePolicies", "eligibilityResult"
    );

    private final AuditEventRepository auditEvents;
    private final SecurityUtils securityUtils;
    private final MongoTemplate mongoTemplate;

    public AuditService(AuditEventRepository auditEvents, SecurityUtils securityUtils, MongoTemplate mongoTemplate) {
        this.auditEvents = auditEvents;
        this.securityUtils = securityUtils;
        this.mongoTemplate = mongoTemplate;
    }

    public void record(String requestId, String eventType, String fieldName, Object oldValue, Object newValue) {
        AuditEvent event = baseEvent(eventType, "VOB_REQUEST", requestId, true);
        event.setRequestId(requestId);
        event.setVobRequestId(requestId);
        event.setEventType(eventType);
        event.setFieldName(fieldName);
        event.setOldValue(mask(fieldName, oldValue));
        event.setNewValue(mask(fieldName, newValue));
        auditEvents.save(event);
    }

    public List<AuditEvent> forRequest(String requestId) {
        return auditEvents.findByRequestIdOrderByTimestampDesc(requestId);
    }

    public void logPatientCreated(String patientId) {
        AuditEvent event = baseEvent("PATIENT_CREATED", "PATIENT", patientId, true);
        event.setPatientId(patientId);
        auditEvents.save(event);
    }

    public void logPatientUpdated(String patientId, List<String> changedFields) {
        AuditEvent event = baseEvent("PATIENT_UPDATED", "PATIENT", patientId, true);
        event.setPatientId(patientId);
        event.setMetadata(Map.of("changedFields", changedFields));
        auditEvents.save(event);
    }

    public void logPatientViewed(String patientId) {
        AuditEvent event = baseEvent("PATIENT_VIEWED", "PATIENT", patientId, true);
        event.setPatientId(patientId);
        auditEvents.save(event);
    }

    public void logPatientSearched(int page, int size, boolean filtersUsed) {
        AuditEvent event = baseEvent("PATIENT_SEARCHED", "PATIENT", null, true);
        event.setMetadata(Map.of("page", page, "size", size, "filtersUsed", filtersUsed));
        auditEvents.save(event);
    }

    public void logLoginSucceeded(User user) {
        AuditEvent event = baseEvent("USER_LOGGED_IN", "USER", user.getId(), true, user.getUsername(), user.getRole().name());
        auditEvents.save(event);
    }

    public void logLoginFailed(String username) {
        AuditEvent event = baseEvent("USER_LOGIN_FAILED", "USER", null, false, username, "UNKNOWN");
        event.setErrorCode("AUTHENTICATION_FAILED");
        event.setErrorMessage("Invalid username or password.");
        auditEvents.save(event);
    }

    public void logTokenRefreshed(User user) {
        AuditEvent event = baseEvent("TOKEN_REFRESHED", "USER", user.getId(), true, user.getUsername(), user.getRole().name());
        auditEvents.save(event);
    }

    public void logLogoutSucceeded() {
        auditEvents.save(baseEvent("USER_LOGGED_OUT", "USER", null, true));
    }

    public void logUserRegistered(User user) {
        AuditEvent event = baseEvent("USER_REGISTERED", "USER", user.getId(), true);
        event.setMetadata(Map.of("targetUsername", user.getUsername(), "targetRole", user.getRole().name()));
        auditEvents.save(event);
    }

    public void logMockDataUpdated() {
        auditEvents.save(baseEvent("MOCK_DATA_UPDATED", "MOCK_DATA", "default", true));
    }

    public void logAccessDenied(HttpServletRequest request) {
        AuditEvent event = baseEvent("ACCESS_DENIED", "SYSTEM", null, false);
        event.setHttpMethod(request.getMethod());
        event.setPath(request.getRequestURI());
        event.setErrorCode("ACCESS_DENIED");
        event.setErrorMessage("Access denied.");
        event.setCorrelationId(correlationId(request));
        auditEvents.save(event);
    }

    public AuditEventPageResponse searchAuditEvents(
            AuditEventSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        validatePagingAndSort(page, size, sortBy, sortDirection);

        Query countQuery = buildQuery(criteria);
        long totalElements = mongoTemplate.count(countQuery, AuditEvent.class);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Query pageQuery = buildQuery(criteria)
                .with(Sort.by(direction, sortBy))
                .skip((long) page * size)
                .limit(size);
        List<AuditEventResponse> content = mongoTemplate.find(pageQuery, AuditEvent.class).stream()
                .map(this::toResponse)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditEventPageResponse(content, page, size, totalElements, totalPages,
                page == 0, totalPages == 0 || page >= totalPages - 1);
    }

    private AuditEvent baseEvent(String action, String entityType, String entityId, boolean success) {
        return baseEvent(action, entityType, entityId, success, securityUtils.username(), securityUtils.role());
    }

    private AuditEvent baseEvent(String action, String entityType, String entityId, boolean success,
                                 String actorUsername, String actorRole) {
        AuditEvent event = new AuditEvent();
        event.setActorUsername(actorUsername);
        event.setActorRole(actorRole);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setSuccess(success);
        event.setTimestamp(Instant.now());
        event.setMetadata(new LinkedHashMap<>());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            event.setHttpMethod(request.getMethod());
            event.setPath(request.getRequestURI());
            event.setCorrelationId(correlationId(request));
        }
        return event;
    }

    private Query buildQuery(AuditEventSearchCriteria criteria) {
        Query query = new Query();
        addIfPresent(query, "actorUsername", criteria.actorUsername());
        addIfPresent(query, "actorRole", criteria.actorRole());
        addIfPresent(query, "action", criteria.action());
        addIfPresent(query, "entityType", criteria.entityType());
        addIfPresent(query, "entityId", criteria.entityId());
        addIfPresent(query, "patientId", criteria.patientId());
        addIfPresent(query, "vobRequestId", criteria.vobRequestId());
        addIfPresent(query, "success", criteria.success());
        addIfPresent(query, "errorCode", criteria.errorCode());
        addIfPresent(query, "httpMethod", criteria.httpMethod());
        addIfPresent(query, "path", criteria.path());

        if (criteria.from() != null || criteria.to() != null) {
            Criteria timestamp = Criteria.where("timestamp");
            if (criteria.from() != null) {
                timestamp = timestamp.gte(criteria.from());
            }
            if (criteria.to() != null) {
                timestamp = timestamp.lte(criteria.to());
            }
            query.addCriteria(timestamp);
        }
        return query;
    }

    private void addIfPresent(Query query, String field, Object value) {
        if (value != null) {
            query.addCriteria(Criteria.where(field).is(value));
        }
    }

    private void validatePagingAndSort(int page, int size, String sortBy, String sortDirection) {
        if (page < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "page must be greater than or equal to 0.");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "size must be between 1 and 100.");
        }
        if (!SAFE_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "sortBy is not allowed.");
        }
        if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "sortDirection must be asc or desc.");
        }
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getActorUsername(), event.getActorRole(), event.getAction(),
                event.getEntityType(), event.getEntityId(), event.getPatientId(), event.getVobRequestId(),
                event.getCorrelationId(), event.getHttpMethod(), event.getPath(), event.getSuccess(), event.getErrorCode(),
                event.getErrorMessage(), event.getTimestamp(), event.getMetadata());
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String correlationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader("X-Request-Id");
        }
        return correlationId;
    }

    private String mask(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        if (fieldName != null && isPhiField(fieldName)) {
            return "MASKED";
        }
        return String.valueOf(value);
    }

    private boolean isPhiField(String fieldName) {
        String normalized = fieldName.toLowerCase();
        return PHI_FIELDS.stream().anyMatch(field -> normalized.contains(field.toLowerCase()));
    }
}
