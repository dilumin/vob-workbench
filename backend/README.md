# VOB Workbench Backend README / Implementation Guide

## 1. Project Overview

**VOB Workbench** is a professional Spring Boot backend for managing the **Verification of Benefits (VOB)** process in a healthcare-style operations workflow.

The system supports:

- Receptionist / Front Office intake
- Patient creation and lookup
- VOB request creation
- Specialist worklist processing
- Mock eligibility and coverage verification
- Manual correction of incomplete coverage data
- Procedure-level insurance coverage calculation
- Primary, secondary, and tertiary insurance handling
- Final patient responsibility estimation
- Full PHI-safe audit trail
- Role-based access control
- Optimistic locking with `@Version`
- Standardized API errors
- Swagger/OpenAPI documentation
- Dockerized MongoDB setup
- Unit/integration test-ready architecture

This backend is designed for a coding assessment using:

- Java 21
- Spring Boot 3.x
- Spring Data MongoDB
- Spring Security
- Maven
- MongoDB
- Docker Compose
- Swagger/OpenAPI

The assessment requires a clean full-stack vertical slice with Spring Boot, MongoDB, Angular, workflow rules, validation, auth/roles, audit trail, optimistic locking, Docker, CI, Swagger, and documentation.

---

## 2. Domain Background

### 2.1 What is Verification of Benefits?

Verification of Benefits (VOB) is the process of checking a patient's insurance coverage before the date of service. It helps the healthcare provider understand:

- Whether the patient's insurance is active
- Whether the provider is in-network or out-of-network
- Whether the planned procedures are covered
- Whether copay, deductible, coinsurance, or out-of-pocket limits apply
- How much the insurance may pay
- How much the patient may need to pay

The VOB result is an **estimate**, not a payment guarantee. Final payment may still depend on payer adjudication, medical necessity, authorization, exclusions, or claim rules.

### 2.2 Key Domain Terms

#### Copay

A fixed amount the patient pays for a service.

Example:

```text
Consultation copay = 500
Patient pays 500 regardless of total charge
```

#### Coinsurance

A percentage of the allowed cost that the patient pays.

Example:

```text
X-Ray charge = 10,000
Insurance covers 80%
Patient coinsurance = 20%
Patient pays 2,000
```

#### Deductible

The amount the patient must pay before insurance starts covering certain services.

Example:

```text
Deductible total = 20,000
Deductible met = 15,000
Remaining deductible = 5,000
```

#### Out-of-Pocket Maximum

The maximum amount the patient pays in a plan year before insurance covers eligible costs fully.

#### In-Network vs Out-of-Network

- **In-network**: Provider is contracted with the payer. Better coverage.
- **Out-of-network**: Provider is not contracted. Lower coverage or no coverage.

#### Subscriber vs Dependent

- **Subscriber**: Main insurance holder.
- **Dependent**: Person covered under subscriber's plan, such as spouse or child.

#### Member ID and Group Number

- **Member ID**: Unique ID for the patient's insurance coverage.
- **Group Number**: Employer/group plan identifier.

---

## 3. Simplified Business Workflow

### 3.1 Main Flow

```text
Receptionist creates patient
        ↓
Receptionist creates VOB request
        ↓
Request starts as PENDING
        ↓
Specialist opens assigned request
        ↓
Specialist starts processing
        ↓
Status becomes IN_PROGRESS
        ↓
System validates request data
        ↓
Specialist runs mock eligibility / coverage check
        ↓
System reads mock master coverage data
        ↓
System auto-fills coverage result
        ↓
Specialist reviews and corrects missing data if needed
        ↓
System calculates coverage and patient responsibility
        ↓
Specialist finalizes request
        ↓
Status becomes VERIFIED or UNABLE_TO_VERIFY
        ↓
Final result is visible to receptionist, specialist, supervisor/admin, and viewer
        ↓
Every meaningful action is saved in audit trail
```

### 3.2 Final Roles

Use the following roles:

```java
public enum Role {
    RECEPTIONIST,
    SPECIALIST,
    SUPERVISOR_ADMIN,
    VIEWER
}
```

#### RECEPTIONIST / FO

Can:

- Create patient
- Search patient
- View patient
- Create VOB request
- Add insurance details
- Add planned procedures
- View final result
- View audit trail

Cannot:

- Finalize verification
- Override verified requests
- Manage users or master data

#### SPECIALIST

Can:

- View assigned worklist
- Open VOB request
- Start processing
- Run mock eligibility check
- Review auto-filled coverage data
- Correct missing/incomplete data
- Calculate coverage
- Mark request as `VERIFIED`
- Mark request as `UNABLE_TO_VERIFY`
- Add notes
- View audit trail

#### SUPERVISOR_ADMIN

Can:

- View all requests
- Assign/reassign specialists
- Reopen verified requests
- View dashboard
- View audit trail
- Manage users
- Manage mock payer master data
- Manage procedure catalogue

#### VIEWER

Can:

- View patients
- View VOB requests
- View final results
- View dashboard
- View audit trail

Cannot:

- Create/update/delete anything

#### SYSTEM

System is not a login user. It represents automated actions:

- Validate data
- Run mock verification
- Calculate coverage
- Detect missing fields
- Create audit events
- Return standardized errors
- Detect optimistic locking conflicts

---

## 4. Backend Architecture

Use a clean layered architecture:

```text
Controller Layer
    ↓
Service Layer
    ↓
Domain / Validation / Calculation Services
    ↓
Repository Layer
    ↓
MongoDB
```

### 4.1 Package Structure

Recommended structure:

```text
src/main/java/com/synergen/vobworkbench
│
├── VobWorkbenchApplication.java
│
├── config
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── MongoConfig.java
│
├── controller
│   ├── AuthController.java
│   ├── PatientController.java
│   ├── VobRequestController.java
│   ├── EligibilityController.java
│   ├── CoverageController.java
│   ├── AuditController.java
│   ├── DashboardController.java
│   └── AdminController.java
│
├── dto
│   ├── request
│   └── response
│
├── exception
│   ├── ApiErrorResponse.java
│   ├── ErrorCode.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── ConflictException.java
│   └── GlobalExceptionHandler.java
│
├── model
│   ├── Patient.java
│   ├── User.java
│   ├── VobRequest.java
│   ├── AuditLog.java
│   ├── MockPayer.java
│   ├── MockPlan.java
│   └── MockProcedureRule.java
│
├── model/embedded
│   ├── InsurancePolicySnapshot.java
│   ├── RequestedProcedure.java
│   ├── EligibilityResult.java
│   ├── EligibilityCheck.java
│   ├── ProcedureCoverage.java
│   ├── CoverageSummary.java
│   └── InternalNote.java
│
├── model/enums
│   ├── Role.java
│   ├── VobStatus.java
│   ├── Priority.java
│   ├── InsuranceOrder.java
│   ├── PlanType.java
│   ├── NetworkStatus.java
│   ├── CoverageStatus.java
│   ├── EligibilityCheckStatus.java
│   ├── AuditEventType.java
│   └── AuditSource.java
│
├── repository
│   ├── PatientRepository.java
│   ├── UserRepository.java
│   ├── VobRequestRepository.java
│   ├── AuditLogRepository.java
│   ├── MockPayerRepository.java
│   ├── MockPlanRepository.java
│   └── MockProcedureRuleRepository.java
│
├── security
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
│
├── service
│   ├── AuthService.java
│   ├── PatientService.java
│   ├── VobRequestService.java
│   ├── WorkflowService.java
│   ├── AuditService.java
│   ├── MockEligibilityService.java
│   ├── CoverageCalculationService.java
│   ├── DashboardService.java
│   └── AdminService.java
│
└── util
    ├── MaskingUtils.java
    ├── DateTimeUtils.java
    └── MoneyUtils.java
```

---

## 5. MongoDB Collections

Use these main collections:

```text
patients
users
vobRequests
auditLogs
mockPayers
mockPlans
mockProcedureRules
```

### 5.1 Why MongoDB?

MongoDB is a good fit because a VOB request contains nested data:

- Insurance policies
- Procedures
- Eligibility result
- Coverage summary
- Internal notes
- Eligibility checks

This data naturally fits as a document structure.

### 5.2 Embedding vs Referencing Decision

#### Reference Patient from VOB Request

Use `patientId` inside `VobRequest`.

Reason:

- Patient may have many VOB requests.
- Patient data is reused.
- Patient profile can be searched independently.

#### Embed Insurance Policies in VOB Request

Use `List<InsurancePolicySnapshot>`.

Reason:

- Insurance details are part of the verification snapshot.
- Even if the patient's insurance changes later, the VOB request should preserve what was verified at that time.
- VOB request should be auditable as a historical record.

#### Embed Procedures in VOB Request

Use `List<RequestedProcedure>`.

Reason:

- Procedures are specific to this request.
- Coverage calculation is request-specific.

#### Store Audit Logs Separately

Use separate `auditLogs` collection.

Reason:

- Audit records can grow large.
- Audit logs should be queried independently.
- Audit logs should avoid storing PHI directly.

---

## 6. Enums

Use enums instead of raw strings.

### 6.1 Role

```java
public enum Role {
    RECEPTIONIST,
    SPECIALIST,
    SUPERVISOR_ADMIN,
    VIEWER
}
```

### 6.2 VobStatus

```java
public enum VobStatus {
    PENDING,
    IN_PROGRESS,
    NEEDS_INFO,
    SPECIALIST_REVIEW,
    VERIFIED,
    UNABLE_TO_VERIFY,
    REOPENED,
    CANCELLED
}
```

### 6.3 Priority

```java
public enum Priority {
    ROUTINE,
    URGENT
}
```

### 6.4 InsuranceOrder

```java
public enum InsuranceOrder {
    PRIMARY,
    SECONDARY,
    TERTIARY
}
```

### 6.5 PlanType

```java
public enum PlanType {
    PPO,
    HMO,
    EPO,
    POS
}
```

### 6.6 NetworkStatus

```java
public enum NetworkStatus {
    IN_NETWORK,
    OUT_OF_NETWORK,
    UNKNOWN
}
```

### 6.7 CoverageStatus

```java
public enum CoverageStatus {
    COVERED,
    PARTIALLY_COVERED,
    NOT_COVERED,
    REQUIRES_AUTHORIZATION,
    UNKNOWN
}
```

### 6.8 EligibilityCheckStatus

```java
public enum EligibilityCheckStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    INCOMPLETE,
    FAILED,
    MANUAL_REVIEW_REQUIRED
}
```

### 6.9 AuditEventType

```java
public enum AuditEventType {
    USER_LOGIN,
    USER_LOGOUT,

    PATIENT_CREATED,
    PATIENT_UPDATED,

    VOB_REQUEST_CREATED,
    VOB_REQUEST_ASSIGNED,
    STATUS_CHANGED,
    REQUEST_LOCKED,
    REQUEST_REOPENED,
    REQUEST_CANCELLED,

    INSURANCE_POLICY_ADDED,
    INSURANCE_POLICY_UPDATED,
    INSURANCE_ORDER_CHANGED,

    PROCEDURE_ADDED,
    PROCEDURE_UPDATED,
    PROCEDURE_REMOVED,

    VALIDATION_STARTED,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    MISSING_INFO_DETECTED,

    ELIGIBILITY_CHECK_REQUESTED,
    ELIGIBILITY_CHECK_COMPLETED,
    ELIGIBILITY_CHECK_FAILED,
    ELIGIBILITY_RESPONSE_INCOMPLETE,

    MANUAL_FIELD_CORRECTION,
    MANUAL_COPAY_ENTERED,
    MANUAL_DEDUCTIBLE_ENTERED,
    MANUAL_COINSURANCE_ENTERED,
    MANUAL_NETWORK_STATUS_CHANGED,
    MANUAL_PROCEDURE_COVERAGE_UPDATED,

    COVERAGE_CALCULATED,
    COVERAGE_RECALCULATED,
    PATIENT_RESPONSIBILITY_UPDATED,

    REQUEST_VERIFIED,
    REQUEST_UNABLE_TO_VERIFY,

    VERSION_CONFLICT_DETECTED,
    STALE_UPDATE_REJECTED,
    REQUEST_RELOADED_AFTER_CONFLICT,

    UNAUTHORIZED_WRITE_ATTEMPT
}
```

### 6.10 AuditSource

```java
public enum AuditSource {
    USER,
    SYSTEM,
    MOCK_ELIGIBILITY_SERVICE
}
```

### 6.11 ErrorCode

```java
public enum ErrorCode {
    PATIENT_NOT_FOUND,
    USER_NOT_FOUND,
    VOB_REQUEST_NOT_FOUND,

    INVALID_STATUS_TRANSITION,
    VERIFIED_REQUEST_LOCKED,
    ELIGIBILITY_RESULT_REQUIRED,

    INVALID_MEMBER_ID,
    INVALID_COVERAGE_DATES,
    DATE_OF_SERVICE_IN_PAST,
    DUPLICATE_PRIMARY_INSURANCE,
    INVALID_INSURANCE_ORDER,
    INSURANCE_NOT_ACTIVE,

    PROCEDURE_NOT_FOUND,
    PROCEDURE_NOT_COVERED,
    PROCEDURE_REQUIRES_AUTHORIZATION,

    COVERAGE_CALCULATION_REQUIRED,
    MANUAL_REVIEW_REQUIRED,

    VERSION_CONFLICT,
    ACCESS_DENIED,
    AUTHENTICATION_FAILED,

    VALIDATION_ERROR,
    INTERNAL_ERROR
}
```

---

## 7. Core Models

### 7.1 Patient

```java
@Document(collection = "patients")
public class Patient {
    @Id
    private String id;

    @Indexed(unique = true)
    private String mrn;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;

    private Instant createdAt;
    private Instant updatedAt;
}
```

### 7.2 User

```java
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;
    private String fullName;
    private Role role;
    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;
}
```

### 7.3 VobRequest

```java
@Document(collection = "vobRequests")
public class VobRequest {
    @Id
    private String id;

    @Indexed
    private String patientId;

    private LocalDate dateOfService;
    private Priority priority;
    private VobStatus status;

    private String assignedTo;
    private String createdBy;

    private List<InsurancePolicySnapshot> insurancePolicies;
    private List<RequestedProcedure> procedures;

    private EligibilityResult eligibilityResult;
    private CoverageSummary coverageSummary;
    private List<EligibilityCheck> eligibilityChecks;
    private List<InternalNote> notes;

    private boolean locked;

    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;
    private String verifiedBy;
    private Instant verifiedAt;
}
```

### 7.4 InsurancePolicySnapshot

```java
public class InsurancePolicySnapshot {
    private String id;

    private String payerName;
    private String memberId;
    private String groupNumber;
    private PlanType planType;
    private String relationshipToSubscriber;

    private InsuranceOrder insuranceOrder;

    private LocalDate coverageStart;
    private LocalDate coverageEnd;

    private NetworkStatus networkStatus;
    private boolean active;
}
```

### 7.5 RequestedProcedure

```java
public class RequestedProcedure {
    private String procedureCode;
    private String procedureName;
    private BigDecimal estimatedCharge;

    private CoverageStatus coverageStatus;
    private boolean requiresAuthorization;
    private String notes;
}
```

### 7.6 EligibilityResult

```java
public class EligibilityResult {
    private Boolean coverageActive;
    private NetworkStatus networkStatus;

    private BigDecimal copay;
    private Integer coinsurancePercent;

    private BigDecimal deductibleTotal;
    private BigDecimal deductibleMet;

    private BigDecimal oopMax;
    private BigDecimal oopMet;

    private String notes;

    private String verifiedBy;
    private Instant verifiedAt;
}
```

### 7.7 ProcedureCoverage

```java
public class ProcedureCoverage {
    private String procedureCode;
    private String procedureName;

    private BigDecimal estimatedCharge;

    private BigDecimal primaryPays;
    private BigDecimal secondaryPays;
    private BigDecimal tertiaryPays;

    private BigDecimal totalInsurancePays;
    private BigDecimal patientResponsibility;

    private BigDecimal copayApplied;
    private BigDecimal deductibleApplied;
    private BigDecimal coinsuranceApplied;
    private BigDecimal nonCoveredAmount;

    private CoverageStatus coverageStatus;
    private boolean requiresAuthorization;

    private String notes;
}
```

### 7.8 CoverageSummary

```java
public class CoverageSummary {
    private BigDecimal totalEstimatedCharge;

    private BigDecimal totalPrimaryPays;
    private BigDecimal totalSecondaryPays;
    private BigDecimal totalTertiaryPays;

    private BigDecimal totalInsurancePays;
    private BigDecimal totalPatientResponsibility;

    private BigDecimal totalCopay;
    private BigDecimal totalDeductibleApplied;
    private BigDecimal totalCoinsurance;
    private BigDecimal totalNonCoveredAmount;

    private List<ProcedureCoverage> procedureCoverages;

    private Instant calculatedAt;
    private String calculatedBy;
}
```

### 7.9 AuditLog

```java
@Document(collection = "auditLogs")
public class AuditLog {
    @Id
    private String id;

    @Indexed
    private String requestId;

    private String patientId;

    private String actorUserId;
    private String actorUsername;
    private Role actorRole;

    private AuditEventType eventType;

    private String entityType;
    private String entityId;

    private String fieldName;
    private String oldValue;
    private String newValue;

    private String reason;
    private AuditSource source;

    private Map<String, Object> metadata;

    private Instant timestamp;
}
```

---

## 8. PHI / HIPAA Hygiene Rules

Use only synthetic data.

Do not log or expose unnecessarily:

- Patient full name
- DOB
- MRN
- Phone
- Member ID
- Subscriber name
- Group number when unnecessary

### 8.1 Logging Rule

Bad:

```java
log.info("Created VOB for patient John Silva with memberId ABC123");
```

Good:

```java
log.info("Created VOB request id={} for patientId={}", requestId, patientId);
```

### 8.2 Audit Rule

For sensitive fields, store masked values.

Bad audit:

```json
{
  "fieldName": "memberId",
  "oldValue": "ABC123456",
  "newValue": "XYZ987654"
}
```

Good audit:

```json
{
  "fieldName": "memberId",
  "oldValue": "MASKED",
  "newValue": "MASKED"
}
```

Create utility:

```java
public class MaskingUtils {
    public static String maskSensitiveField(String fieldName, Object value) {
        // memberId, mrn, phone, dob, name should return "MASKED"
    }
}
```

---

## 9. Status Workflow Rules

### 9.1 Allowed Transitions

```text
PENDING → IN_PROGRESS
PENDING → CANCELLED

IN_PROGRESS → NEEDS_INFO
IN_PROGRESS → SPECIALIST_REVIEW
IN_PROGRESS → UNABLE_TO_VERIFY

NEEDS_INFO → IN_PROGRESS
NEEDS_INFO → CANCELLED

SPECIALIST_REVIEW → VERIFIED
SPECIALIST_REVIEW → UNABLE_TO_VERIFY
SPECIALIST_REVIEW → NEEDS_INFO

VERIFIED → REOPENED only by SUPERVISOR_ADMIN

REOPENED → IN_PROGRESS

UNABLE_TO_VERIFY → REOPENED only by SUPERVISOR_ADMIN

CANCELLED is terminal
```

### 9.2 Hard Rules

- `PENDING` cannot directly become `VERIFIED`.
- `VERIFIED` requires a valid eligibility result.
- `VERIFIED` requires coverage calculation.
- `VERIFIED` requests are locked.
- Only `SUPERVISOR_ADMIN` can reopen.
- `VIEWER` cannot change status.
- `RECEPTIONIST` cannot verify.
- `SPECIALIST` can verify only when the request is assigned to them or supervisor allows it.

### 9.3 WorkflowService Responsibility

Create `WorkflowService`:

```java
public void validateTransition(
    VobRequest request,
    VobStatus targetStatus,
    User currentUser
)
```

Responsibilities:

- Check allowed transition
- Check user role
- Check locked request
- Check eligibility result requirement
- Check coverage summary requirement
- Throw `BusinessException` with `ErrorCode.INVALID_STATUS_TRANSITION` if invalid

---

## 10. Mock Coverage Master Data

All data must be synthetic.

### 10.1 MockPayer

```java
@Document(collection = "mockPayers")
public class MockPayer {
    @Id
    private String id;

    private String payerName;
    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;
}
```

### 10.2 MockPlan

```java
@Document(collection = "mockPlans")
public class MockPlan {
    @Id
    private String id;

    private String payerName;
    private String planName;
    private PlanType planType;

    private BigDecimal defaultCopay;
    private Integer defaultCoinsurancePercent;

    private BigDecimal deductibleTotal;
    private BigDecimal oopMax;

    private boolean active;
}
```

### 10.3 MockProcedureRule

```java
@Document(collection = "mockProcedureRules")
public class MockProcedureRule {
    @Id
    private String id;

    private String payerName;
    private PlanType planType;
    private String procedureCode;

    private CoverageStatus coverageStatus;

    private Integer coveragePercent;
    private boolean deductibleApplies;
    private boolean copayApplies;
    private boolean requiresAuthorization;

    private boolean active;
}
```

### 10.4 MockEligibilityService

Create:

```java
public class MockEligibilityService {
    public EligibilityResult runEligibilityCheck(VobRequest request) {
        // read insurance policies
        // read mock master data
        // validate coverage active by dateOfService
        // generate synthetic eligibility result
        // return complete/incomplete result
    }
}
```

This service should:

- Check coverage dates
- Check policy active status
- Check member ID format
- Check mock payer availability
- Return coverage result
- Flag incomplete results
- Create audit event

---

## 11. Coverage Calculation Logic

Create:

```java
public class CoverageCalculationService {
    public CoverageSummary calculateCoverage(VobRequest request) {
        // calculate each procedure
        // apply primary, secondary, tertiary
        // sum totals
    }
}
```

### 11.1 Calculation Order

```text
For each procedure:
    charge = estimatedCharge

    remaining = charge

    primaryPays = apply primary rule
    remaining = remaining - primaryPays

    secondaryPays = apply secondary rule to remaining
    remaining = remaining - secondaryPays

    tertiaryPays = apply tertiary rule to remaining
    remaining = remaining - tertiaryPays

    patientResponsibility = remaining + copay + deductible + coinsurance where applicable

    ensure:
        insurancePays <= charge
        patientResponsibility >= 0
```

### 11.2 Simplified Formula

For first implementation:

```text
insurancePays = charge * coveragePercent / 100
patientResponsibility = charge - insurancePays + copay + deductibleApplied
```

Then apply secondary and tertiary only to remaining amount.

### 11.3 Safety Rules

- Do not allow negative patient responsibility.
- Do not allow total insurance pay more than charge.
- Do not calculate coverage for inactive policies.
- If coverage status is `UNKNOWN`, mark request as `SPECIALIST_REVIEW`.
- If authorization is required, show warning.

---

## 12. Audit Trail

Audit trail must capture every meaningful action.

### 12.1 Audit Principle

Create audit event when:

- Data changes
- Status changes
- Assignment changes
- Eligibility check runs
- Mock response is incomplete
- Manual correction is made
- Coverage is calculated
- Request is verified
- Request is reopened
- Unauthorized action is attempted
- Version conflict is detected

### 12.2 AuditService

Create:

```java
public class AuditService {
    public void record(AuditEventRequest request) {
        // create AuditLog
        // mask sensitive values
        // save to auditLogs
    }
}
```

### 12.3 Audit Examples

#### Status Change

```json
{
  "eventType": "STATUS_CHANGED",
  "fieldName": "status",
  "oldValue": "PENDING",
  "newValue": "IN_PROGRESS",
  "source": "USER"
}
```

#### Manual Correction

```json
{
  "eventType": "MANUAL_FIELD_CORRECTION",
  "fieldName": "copay",
  "oldValue": "UNKNOWN",
  "newValue": "500.00",
  "reason": "Mock payer response incomplete",
  "source": "USER"
}
```

#### Sensitive Manual Correction

```json
{
  "eventType": "MANUAL_FIELD_CORRECTION",
  "fieldName": "memberId",
  "oldValue": "MASKED",
  "newValue": "MASKED",
  "reason": "Corrected from insurance card",
  "source": "USER"
}
```

---

## 13. Optimistic Locking

Use `@Version` in `VobRequest`.

```java
@Version
private Long version;
```

### 13.1 Why?

Two specialists may open the same request. If one updates first, the second user should not silently overwrite.

### 13.2 Expected Behavior

```text
User A opens request version 1
User B opens request version 1

User A saves → database version becomes 2

User B saves with version 1 → backend returns 409 Conflict
```

### 13.3 Error Response

```json
{
  "timestamp": "2026-06-30T10:30:00Z",
  "status": 409,
  "errorCode": "VERSION_CONFLICT",
  "message": "This request was updated by another user. Please reload and try again.",
  "path": "/api/vob-requests/{id}/status",
  "details": []
}
```

---

## 14. Standard API Error Handling

Use one error response format everywhere.

### 14.1 ApiErrorResponse

```java
public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private ErrorCode errorCode;
    private String message;
    private String path;
    private List<String> details;
}
```

### 14.2 GlobalExceptionHandler

Use:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // handle MethodArgumentNotValidException → 400
    // handle BusinessException → status from exception
    // handle ResourceNotFoundException → 404
    // handle OptimisticLockingFailureException → 409
    // handle AccessDeniedException → 403
    // handle generic Exception → 500
}
```

### 14.3 Recommended Status Codes

```text
400 Bad Request
    validation failure
    invalid request body
    invalid member ID
    invalid coverage dates

401 Unauthorized
    missing/invalid token

403 Forbidden
    role not allowed

404 Not Found
    patient/request/user not found

409 Conflict
    invalid status transition
    verified request locked
    optimistic locking/version conflict

500 Internal Server Error
    unexpected server error
```

---

## 15. Validation Rules

### 15.1 Patient

- `mrn` required
- `mrn` unique
- `firstName` required
- `lastName` required
- `dateOfBirth` must be in the past
- `phone` basic format

### 15.2 VOB Request

- `patientId` required
- patient must exist
- `dateOfService` must not be in the past at creation
- at least one insurance policy required
- at least one procedure required
- priority required

### 15.3 Insurance Policy

- payer name required
- member ID required
- member ID alphanumeric and minimum length
- coverage end after coverage start
- only one primary insurance
- secondary cannot exist without primary
- tertiary cannot exist without secondary

### 15.4 Procedure

- procedure code required
- procedure name required
- estimated charge must be positive

### 15.5 Verification

To mark `VERIFIED`:

- eligibility result must exist
- coverage summary must exist
- no required missing fields
- request must not be locked
- target status must be allowed
- current user must be `SPECIALIST` or `SUPERVISOR_ADMIN`

---

## 16. DTO Design

Do not expose MongoDB documents directly.

Use request/response DTOs.

### 16.1 Patient DTOs

```text
CreatePatientRequest
UpdatePatientRequest
PatientResponse
```

### 16.2 VOB DTOs

```text
CreateVobRequestRequest
VobRequestResponse
VobWorklistItemResponse
AssignVobRequestRequest
TransitionStatusRequest
UpdateEligibilityResultRequest
ManualCorrectionRequest
CoverageSummaryResponse
```

### 16.3 Audit DTOs

```text
AuditLogResponse
AuditEventRequest
```

### 16.4 Auth DTOs

```text
LoginRequest
LoginResponse
CurrentUserResponse
```

---

## 17. REST API Contract

### 17.1 Auth

```http
POST /api/auth/login
GET  /api/auth/me
```

### 17.2 Patients

```http
POST  /api/patients
GET   /api/patients?q=
GET   /api/patients/{id}
PATCH /api/patients/{id}
```

### 17.3 VOB Requests

```http
POST  /api/vob-requests
GET   /api/vob-requests?status=&payer=&q=&assignedTo=&page=&size=
GET   /api/vob-requests/{id}
PATCH /api/vob-requests/{id}/assign
PATCH /api/vob-requests/{id}/status
PATCH /api/vob-requests/{id}/insurance-policies
PATCH /api/vob-requests/{id}/procedures
```

### 17.4 Eligibility

```http
POST  /api/vob-requests/{id}/eligibility-check
PATCH /api/vob-requests/{id}/eligibility-result
POST  /api/vob-requests/{id}/manual-corrections
```

### 17.5 Coverage

```http
POST /api/vob-requests/{id}/calculate-coverage
GET  /api/vob-requests/{id}/coverage-summary
```

### 17.6 Audit

```http
GET /api/vob-requests/{id}/audit
```

### 17.7 Dashboard

```http
GET /api/dashboard/summary
```

### 17.8 Admin

```http
GET  /api/admin/users
POST /api/admin/users

GET  /api/admin/mock-payers
POST /api/admin/mock-payers

GET  /api/admin/mock-plans
POST /api/admin/mock-plans

GET  /api/admin/mock-procedure-rules
POST /api/admin/mock-procedure-rules
```

---

## 18. Security Rules

### 18.1 Access Matrix

| Feature | Receptionist | Specialist | Supervisor/Admin | Viewer |
|---|---:|---:|---:|---:|
| Create patient | Yes | No/Optional | Yes | No |
| View patient | Yes | Yes | Yes | Yes |
| Create VOB request | Yes | Yes | Yes | No |
| View worklist | Yes | Yes | Yes | Yes |
| Assign request | No | No | Yes | No |
| Start processing | No | Yes | Yes | No |
| Run eligibility check | No | Yes | Yes | No |
| Manual correction | No | Yes | Yes | No |
| Verify request | No | Yes | Yes | No |
| Reopen request | No | No | Yes | No |
| Manage users/master data | No | No | Yes | No |
| View audit trail | Yes | Yes | Yes | Yes |

### 18.2 Spring Security

Use method-level security:

```java
@PreAuthorize("hasRole('SPECIALIST') or hasRole('SUPERVISOR_ADMIN')")
```

### 18.3 Passwords

Use BCrypt.

Never store plaintext passwords.

---

## 19. Swagger / OpenAPI

Add dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.0</version>
</dependency>
```

Swagger URL:

```text
http://localhost:8080/swagger-ui.html
```

Each controller method should have:

- Summary
- Description
- Response codes
- Request/response schemas

---

## 20. Docker Compose

Root-level `docker-compose.yml`:

```yaml
services:
  mongodb:
    image: mongo:7
    container_name: vob-workbench-mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

  backend:
    build:
      context: ./backend
    container_name: vob-workbench-api
    ports:
      - "8080:8080"
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/vob_workbench
      JWT_SECRET: change-me-in-env
    depends_on:
      - mongodb

volumes:
  mongo_data:
```

Backend `Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 21. application.yml

```yaml
spring:
  application:
    name: vob-workbench

  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/vob_workbench}

server:
  port: 8080

security:
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-me}
    expiration-ms: 86400000

logging:
  level:
    root: INFO
    com.synergen.vobworkbench: INFO
```

---

## 22. Seed Data

Create a seed runner for development:

```java
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // create users
        // create mock payers
        // create mock plans
        // create mock procedure rules
        // create sample patients
    }
}
```

Seed users:

```text
receptionist1 / password123 / RECEPTIONIST
specialist1 / password123 / SPECIALIST
admin1        / password123 / SUPERVISOR_ADMIN
viewer1       / password123 / VIEWER
```

Seed procedures:

```text
CONS-001 Consultation
LAB-001  Blood Test
IMG-001  X-Ray
MRI-001  MRI Scan
PHY-001  Physiotherapy
```

Seed payers:

```text
SynerCare Insurance
MediPlus Health
CareFirst Mock
```

All data must be synthetic.

---

## 23. Testing Strategy

### 23.1 Unit Tests

Test services:

```text
WorkflowServiceTest
CoverageCalculationServiceTest
MockEligibilityServiceTest
AuditServiceTest
PatientServiceTest
VobRequestServiceTest
```

### 23.2 Must-have Test Cases

Workflow tests:

- `PENDING → IN_PROGRESS` allowed
- `PENDING → VERIFIED` rejected
- `SPECIALIST_REVIEW → VERIFIED` allowed only with eligibility result and coverage summary
- `VERIFIED` request cannot be edited
- only supervisor/admin can reopen

Validation tests:

- date of service in past rejected
- coverage end before start rejected
- duplicate primary insurance rejected
- missing member ID rejected
- patient not found returns 404

Coverage tests:

- primary insurance calculation
- secondary insurance calculation
- patient responsibility never negative
- total insurance payment never exceeds charge
- unknown coverage triggers review

Audit tests:

- status change creates audit
- manual correction creates audit
- sensitive fields are masked

Concurrency test:

- stale version update returns conflict

---

## 24. GitHub Actions CI

Create `.github/workflows/backend-ci.yml`:

```yaml
name: Backend CI

on:
  push:
    paths:
      - "backend/**"
      - ".github/workflows/backend-ci.yml"
  pull_request:
    paths:
      - "backend/**"

jobs:
  build-test:
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: backend

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven

      - name: Build and test
        run: mvn clean test
```

---

## 25. Development Order for Codex

When using Codex, implement in this order.

### Phase 1: Foundation

1. Create enums
2. Create model classes
3. Create repositories
4. Create exception classes
5. Create standard error response
6. Create global exception handler

### Phase 2: Patient Module

1. Create patient DTOs
2. Create patient service
3. Create patient controller
4. Add validation
5. Add audit for create/update
6. Add tests

### Phase 3: VOB Request Module

1. Create VOB DTOs
2. Create request creation logic
3. Validate patient exists
4. Validate insurance policies
5. Validate procedures
6. Create worklist query
7. Add request detail endpoint
8. Add audit for request creation

### Phase 4: Workflow

1. Create WorkflowService
2. Implement status transition rules
3. Implement locked request rule
4. Implement assign/reassign
5. Add audit for status/assignment changes
6. Add tests

### Phase 5: Mock Eligibility

1. Create mock master data models
2. Create mock repositories
3. Create seed data
4. Create MockEligibilityService
5. Create eligibility endpoint
6. Auto-fill eligibility result
7. Audit mock check events

### Phase 6: Coverage Calculation

1. Create CoverageCalculationService
2. Calculate procedure-level coverage
3. Apply primary/secondary/tertiary order
4. Calculate summary totals
5. Audit calculation events
6. Add tests

### Phase 7: Manual Corrections

1. Create ManualCorrectionRequest
2. Allow specialist to update allowed fields
3. Mask sensitive audit values
4. Recalculate coverage after correction if needed
5. Add tests

### Phase 8: Security

1. Create User model/repository
2. Add BCrypt password hashing
3. Add login endpoint
4. Add JWT
5. Add role-based endpoint protection
6. Add method security

### Phase 9: Dashboard and Audit

1. Create dashboard summary endpoint
2. Create audit history endpoint
3. Add filtering/sorting audit logs

### Phase 10: Engineering

1. Add Swagger annotations
2. Add Dockerfile
3. Update docker-compose
4. Add CI workflow
5. Add README run instructions

---

## 26. Coding Rules for Codex

When generating code, follow these rules.

### 26.1 General

- Use Java 21.
- Use Spring Boot 3.x.
- Use Spring Data MongoDB, not JPA.
- Use DTOs. Do not expose Mongo documents directly.
- Keep controllers thin.
- Put business rules in services.
- Use enums instead of strings.
- Use `BigDecimal` for money.
- Use `Instant` for timestamps.
- Use `LocalDate` for dates without time.
- Use constructor injection.
- Use clear method names.
- Keep methods small.
- Add comments only where useful.

### 26.2 Security

- No plaintext passwords.
- Use BCrypt.
- Use role-based access.
- Do not log tokens or passwords.
- Keep JWT secret in environment variables.

### 26.3 PHI

- No real patient data.
- Do not log PHI.
- Do not store PHI in audit old/new values.
- Mask member IDs, MRNs, phone numbers, DOB, and names in audit events.

### 26.4 Errors

- Always return standard `ApiErrorResponse`.
- Use correct HTTP status codes.
- Never return stack traces to client.
- Error messages should be clear but not leak PHI.

### 26.5 Audit

- Audit all meaningful changes.
- Audit status transitions.
- Audit eligibility checks.
- Audit manual corrections.
- Audit coverage recalculation.
- Audit blocked unauthorized write attempts.
- Audit version conflicts.

### 26.6 Tests

- Test business logic, not only getters/setters.
- Test workflow rules.
- Test validation rules.
- Test coverage calculations.
- Test audit creation.
- Test conflict behavior.

---

## 27. Example User Stories

### Story: Receptionist Creates VOB Request

```text
As a receptionist,
I want to create a VOB request for a patient with insurance details and planned procedures,
so that a specialist can verify benefits before service.
```

Acceptance criteria:

- Patient must exist.
- Date of service must not be in the past.
- At least one insurance policy is required.
- At least one procedure is required.
- Request status starts as `PENDING`.
- Audit event `VOB_REQUEST_CREATED` is saved.

### Story: Specialist Verifies Request

```text
As a specialist,
I want to process a VOB request and verify coverage,
so that the front office can see the final patient responsibility estimate.
```

Acceptance criteria:

- Specialist can move request from `PENDING` to `IN_PROGRESS`.
- Specialist can run mock eligibility check.
- System creates eligibility result.
- Specialist can manually correct missing fields.
- System calculates coverage summary.
- Specialist can move request to `VERIFIED`.
- Verified request is locked.
- Audit trail shows all actions.

---

## 28. Definition of Done

Backend is done when:

- App starts successfully
- MongoDB connection works
- Swagger opens at `/swagger-ui.html`
- Auth/login works
- Role restrictions work
- Patient APIs work
- VOB request APIs work
- Worklist filtering/searching works
- Status workflow is enforced
- Mock eligibility check works
- Coverage calculation works
- Manual correction works
- Audit trail works
- Optimistic locking works
- Docker compose works
- Unit tests pass
- README explains setup and test users

---

## 29. Local Run Commands

### Start MongoDB

```bash
docker compose up -d mongodb
```

### Run backend locally

```bash
cd backend
mvn spring-boot:run
```

### Health check

```text
http://localhost:8080/api/health
```

### Swagger

```text
http://localhost:8080/swagger-ui.html
```

---

## 30. Suggested First Commit Plan

```text
commit 1: initial backend skeleton
commit 2: add domain enums and models
commit 3: add patient module
commit 4: add vob request creation and worklist
commit 5: add workflow validation and audit trail
commit 6: add mock eligibility and coverage calculation
commit 7: add security and roles
commit 8: add swagger docker and ci
commit 9: add tests and polish
```

---

## 31. Final Backend Goal

The backend should feel like a professional mini healthcare operations system:

```text
Receptionist creates the request.
Specialist verifies and corrects.
System mocks payer coverage and calculates responsibility.
Supervisor/Admin manages and audits.
Viewer reads final results.
Every important action is tracked safely.
```

Keep the implementation clean, testable, and explainable in a code review.
