import { Injectable, inject } from '@angular/core';
import { VobRequest } from '../models/api.types';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class PermissionsService {
  private readonly auth = inject(AuthService);

  canCreatePatient(): boolean {
    return this.auth.hasRole(['RECEPTIONIST', 'SUPERVISOR_ADMIN']);
  }

  canEditPatient(): boolean {
    return this.auth.hasRole(['RECEPTIONIST', 'SUPERVISOR_ADMIN']);
  }

  canCreateVobRequest(): boolean {
    return this.auth.hasRole(['RECEPTIONIST', 'SPECIALIST', 'SUPERVISOR_ADMIN']);
  }

  canEditVobRequest(request?: VobRequest): boolean {
    return this.auth.hasRole(['SPECIALIST', 'SUPERVISOR_ADMIN']) && !request?.locked;
  }

  canAssignVobRequest(): boolean {
    return this.auth.hasRole(['SUPERVISOR_ADMIN']);
  }

  canRunSpecialistWorkflow(request?: VobRequest): boolean {
    return this.auth.hasRole(['SPECIALIST', 'SUPERVISOR_ADMIN']) && !request?.locked;
  }

  canVerifyVobRequest(request?: VobRequest): boolean {
    return (
      this.auth.hasRole(['SPECIALIST', 'SUPERVISOR_ADMIN']) &&
      !!request?.eligibilityResult &&
      !!request?.coverageSummary &&
      !request?.locked
    );
  }

  canReopenVobRequest(request?: VobRequest): boolean {
    return (
      this.auth.hasRole(['SUPERVISOR_ADMIN']) &&
      (request?.status === 'VERIFIED' || request?.status === 'UNABLE_TO_VERIFY')
    );
  }

  canManageAdmin(): boolean {
    return this.auth.hasRole(['SUPERVISOR_ADMIN']);
  }

  canSearchGlobalAudit(): boolean {
    return this.auth.hasRole(['SUPERVISOR_ADMIN']);
  }
}
