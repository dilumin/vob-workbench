import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MockData, PageInfo, Role, User } from '../../core/models/api.types';
import { AdminService } from '../../core/services/admin.service';
import { handleAction, handleRequest } from '../../core/utils/request-state';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-admin',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    ModalComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    LoadingIndicatorComponent,
    PaginationComponent,
  ],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  readonly roles: Role[] = ['RECEPTIONIST', 'SPECIALIST', 'SUPERVISOR_ADMIN', 'VIEWER'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly loadingMockData = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly users = signal<User[]>([]);
  readonly userPageInfo = signal<PageInfo | undefined>(undefined);
  readonly userPage = signal(0);
  readonly userPageSize = signal(20);
  readonly userModalOpen = signal(false);
  readonly mockModalOpen = signal(false);
  mockJson = '';

  readonly userForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['password123', Validators.required],
    fullName: ['', Validators.required],
    role: ['VIEWER' as Role, Validators.required],
  });

  ngOnInit(): void {
    this.load();
  }

  load(page = this.userPage()): void {
    this.userPage.set(page);
    handleRequest(this.adminService.users(page, this.userPageSize()), this, (result) => {
      this.users.set(result.content);
      this.userPageInfo.set(result.page);
    });
  }

  changeUserPageSize(size: number): void {
    this.userPageSize.set(size);
    this.load(0);
  }

  openUserModal(): void {
    this.userForm.reset({
      username: '',
      password: 'password123',
      fullName: '',
      role: 'VIEWER',
    });
    this.userModalOpen.set(true);
  }

  createUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    handleAction(
      this.adminService.createUser(this.userForm.getRawValue()),
      { loading: this.saving, error: this.error },
      (user) => {
        this.message.set(`Created ${user.username}.`);
        this.userModalOpen.set(false);
        this.load();
      },
    );
  }

  loadMockData(): void {
    handleRequest(this.adminService.mockData(), { loading: this.loadingMockData, error: this.error }, (mockData) => {
      this.mockJson = JSON.stringify(mockData, null, 2);
      this.mockModalOpen.set(true);
    });
  }

  saveMockData(): void {
    let parsed: MockData;

    try {
      parsed = JSON.parse(this.mockJson) as MockData;
    } catch {
      this.error.set('Mock data must be valid JSON.');
      return;
    }

    handleAction(this.adminService.saveMockData(parsed), { loading: this.saving, error: this.error }, (mockData) => {
      this.mockJson = JSON.stringify(mockData, null, 2);
      this.message.set('Mock data saved.');
    });
  }
}
