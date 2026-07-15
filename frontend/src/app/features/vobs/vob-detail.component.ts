import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AuditEvent, EligibilityResult, Priority, VobRequest } from '../../core/models/api.types';
import { PermissionsService } from '../../core/services/permissions.service';
import { VobRequestService } from '../../core/services/vob-request.service';
import { handleAction, handleRequest } from '../../core/utils/request-state';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

type PendingAction = 'verify' | 'reopen' | '';

@Component({
  selector: 'app-vob-detail',
  imports: [
    CommonModule,
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    StatusBadgeComponent,
    ModalComponent,
    ConfirmModalComponent,
    EmptyStateComponent,
    LoadingIndicatorComponent,
  ],
  templateUrl: './vob-detail.component.html',
  styleUrl: './vob-detail.component.scss',
})
export class VobDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly vobService = inject(VobRequestService);
  readonly permissions = inject(PermissionsService);

  readonly priorities: Priority[] = ['ROUTINE', 'URGENT'];

  readonly loading = signal(true);
  readonly acting = signal(false);
  readonly activeAction = signal('');
  readonly error = signal('');
  readonly message = signal('');
  readonly request = signal<VobRequest | undefined>(undefined);
  readonly auditEvents = signal<AuditEvent[]>([]);
  readonly editModalOpen = signal(false);
  readonly eligibilityModalOpen = signal(false);
  readonly auditModalOpen = signal(false);
  readonly pendingAction = signal<PendingAction>('');

  readonly editForm = this.fb.nonNullable.group({
    dateOfService: ['', Validators.required],
    priority: ['ROUTINE' as Priority, Validators.required],
    assignedTo: [''],
    payerName: ['', Validators.required],
    memberId: ['', Validators.required],
    groupNumber: [''],
    coverageStart: [''],
    coverageEnd: [''],
    procedureCode: ['', Validators.required],
    procedureName: ['', Validators.required],
    estimatedCharge: [0, [Validators.required, Validators.min(1)]],
    requiresAuthorization: [false],
    note: [''],
    reason: ['Updated from Angular UI'],
  });

  readonly eligibilityForm = this.fb.nonNullable.group({
    coverageActive: [true],
    networkStatus: ['IN_NETWORK' as 'IN_NETWORK' | 'OUT_OF_NETWORK' | 'UNKNOWN'],
    copay: [25],
    coinsurancePercent: [20],
    deductibleRemaining: [100],
    notes: ['Manual eligibility review.'],
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.load(params.get('id'));
    });
  }

  load(id: string | null | undefined = this.request()?.id): void {
    if (!id) {
      this.error.set('Missing request id.');
      this.loading.set(false);
      return;
    }

    handleRequest(this.vobService.get(id), this, (request) => {
      this.request.set(request);
      this.populateForms(request);
    });
  }

  openEdit(): void {
    const request = this.request();

    if (!this.permissions.canEditVobRequest(request)) {
      return;
    }

    if (request) {
      this.populateForms(request);
    }
    this.editModalOpen.set(true);
  }

  saveEdit(): void {
    const request = this.request();

    if (!request || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const value = this.editForm.getRawValue();
    const patch = {
      dateOfService: value.dateOfService,
      priority: value.priority,
      insurancePolicies: [
        {
          payerName: value.payerName,
          memberId: value.memberId,
          groupNumber: value.groupNumber,
          planType: request.insurancePolicies[0]?.planType || 'PPO',
          insuranceOrder: 'PRIMARY' as const,
          coverageStart: value.coverageStart,
          coverageEnd: value.coverageEnd,
          active: true,
        },
      ],
      procedures: [
        {
          procedureCode: value.procedureCode,
          procedureName: value.procedureName,
          estimatedCharge: value.estimatedCharge,
          requiresAuthorization: value.requiresAuthorization,
        },
      ],
      note: value.note,
      reason: value.reason,
      version: request.version,
    };

    if (this.permissions.canAssignVobRequest()) {
      Object.assign(patch, { assignedTo: value.assignedTo });
    }

    this.runAction('Request changes saved', 'Saving request changes', () => this.vobService.patch(request.id, patch));
    this.editModalOpen.set(false);
  }

  saveEligibility(): void {
    const request = this.request();

    if (!this.permissions.canRunSpecialistWorkflow(request)) {
      return;
    }

    const value = this.eligibilityForm.getRawValue();
    const eligibilityResult: EligibilityResult = {
      coverageActive: value.coverageActive,
      networkStatus: value.networkStatus,
      copay: value.copay,
      coinsurancePercent: value.coinsurancePercent,
      deductibleRemaining: value.deductibleRemaining,
      notes: value.notes,
    };

    this.runAction('Eligibility result saved', 'Saving eligibility result', () =>
      this.vobService.patch(request!.id, {
        eligibilityResult,
        reason: 'Manual eligibility correction',
        version: request!.version,
      }),
    );
    this.eligibilityModalOpen.set(false);
  }

  runEligibility(): void {
    const request = this.request();

    if (!this.permissions.canRunSpecialistWorkflow(request)) {
      return;
    }

    this.runAction('Eligibility completed', 'Running eligibility', () => this.vobService.runEligibility(request!.id));
  }

  calculateCoverage(): void {
    const request = this.request();

    if (!this.permissions.canRunSpecialistWorkflow(request)) {
      return;
    }

    this.activeAction.set('Calculating coverage');

    handleAction(
      this.vobService.calculateCoverage(request!.id),
      { loading: this.acting, error: this.error },
      () => {
        this.message.set('Coverage calculated.');
        this.activeAction.set('');
        this.load(request!.id);
      },
    );
  }

  confirm(action: PendingAction): void {
    const request = this.request();

    if (action === 'verify' && !this.permissions.canVerifyVobRequest(request)) {
      return;
    }

    if (action === 'reopen' && !this.permissions.canReopenVobRequest(request)) {
      return;
    }

    this.pendingAction.set(action);
  }

  completeConfirmedAction(): void {
    const request = this.request();
    const action = this.pendingAction();
    this.pendingAction.set('');

    if (!request) {
      return;
    }

    if (action === 'verify' && this.permissions.canVerifyVobRequest(request)) {
      this.runAction('Request verified', 'Verifying request', () => this.vobService.verify(request.id));
    }

    if (action === 'reopen' && this.permissions.canReopenVobRequest(request)) {
      this.runAction('Request reopened', 'Reopening request', () => this.vobService.reopen(request.id));
    }
  }

  loadAudit(): void {
    const request = this.request();

    if (!request) {
      return;
    }

    this.activeAction.set('Loading audit trail');

    handleRequest(
      this.vobService.audit(request.id),
      { loading: this.acting, error: this.error },
      (events) => {
        this.auditEvents.set(events);
        this.auditModalOpen.set(true);
        this.activeAction.set('');
      },
    );
  }

  private populateForms(request: VobRequest): void {
    const policy = request.insurancePolicies[0];
    const procedure = request.procedures[0];

    this.editForm.patchValue({
      dateOfService: request.dateOfService,
      priority: request.priority,
      assignedTo: request.assignedTo || '',
      payerName: policy?.payerName || '',
      memberId: policy?.memberId || '',
      groupNumber: policy?.groupNumber || '',
      coverageStart: policy?.coverageStart || '',
      coverageEnd: policy?.coverageEnd || '',
      procedureCode: procedure?.procedureCode || '',
      procedureName: procedure?.procedureName || '',
      estimatedCharge: procedure?.estimatedCharge || 0,
      requiresAuthorization: procedure?.requiresAuthorization || false,
      note: '',
      reason: 'Updated from Angular UI',
    });

    if (request.eligibilityResult) {
      this.eligibilityForm.patchValue({
        coverageActive: request.eligibilityResult.coverageActive === true,
        networkStatus: request.eligibilityResult.networkStatus,
        copay: request.eligibilityResult.copay || 0,
        coinsurancePercent: request.eligibilityResult.coinsurancePercent || 0,
        deductibleRemaining: request.eligibilityResult.deductibleRemaining || 0,
        notes: request.eligibilityResult.notes || '',
      });
    }
  }

  private runAction(label: string, loadingLabel: string, requestFactory: () => Observable<VobRequest>): void {
    this.message.set('');
    this.activeAction.set(loadingLabel);

    handleAction(requestFactory(), { loading: this.acting, error: this.error }, (request) => {
      this.request.set(request);
      this.populateForms(request);
      this.message.set(label);
      this.activeAction.set('');
    });
  }
}
