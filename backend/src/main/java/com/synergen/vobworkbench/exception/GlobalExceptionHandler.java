package com.synergen.vobworkbench.exception;

import com.synergen.vobworkbench.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        if (ex.getStatus() == HttpStatus.FORBIDDEN || "ACCESS_DENIED".equals(ex.getCode())) {
            auditService.logAccessDenied(request);
        }
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request, details);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiError> handleDuplicate(DuplicateKeyException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_VALUE", "A record with this unique value already exists.", request, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConflict(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "VERSION_CONFLICT", "This record was updated by another user. Reload and try again.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameter.", request,
                List.of(ex.getName() + ": invalid value"));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication failed.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        auditService.logAccessDenied(request);
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not allowed to perform this action.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error.", request, List.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request, List<String> details) {
        ApiError body = new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
