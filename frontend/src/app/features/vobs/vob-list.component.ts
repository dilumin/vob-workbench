import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PageInfo, Patient, Priority, VobRequest, VobStatus } from '../../core/models/api.types';
import { PatientService } from '../../core/services/patient.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { VobRequestService } from '../../core/services/vob-request.service';
import { handleAction, handleRequest } from '../../core/utils/request-state';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-vob-list',
  imports: [
    CommonModule,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    ModalComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    LoadingIndicatorComponent,
    PaginationComponent,
  ],
  templateUrl: './vob-list.component.html',
  styleUrl: './vob-list.component.scss',
})
export class VobListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly patientService = inject(PatientService);
  private readonly vobService = inject(VobRequestService);
  readonly permissions = inject(PermissionsService);

  readonly statuses: (VobStatus | '')[] = [
    '',
    'PENDING',
    'IN_PROGRESS',
    'NEEDS_INFO',
    'SPECIALIST_REVIEW',
    'VERIFIED',
    'UNABLE_TO_VERIFY',
    'REOPENED',
    'CANCELLED',
  ];
  readonly priorities: Priority[] = ['ROUTINE', 'URGENT'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly modalOpen = signal(false);
  readonly requests = signal<VobRequest[]>([]);
  readonly pageInfo = signal<PageInfo | undefined>(undefined);
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);

  readonly patientSearchLoading = signal(false);
  readonly patientSearchError = signal('');
  readonly patientResults = signal<Patient[]>([]);
  readonly selectedPatient = signal<Patient | undefined>(undefined);

  patientSearchTerm = '';
  filters: { status: VobStatus | ''; assignedTo: string; q: string } = { status: '', assignedTo: '', q: '' };

  readonly form = this.fb.nonNullable.group({
    dateOfService: [new Date().toISOString().slice(0, 10), Validators.required],
    priority: ['ROUTINE' as Priority, Validators.required],
    payerName: ['SynerCare Insurance', Validators.required],
    memberId: ['MEM12345', Validators.required],
    groupNumber: ['GRP-100'],
    planType: ['PPO'],
    coverageStart: [new Date().toISOString().slice(0, 10)],
    coverageEnd: [''],
    procedureCode: ['CONS-001', Validators.required],
    procedureName: ['Office Consultation', Validators.required],
    estimatedCharge: [180, [Validators.required, Validators.min(1)]],
    requiresAuthorization: [false],
  });

  ngOnInit(): void {
    this.load();
    this.watchPatientQueryParam();
  }

  load(page = this.currentPage()): void {
    this.currentPage.set(page);
    handleRequest(
      this.vobService.search({ ...this.filters, page, size: this.pageSize() }),
      this,
      (result) => {
        this.requests.set(result.content);
        this.pageInfo.set(result.page);
      },
    );
  }

  applyFilters(): void {
    this.load(0);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.load(0);
  }

  openCreateModal(): void {
    if (!this.permissions.canCreateVobRequest()) {
      return;
    }

    this.selectedPatient.set(undefined);
    this.patientResults.set([]);
    this.patientSearchTerm = '';
    this.patientSearchError.set('');
    this.form.patchValue({
      dateOfService: new Date().toISOString().slice(0, 10),
      priority: 'ROUTINE',
      payerName: 'SynerCare Insurance',
      memberId: 'MEM12345',
      groupNumber: 'GRP-100',
      planType: 'PPO',
      coverageStart: new Date().toISOString().slice(0, 10),
      coverageEnd: '',
      procedureCode: 'CONS-001',
      procedureName: 'Office Consultation',
      estimatedCharge: 180,
      requiresAuthorization: false,
    });
    this.modalOpen.set(true);
  }

  searchPatients(): void {
    if (!this.patientSearchTerm.trim()) {
      this.patientSearchError.set('Search by MRN, name, or phone before creating the request.');
      this.patientResults.set([]);
      return;
    }

    handleRequest(
      this.patientService.search(this.patientSearchTerm),
      { loading: this.patientSearchLoading, error: this.patientSearchError },
      (page) => {
        this.patientResults.set(page.content);
        if (!page.content.length) {
          this.patientSearchError.set('No patients matched that search.');
        }
      },
    );
  }

  selectPatient(patient: Patient): void {
    this.selectedPatient.set(patient);
    this.patientSearchTerm = `${patient.mrn} ${patient.firstName} ${patient.lastName}`;
    this.patientSearchError.set('');
  }

  clearSelectedPatient(): void {
    this.selectedPatient.set(undefined);
  }

  create(): void {
    if (!this.permissions.canCreateVobRequest()) {
      return;
    }

    const patient = this.selectedPatient();

    if (!patient) {
      this.patientSearchError.set('Select a patient before creating the VOB request.');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    handleAction(
      this.vobService.create({
        patientId: patient.id,
        dateOfService: value.dateOfService,
        priority: value.priority,
        insurancePolicies: [
          {
            payerName: value.payerName,
            memberId: value.memberId,
            groupNumber: value.groupNumber,
            planType: value.planType as 'PPO',
            insuranceOrder: 'PRIMARY',
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
      }),
      { loading: this.saving, error: this.error },
      (request) => {
        this.modalOpen.set(false);
        this.router.navigate(['/vobs', request.id]);
      },
    );
  }

  private watchPatientQueryParam(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const patientId = params.get('patientId');

      if (!patientId || !this.permissions.canCreateVobRequest()) {
        return;
      }

      this.openCreateModal();
      handleRequest(
        this.patientService.get(patientId),
        { loading: this.patientSearchLoading, error: this.patientSearchError },
        (patient) => this.selectPatient(patient),
      );
    });
  }
}
