export type Role = 'RECEPTIONIST' | 'SPECIALIST' | 'SUPERVISOR_ADMIN' | 'VIEWER';
export type Priority = 'ROUTINE' | 'URGENT';
export type VobStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'NEEDS_INFO'
  | 'SPECIALIST_REVIEW'
  | 'VERIFIED'
  | 'UNABLE_TO_VERIFY'
  | 'REOPENED'
  | 'CANCELLED';
export type PlanType = 'PPO' | 'HMO' | 'EPO' | 'POS';
export type InsuranceOrder = 'PRIMARY' | 'SECONDARY' | 'TERTIARY';
export type NetworkStatus = 'IN_NETWORK' | 'OUT_OF_NETWORK' | 'UNKNOWN';
export type BenefitType =
  | 'COPAY_ONLY'
  | 'COINSURANCE_ONLY'
  | 'DEDUCTIBLE_THEN_COINSURANCE'
  | 'COPAY_THEN_COINSURANCE'
  | 'DEDUCTIBLE_COPAY_THEN_COINSURANCE'
  | 'NOT_COVERED';

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PageResponse<T> {
  content: T[];
  page: PageInfo;
}

export interface ApiError {
  timestamp?: string;
  status?: number;
  code?: string;
  errorCode?: string;
  message?: string;
  path?: string;
  details?: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: Role[];
}

export interface User {
  id?: string;
  username: string;
  fullName: string;
  role: Role;
  active: boolean;
}

export interface UserCreate {
  username: string;
  password: string;
  fullName: string;
  role: Role;
}

export interface PatientWrite {
  mrn: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender?: string;
  phone?: string;
}

export interface PatientPatch {
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  gender?: string;
  phone?: string;
}

export interface Patient extends PatientWrite {
  id: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface InsurancePolicy {
  payerName: string;
  memberId: string;
  groupNumber?: string;
  planType?: PlanType;
  insuranceOrder: InsuranceOrder;
  coverageStart?: string;
  coverageEnd?: string;
  active: boolean;
}

export interface Procedure {
  procedureCode: string;
  procedureName: string;
  estimatedCharge: number;
  requiresAuthorization: boolean;
}

export interface EligibilityResult {
  coverageActive: boolean | null;
  networkStatus: NetworkStatus;
  copay: number;
  coinsurancePercent: number;
  deductibleRemaining: number;
  notes?: string;
  deductibleTotal?: number;
  deductibleMet?: number;
  oopMax?: number;
  oopMet?: number;
  oopRemaining?: number;
  priorAuthorizationRequired?: boolean;
  benefitSource?: string;
  verifiedBy?: string;
  verifiedAt?: string;
}

export interface ProcedureCoverage {
  procedureCode: string;
  insurancePays: number;
  patientResponsibility: number;
  procedureName?: string;
  estimatedCharge?: number;
  allowedAmount?: number;
  deductibleApplied?: number;
  copayApplied?: number;
  coinsuranceApplied?: number;
  nonCoveredAmount?: number;
  priorAuthorizationRequired?: boolean;
  calculationNote?: string;
}

export interface CoverageSummary {
  totalEstimatedCharge: number;
  totalInsurancePays: number;
  totalPatientResponsibility: number;
  totalAllowedAmount?: number;
  totalDeductibleApplied?: number;
  totalCopayApplied?: number;
  totalCoinsuranceApplied?: number;
  totalNonCoveredAmount?: number;
  procedureCoverages: ProcedureCoverage[];
}

export interface VobRequestCreate {
  patientId: string;
  dateOfService: string;
  priority: Priority;
  assignedTo?: string;
  insurancePolicies: InsurancePolicy[];
  procedures: Procedure[];
}

export interface VobRequestPatch {
  dateOfService?: string;
  priority?: Priority;
  assignedTo?: string | null;
  insurancePolicies?: InsurancePolicy[];
  procedures?: Procedure[];
  eligibilityResult?: EligibilityResult;
  note?: string;
  reason?: string;
  version: number;
}

export interface VobRequest extends VobRequestCreate {
  id: string;
  status: VobStatus;
  locked: boolean;
  version: number;
  eligibilityResult?: EligibilityResult;
  coverageSummary?: CoverageSummary;
  notes?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AuditEvent {
  id?: string;
  requestId?: string;
  eventType?: string;
  action?: string;
  entityType?: string;
  entityId?: string;
  patientId?: string;
  vobRequestId?: string;
  actorUsername?: string;
  actorRole?: string;
  fieldName?: string;
  oldValue?: string;
  newValue?: string;
  httpMethod?: string;
  path?: string;
  success?: boolean;
  errorCode?: string;
  errorMessage?: string;
  timestamp?: string;
  metadata?: Record<string, unknown>;
}

export interface AuditEventPage {
  content: AuditEvent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface DashboardSummary {
  totalRequests: number;
  pending: number;
  inProgress: number;
  verified: number;
  urgent: number;
}

export interface MockProcedureRule {
  payerName: string;
  procedureCode: string;
  coveragePercent: number;
  coverageActive?: boolean;
  networkStatus?: NetworkStatus;
  copay?: number;
  coinsurancePercent?: number;
  deductibleRemaining?: number;
  planType?: PlanType;
  memberId?: string;
  groupNumber?: string;
  procedureName?: string;
  benefitType?: BenefitType;
  allowedAmount?: number;
  deductibleTotal?: number;
  deductibleMet?: number;
  oopMax?: number;
  oopMet?: number;
  deductibleApplies?: boolean;
  priorAuthorizationRequired?: boolean;
  notes?: string;
}

export interface MockData {
  id?: string;
  payers: string[];
  procedureRules: MockProcedureRule[];
}
