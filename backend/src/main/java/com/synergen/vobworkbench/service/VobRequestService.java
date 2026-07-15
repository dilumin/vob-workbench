package com.synergen.vobworkbench.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.synergen.vobworkbench.dto.CommonDtos.PageInfo;
import com.synergen.vobworkbench.dto.CommonDtos.PageResponse;
import com.synergen.vobworkbench.dto.VobDtos.VobRequestCreate;
import com.synergen.vobworkbench.dto.VobDtos.VobRequestPatch;
import com.synergen.vobworkbench.dto.VobDtos.VobRequestResponse;
import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.BenefitType;
import com.synergen.vobworkbench.model.CoverageSummary;
import com.synergen.vobworkbench.model.EligibilityResult;
import com.synergen.vobworkbench.model.InsuranceOrder;
import com.synergen.vobworkbench.model.InsurancePolicy;
import com.synergen.vobworkbench.model.MockData;
import com.synergen.vobworkbench.model.MockProcedureRule;
import com.synergen.vobworkbench.model.NetworkStatus;
import com.synergen.vobworkbench.model.Procedure;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.VobRequest;
import com.synergen.vobworkbench.model.VobStatus;
import com.synergen.vobworkbench.repository.MockDataRepository;
import com.synergen.vobworkbench.repository.PatientRepository;
import com.synergen.vobworkbench.repository.RefreshTokenRepository;
import com.synergen.vobworkbench.repository.UserRepository;
import com.synergen.vobworkbench.repository.VobRequestRepository;
import com.synergen.vobworkbench.security.SecurityUtils;

@Service
public class VobRequestService {
    private static final Map<VobStatus, List<VobStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(VobStatus.class);
    private static final Set<VobStatus> STARTABLE_STATUSES = Set.of(VobStatus.PENDING, VobStatus.NEEDS_INFO, VobStatus.REOPENED);
    private static final Set<VobStatus> QUEUED_STATUSES = Set.of(VobStatus.PENDING, VobStatus.REOPENED);
    private static final Set<VobStatus> OPEN_WORK_STATUSES = Set.of(
            VobStatus.PENDING,
            VobStatus.IN_PROGRESS,
            VobStatus.NEEDS_INFO,
            VobStatus.SPECIALIST_REVIEW,
            VobStatus.REOPENED
    );

    static {
        ALLOWED_TRANSITIONS.put(VobStatus.PENDING, List.of(VobStatus.IN_PROGRESS, VobStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(VobStatus.IN_PROGRESS, List.of(VobStatus.NEEDS_INFO, VobStatus.SPECIALIST_REVIEW, VobStatus.UNABLE_TO_VERIFY));
        ALLOWED_TRANSITIONS.put(VobStatus.NEEDS_INFO, List.of(VobStatus.IN_PROGRESS, VobStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(VobStatus.SPECIALIST_REVIEW, List.of(VobStatus.VERIFIED, VobStatus.UNABLE_TO_VERIFY, VobStatus.NEEDS_INFO));
        ALLOWED_TRANSITIONS.put(VobStatus.VERIFIED, List.of(VobStatus.REOPENED));
        ALLOWED_TRANSITIONS.put(VobStatus.UNABLE_TO_VERIFY, List.of(VobStatus.REOPENED));
        ALLOWED_TRANSITIONS.put(VobStatus.REOPENED, List.of(VobStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(VobStatus.CANCELLED, List.of());
    }

    private final VobRequestRepository requests;
    private final PatientRepository patients;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final CoverageCalculationService coverageCalculationService;
    private final MockDataRepository mockDataRepository;

    public VobRequestService(VobRequestRepository requests, PatientRepository patients, UserRepository users,
                             RefreshTokenRepository refreshTokens, SecurityUtils securityUtils,
                             AuditService auditService, CoverageCalculationService coverageCalculationService,
                             MockDataRepository mockDataRepository) {
        this.requests = requests;
        this.patients = patients;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.coverageCalculationService = coverageCalculationService;
        this.mockDataRepository = mockDataRepository;
    }

    public PageResponse<VobRequestResponse> search(VobStatus status, String assignedTo, String q, int page, int size) {
        List<VobRequestResponse> filtered = visibleRequests()
                .stream()
                .filter(request -> status == null || request.getStatus() == status)
                .filter(request -> !StringUtils.hasText(assignedTo) || assignedTo.equals(request.getAssignedTo()))
                .filter(request -> !StringUtils.hasText(q) || request.getPatientId().contains(q))
                .map(this::toResponse)
                .toList();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new PageResponse<>(filtered.subList(from, to), new PageInfo(page, size, filtered.size(), totalPages));
    }

    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SPECIALIST', 'SUPERVISOR_ADMIN')")
    public VobRequestResponse create(VobRequestCreate request) {
        if (!patients.existsById(request.patientId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "Patient was not found.");
        }
        validateCreate(request);
        Instant now = Instant.now();
        VobRequest entity = new VobRequest();
        entity.setPatientId(request.patientId());
        entity.setDateOfService(request.dateOfService());
        entity.setPriority(request.priority());
        entity.setStatus(VobStatus.PENDING);
        entity.setAssignedTo(null);
        entity.setCreatedBy(securityUtils.username());
        entity.setInsurancePolicies(request.insurancePolicies());
        entity.setProcedures(request.procedures());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        VobRequest saved = requests.save(entity);
        auditService.record(saved.getId(), "VOB_REQUEST_CREATED", "status", null, VobStatus.PENDING);
        dispatchNextQueuedRequest();
        return toResponse(requests.findById(saved.getId()).orElse(saved));
    }

    public VobRequestResponse get(String id) {
        VobRequest request = find(id);
        requireCanView(request);
        if (shouldStartWorkOnOpen(request)) {
            moveStatus(request, VobStatus.IN_PROGRESS, "VOB_REQUEST_OPENED");
            request.setUpdatedAt(Instant.now());
            request = requests.save(request);
        }
        return toResponse(request);
    }

    public void ensureExists(String id) {
        requireCanView(find(id));
    }

    @PreAuthorize("hasAnyRole('SPECIALIST', 'SUPERVISOR_ADMIN')")
    public VobRequestResponse patch(String id, VobRequestPatch patch) {
        VobRequest request = find(id);
        requireCanWork(request);
        requireFreshVersion(request, patch.version());
        if (request.isLocked()) {
            throw new BusinessException(HttpStatus.CONFLICT, "VERIFIED_REQUEST_LOCKED", "Verified requests are locked.");
        }

        if (patch.dateOfService() != null) {
            if (patch.dateOfService().isBefore(LocalDate.now())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "DATE_OF_SERVICE_IN_PAST", "Date of service cannot be in the past.");
            }
            auditService.record(id, "VOB_REQUEST_UPDATED", "dateOfService", request.getDateOfService(), patch.dateOfService());
            request.setDateOfService(patch.dateOfService());
        }
        if (patch.priority() != null) {
            auditService.record(id, "VOB_REQUEST_UPDATED", "priority", request.getPriority(), patch.priority());
            request.setPriority(patch.priority());
        }
        if (patch.assignedTo() != null) {
            securityUtils.requireAny(Role.SUPERVISOR_ADMIN);
            auditService.record(id, "VOB_REQUEST_ASSIGNED", "assignedTo", request.getAssignedTo(), patch.assignedTo());
            request.setAssignedTo(patch.assignedTo());
        }
        if (patch.insurancePolicies() != null) {
            validateInsurance(patch.insurancePolicies());
            request.setInsurancePolicies(patch.insurancePolicies());
            auditService.record(id, "INSURANCE_POLICY_UPDATED", "insurancePolicies", "previous", "updated");
        }
        if (patch.procedures() != null) {
            validateProcedures(patch.procedures());
            request.setProcedures(patch.procedures());
            auditService.record(id, "PROCEDURE_UPDATED", "procedures", "previous", "updated");
        }
        if (patch.eligibilityResult() != null) {
            securityUtils.requireAny(Role.SPECIALIST, Role.SUPERVISOR_ADMIN);
            request.setEligibilityResult(patch.eligibilityResult());
            moveAfterEligibilityResult(request, patch.eligibilityResult());
            auditService.record(id, "MANUAL_FIELD_CORRECTION", "eligibilityResult", "previous", "updated");
        }
        if (StringUtils.hasText(patch.note())) {
            request.getNotes().add(patch.note());
            auditService.record(id, "NOTE_ADDED", "note", null, patch.note());
        }

        request.setUpdatedAt(Instant.now());
        return toResponse(requests.save(request));
    }

    @PreAuthorize("hasAnyRole('SPECIALIST', 'SUPERVISOR_ADMIN')")
    public VobRequestResponse runEligibility(String id) {
        VobRequest request = find(id);
        securityUtils.requireAny(Role.SPECIALIST, Role.SUPERVISOR_ADMIN);
        requireCanWork(request);
        startWorkIfNeeded(request);
        EligibilityResult eligibility = determineEligibility(request);
        request.setEligibilityResult(eligibility);
        moveAfterEligibilityResult(request, eligibility);
        request.setUpdatedAt(Instant.now());
        auditService.record(id, "ELIGIBILITY_CHECK_COMPLETED", "eligibilityResult", null, request.getEligibilityResult().getNotes());
        return toResponse(requests.save(request));
    }

    @PreAuthorize("hasAnyRole('SPECIALIST', 'SUPERVISOR_ADMIN')")
    public CoverageSummary calculateCoverage(String id) {
        VobRequest request = find(id);
        securityUtils.requireAny(Role.SPECIALIST, Role.SUPERVISOR_ADMIN);
        requireCanWork(request);
        if (request.getEligibilityResult() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "ELIGIBILITY_REQUIRED", "Run eligibility before calculating coverage.");
        }
        CoverageSummary summary = coverageCalculationService.calculate(request);
        request.setCoverageSummary(summary);
        request.setUpdatedAt(Instant.now());
        requests.save(request);
        auditService.record(id, "COVERAGE_CALCULATED", "coverageSummary", null, "calculated");
        return summary;
    }

    @PreAuthorize("hasAnyRole('SPECIALIST', 'SUPERVISOR_ADMIN')")
    public VobRequestResponse verify(String id) {
        VobRequest request = find(id);
        securityUtils.requireAny(Role.SPECIALIST, Role.SUPERVISOR_ADMIN);
        requireCanWork(request);
        moveToReviewIfReadyForVerification(request);
        moveStatus(request, VobStatus.VERIFIED, "VOB_REQUEST_VERIFIED");
        request.setUpdatedAt(Instant.now());
        return toResponse(requests.save(request));
    }

    @PreAuthorize("hasRole('SUPERVISOR_ADMIN')")
    public VobRequestResponse reopen(String id) {
        VobRequest request = find(id);
        securityUtils.requireAny(Role.SUPERVISOR_ADMIN);
        moveStatus(request, VobStatus.REOPENED, "VOB_REQUEST_REOPENED");
        request.setLocked(false);
        request.setUpdatedAt(Instant.now());
        return toResponse(requests.save(request));
    }

    private boolean shouldStartWorkOnOpen(VobRequest request) {
        return securityUtils.hasRole(Role.SPECIALIST)
                && securityUtils.username().equals(request.getAssignedTo())
                && STARTABLE_STATUSES.contains(request.getStatus());
    }

    private void startWorkIfNeeded(VobRequest request) {
        if (STARTABLE_STATUSES.contains(request.getStatus())) {
            moveStatus(request, VobStatus.IN_PROGRESS, "VOB_WORK_STARTED");
        }
    }

    private void moveAfterEligibilityResult(VobRequest request, EligibilityResult eligibility) {
        if (eligibility == null || request.getStatus() == VobStatus.VERIFIED
                || request.getStatus() == VobStatus.UNABLE_TO_VERIFY || request.getStatus() == VobStatus.CANCELLED) {
            return;
        }
        startWorkIfNeeded(request);
        VobStatus target = Boolean.TRUE.equals(eligibility.getCoverageActive())
                ? VobStatus.SPECIALIST_REVIEW
                : VobStatus.NEEDS_INFO;
        moveStatus(request, target, "ELIGIBILITY_STATUS_UPDATED");
    }

    private void moveToReviewIfReadyForVerification(VobRequest request) {
        if (request.getStatus() == VobStatus.IN_PROGRESS
                && request.getEligibilityResult() != null
                && request.getCoverageSummary() != null) {
            moveStatus(request, VobStatus.SPECIALIST_REVIEW, "VOB_READY_FOR_REVIEW");
        }
    }

    private List<VobRequest> visibleRequests() {
        if (securityUtils.hasRole(Role.SPECIALIST) && !securityUtils.hasRole(Role.SUPERVISOR_ADMIN)) {
            return requests.findByAssignedTo(securityUtils.username());
        }
        return requests.findAll();
    }

    private void requireCanView(VobRequest request) {
        if (securityUtils.hasRole(Role.SUPERVISOR_ADMIN)) {
            return;
        }
        if (securityUtils.hasRole(Role.SPECIALIST) && securityUtils.username().equals(request.getAssignedTo())) {
            return;
        }
        if (!securityUtils.hasRole(Role.SPECIALIST)) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "VOB_REQUEST_NOT_ASSIGNED",
                "You can only access VOB requests assigned to you.");
    }

    private void requireCanWork(VobRequest request) {
        if (securityUtils.hasRole(Role.SUPERVISOR_ADMIN)) {
            return;
        }
        if (securityUtils.hasRole(Role.SPECIALIST) && securityUtils.username().equals(request.getAssignedTo())) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "VOB_REQUEST_NOT_ASSIGNED",
                "You can only work VOB requests assigned to you.");
    }

    public void dispatchNextQueuedRequest() {
        List<String> onlineSpecialists = onlineSpecialistUsernames();
        if (onlineSpecialists.isEmpty()) {
            return;
        }

        Optional<VobRequest> nextRequest = requests.findAll().stream()
                .filter(this::isQueuedForAssignment)
                .min(Comparator.comparing(VobRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        nextRequest.ifPresent(request -> {
            String assignee = onlineSpecialists.stream()
                    .min(Comparator.comparingLong(this::openAssignedWorkCount).thenComparing(Comparator.naturalOrder()))
                    .orElseThrow();
            auditService.record(request.getId(), "VOB_REQUEST_ASSIGNED", "assignedTo", request.getAssignedTo(), assignee);
            request.setAssignedTo(assignee);
            request.setUpdatedAt(Instant.now());
            requests.save(request);
        });
    }

    private List<String> onlineSpecialistUsernames() {
        Set<String> onlineUsernames = refreshTokens.findByRevokedFalseAndExpiresAtAfter(Instant.now()).stream()
                .map(token -> token.getUsername())
                .collect(java.util.stream.Collectors.toSet());

        return users.findAll().stream()
                .filter(user -> user.isActive() && user.getRole() == Role.SPECIALIST)
                .map(user -> user.getUsername())
                .filter(onlineUsernames::contains)
                .sorted()
                .toList();
    }

    private boolean isQueuedForAssignment(VobRequest request) {
        return !request.isLocked()
                && !StringUtils.hasText(request.getAssignedTo())
                && QUEUED_STATUSES.contains(request.getStatus());
    }

    private long openAssignedWorkCount(String username) {
        return requests.findAll().stream()
                .filter(request -> username.equals(request.getAssignedTo()))
                .filter(request -> !request.isLocked())
                .filter(request -> OPEN_WORK_STATUSES.contains(request.getStatus()))
                .count();
    }

    private EligibilityResult determineEligibility(VobRequest request) {
        MockData mockData = mockDataRepository.findById("default").orElse(new MockData());
        Optional<InsurancePolicy> primaryPolicy = primaryPolicy(request);

        if (primaryPolicy.isEmpty() || !isPolicyActiveForDate(primaryPolicy.get(), request.getDateOfService())) {
            return eligibility(false, NetworkStatus.UNKNOWN, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, false, "No active primary policy for date of service.");
        }

        InsurancePolicy policy = primaryPolicy.get();
        List<MockProcedureRule> matchedRules = request.getProcedures().stream()
                .map(procedure -> matchingRule(mockData, policy, procedure.getProcedureCode()))
                .flatMap(Optional::stream)
                .toList();

        if (matchedRules.size() != request.getProcedures().size()) {
            return eligibility(false, NetworkStatus.UNKNOWN, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, false, "No configured benefit found for one or more requested procedures.");
        }

        Optional<MockProcedureRule> inactiveRule = matchedRules.stream()
                .filter(rule -> !isRuleActive(rule))
                .findFirst();
        if (inactiveRule.isPresent()) {
            MockProcedureRule rule = inactiveRule.get();
            return eligibility(false, safeNetworkStatus(rule), safe(rule.getCopay()), safePercent(rule.getCoinsurancePercent()),
                    deductibleTotal(rule), deductibleMet(rule), safe(rule.getOopMax()), safe(rule.getOopMet()),
                    priorAuthorizationRequired(request, matchedRules), "Coverage is inactive for one or more requested procedures.");
        }

        MockProcedureRule primaryRule = matchedRules.get(0);
        boolean hasNotCoveredProcedure = matchedRules.stream().anyMatch(rule -> benefitType(rule) == BenefitType.NOT_COVERED);
        return eligibility(true, safeNetworkStatus(primaryRule), firstMoney(matchedRules, MockProcedureRule::getCopay),
                firstPercent(matchedRules, MockProcedureRule::getCoinsurancePercent),
                firstMoney(matchedRules, MockProcedureRule::getDeductibleTotal),
                firstMoney(matchedRules, MockProcedureRule::getDeductibleMet),
                firstMoney(matchedRules, MockProcedureRule::getOopMax),
                firstMoney(matchedRules, MockProcedureRule::getOopMet),
                priorAuthorizationRequired(request, matchedRules),
                hasNotCoveredProcedure
                        ? "Eligibility is active, but one or more requested procedures may not be covered."
                        : "Eligibility matched active mock benefit data for requested procedure codes.");
    }

    private Optional<InsurancePolicy> primaryPolicy(VobRequest request) {
        return request.getInsurancePolicies().stream()
                .filter(policy -> policy.getInsuranceOrder() == InsuranceOrder.PRIMARY)
                .findFirst()
                .or(() -> request.getInsurancePolicies().stream().findFirst());
    }

    private boolean isPolicyActiveForDate(InsurancePolicy policy, LocalDate dateOfService) {
        return policy.isActive()
                && (policy.getCoverageStart() == null || !policy.getCoverageStart().isAfter(dateOfService))
                && (policy.getCoverageEnd() == null || !policy.getCoverageEnd().isBefore(dateOfService));
    }

    private Optional<MockProcedureRule> matchingRule(MockData mockData, InsurancePolicy policy, String procedureCode) {
        return mockData.getProcedureRules().stream()
                .filter(rule -> equalsIgnoreCase(rule.getPayerName(), policy.getPayerName()))
                .filter(rule -> equalsIgnoreCase(rule.getProcedureCode(), procedureCode))
                .filter(rule -> rule.getPlanType() == null || rule.getPlanType() == policy.getPlanType())
                .filter(rule -> !StringUtils.hasText(rule.getMemberId()) || equalsIgnoreCase(rule.getMemberId(), policy.getMemberId()))
                .filter(rule -> !StringUtils.hasText(rule.getGroupNumber()) || equalsIgnoreCase(rule.getGroupNumber(), policy.getGroupNumber()))
                .findFirst();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private NetworkStatus safeNetworkStatus(MockProcedureRule rule) {
        return rule.getNetworkStatus() == null ? NetworkStatus.UNKNOWN : rule.getNetworkStatus();
    }

    private boolean isRuleActive(MockProcedureRule rule) {
        return rule.getCoverageActive() == null || Boolean.TRUE.equals(rule.getCoverageActive());
    }

    private EligibilityResult eligibility(boolean coverageActive, NetworkStatus networkStatus, BigDecimal copay,
                                          int coinsurancePercent, BigDecimal deductibleTotal, BigDecimal deductibleMet,
                                          BigDecimal oopMax, BigDecimal oopMet, boolean priorAuthorizationRequired,
                                          String notes) {
        BigDecimal normalizedDeductibleTotal = safe(deductibleTotal);
        BigDecimal normalizedDeductibleMet = safe(deductibleMet).min(normalizedDeductibleTotal);
        BigDecimal deductibleRemaining = normalizedDeductibleTotal.subtract(normalizedDeductibleMet).max(BigDecimal.ZERO);
        BigDecimal normalizedOopMax = safe(oopMax);
        BigDecimal normalizedOopMet = normalizedOopMax.signum() > 0 ? safe(oopMet).min(normalizedOopMax) : safe(oopMet);
        BigDecimal oopRemaining = normalizedOopMax.signum() > 0
                ? normalizedOopMax.subtract(normalizedOopMet).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        EligibilityResult result = new EligibilityResult();
        result.setCoverageActive(coverageActive);
        result.setNetworkStatus(networkStatus);
        result.setCopay(safe(copay));
        result.setCoinsurancePercent(safePercent(coinsurancePercent));
        result.setDeductibleTotal(normalizedDeductibleTotal);
        result.setDeductibleMet(normalizedDeductibleMet);
        result.setDeductibleRemaining(deductibleRemaining);
        result.setOopMax(normalizedOopMax);
        result.setOopMet(normalizedOopMet);
        result.setOopRemaining(oopRemaining);
        result.setPriorAuthorizationRequired(priorAuthorizationRequired);
        result.setBenefitSource("MOCK_271");
        result.setVerifiedBy(securityUtils.username());
        result.setVerifiedAt(Instant.now());
        result.setNotes(notes);
        return result;
    }

    private boolean priorAuthorizationRequired(VobRequest request, List<MockProcedureRule> matchedRules) {
        return request.getProcedures().stream().anyMatch(Procedure::isRequiresAuthorization)
                || matchedRules.stream().anyMatch(rule -> Boolean.TRUE.equals(rule.getPriorAuthorizationRequired()));
    }

    private BigDecimal deductibleTotal(MockProcedureRule rule) {
        if (rule.getDeductibleTotal() != null) {
            return safe(rule.getDeductibleTotal());
        }
        return safe(rule.getDeductibleRemaining());
    }

    private BigDecimal deductibleMet(MockProcedureRule rule) {
        return safe(rule.getDeductibleMet());
    }

    private BigDecimal firstMoney(List<MockProcedureRule> rules, java.util.function.Function<MockProcedureRule, BigDecimal> extractor) {
        return rules.stream()
                .map(extractor)
                .filter(value -> value != null)
                .findFirst()
                .map(this::safe)
                .orElse(BigDecimal.ZERO);
    }

    private int firstPercent(List<MockProcedureRule> rules, java.util.function.Function<MockProcedureRule, Integer> extractor) {
        return rules.stream()
                .map(extractor)
                .filter(value -> value != null)
                .findFirst()
                .map(this::safePercent)
                .orElse(0);
    }

    private BenefitType benefitType(MockProcedureRule rule) {
        if (rule.getBenefitType() != null) {
            return rule.getBenefitType();
        }
        if (!isRuleActive(rule) || safePercent(rule.getCoveragePercent()) == 0) {
            return BenefitType.NOT_COVERED;
        }
        if (safe(rule.getDeductibleRemaining()).signum() > 0 && safePercent(rule.getCoinsurancePercent()) > 0) {
            return BenefitType.DEDUCTIBLE_THEN_COINSURANCE;
        }
        if (safe(rule.getCopay()).signum() > 0 && safePercent(rule.getCoinsurancePercent()) > 0) {
            return BenefitType.COPAY_THEN_COINSURANCE;
        }
        if (safe(rule.getCopay()).signum() > 0) {
            return BenefitType.COPAY_ONLY;
        }
        return BenefitType.COINSURANCE_ONLY;
    }

    private int safePercent(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private void moveStatus(VobRequest request, VobStatus target, String auditAction) {
        if (request.getStatus() == target) {
            return;
        }
        securityUtils.requireAny(Role.SPECIALIST, Role.SUPERVISOR_ADMIN);
        if (!ALLOWED_TRANSITIONS.getOrDefault(request.getStatus(), List.of()).contains(target)) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", "This status transition is not allowed.");
        }
        if ((target == VobStatus.REOPENED) && !securityUtils.hasRole(Role.SUPERVISOR_ADMIN)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Only supervisor/admin can reopen requests.");
        }
        if (target == VobStatus.VERIFIED && (request.getEligibilityResult() == null || request.getCoverageSummary() == null)) {
            throw new BusinessException(HttpStatus.CONFLICT, "VERIFICATION_INCOMPLETE", "Eligibility and coverage are required before verification.");
        }
        auditService.record(request.getId(), auditAction, "status", request.getStatus(), target);
        request.setStatus(target);
        if (target == VobStatus.VERIFIED) {
            request.setLocked(true);
        }
    }

    private void validateCreate(VobRequestCreate request) {
        if (request.dateOfService().isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DATE_OF_SERVICE_IN_PAST", "Date of service cannot be in the past.");
        }
        validateInsurance(request.insurancePolicies());
        validateProcedures(request.procedures());
    }

    private void validateInsurance(List<InsurancePolicy> policies) {
        long primaryCount = policies.stream().filter(policy -> policy.getInsuranceOrder() == InsuranceOrder.PRIMARY).count();
        if (primaryCount != 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INSURANCE_ORDER", "Exactly one primary insurance policy is required.");
        }
        policies.forEach(policy -> {
            if (!StringUtils.hasText(policy.getPayerName()) || !StringUtils.hasText(policy.getMemberId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INSURANCE_POLICY", "Payer name and member ID are required.");
            }
            if (policy.getCoverageStart() != null && policy.getCoverageEnd() != null
                    && policy.getCoverageEnd().isBefore(policy.getCoverageStart())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_COVERAGE_DATES", "Coverage end must be after coverage start.");
            }
        });
    }

    private void validateProcedures(List<Procedure> procedures) {
        procedures.forEach(procedure -> {
            if (!StringUtils.hasText(procedure.getProcedureCode()) || !StringUtils.hasText(procedure.getProcedureName())
                    || procedure.getEstimatedCharge() == null || procedure.getEstimatedCharge().signum() <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PROCEDURE", "Procedure code, name, and positive charge are required.");
            }
        });
    }

    private void requireFreshVersion(VobRequest request, Long version) {
        if (request.getVersion() != null && !request.getVersion().equals(version)) {
            throw new BusinessException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "This request was updated by another user. Reload and try again.");
        }
    }

    private VobRequest find(String id) {
        return requests.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "VOB_REQUEST_NOT_FOUND", "VOB request was not found."));
    }

    private VobRequestResponse toResponse(VobRequest request) {
        return new VobRequestResponse(request.getId(), request.getPatientId(), request.getDateOfService(), request.getPriority(),
                request.getAssignedTo(), request.getInsurancePolicies(), request.getProcedures(), request.getStatus(),
                request.isLocked(), request.getVersion(), request.getEligibilityResult(), request.getCoverageSummary(),
                request.getNotes(), request.getCreatedAt(), request.getUpdatedAt());
    }
}
