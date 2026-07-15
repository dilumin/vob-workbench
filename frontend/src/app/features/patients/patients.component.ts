import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PageInfo, Patient } from '../../core/models/api.types';
import { PatientService } from '../../core/services/patient.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { handleAction, handleRequest } from '../../core/utils/request-state';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-patients',
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    ModalComponent,
    EmptyStateComponent,
    LoadingIndicatorComponent,
    PaginationComponent,
  ],
  templateUrl: './patients.component.html',
})
export class PatientsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  readonly permissions = inject(PermissionsService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly patients = signal<Patient[]>([]);
  readonly pageInfo = signal<PageInfo | undefined>(undefined);
  readonly editingPatient = signal<Patient | undefined>(undefined);
  readonly modalOpen = signal(false);
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  searchTerm = '';

  readonly form = this.fb.nonNullable.group({
    mrn: ['', Validators.required],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    dateOfBirth: ['', Validators.required],
    gender: [''],
    phone: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(page = this.currentPage()): void {
    this.currentPage.set(page);
    handleRequest(this.patientService.search(this.searchTerm, page, this.pageSize()), this, (result) => {
      this.patients.set(result.content);
      this.pageInfo.set(result.page);
    });
  }

  search(): void {
    this.load(0);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.load(0);
  }

  openCreateModal(): void {
    if (!this.permissions.canCreatePatient()) {
      return;
    }

    this.editingPatient.set(undefined);
    this.form.reset({
      mrn: `MRN-${Date.now().toString().slice(-6)}`,
      firstName: '',
      lastName: '',
      dateOfBirth: '',
      gender: '',
      phone: '',
    });
    this.form.controls.mrn.enable();
    this.modalOpen.set(true);
  }

  openEditModal(patient: Patient): void {
    if (!this.permissions.canEditPatient()) {
      return;
    }

    this.editingPatient.set(patient);
    this.form.reset({
      mrn: patient.mrn,
      firstName: patient.firstName,
      lastName: patient.lastName,
      dateOfBirth: patient.dateOfBirth,
      gender: patient.gender || '',
      phone: patient.phone || '',
    });
    this.form.controls.mrn.disable();
    this.modalOpen.set(true);
  }

  save(): void {
    const editingPatient = this.editingPatient();

    if (editingPatient && !this.permissions.canEditPatient()) {
      return;
    }

    if (!editingPatient && !this.permissions.canCreatePatient()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request = editingPatient
      ? this.patientService.patch(editingPatient.id, value)
      : this.patientService.create(value);

    handleAction(request, { loading: this.saving, error: this.error }, () => {
      this.modalOpen.set(false);
      this.load();
    });
  }
}
