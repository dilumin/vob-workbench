import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditEventPage } from '../../core/models/api.types';
import { AuditService } from '../../core/services/audit.service';
import { handleRequest } from '../../core/utils/request-state';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-audit',
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    PageHeaderComponent,
    PaginationComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    LoadingIndicatorComponent,
  ],
  templateUrl: './audit.component.html',
})
export class AuditComponent implements OnInit {
  private readonly auditService = inject(AuditService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly result = signal<AuditEventPage | undefined>(undefined);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  filters = {
    actorUsername: '',
    action: '',
    entityType: '',
    entityId: '',
    patientId: '',
    vobRequestId: '',
  };

  ngOnInit(): void {
    this.search();
  }

  search(page = this.page()): void {
    this.page.set(page);
    handleRequest(
      this.auditService.search({ ...this.filters, page, size: this.pageSize() }),
      this,
      (result) => this.result.set(result),
    );
  }

  applyFilters(): void {
    this.search(0);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.search(0);
  }

  clear(): void {
    this.filters = {
      actorUsername: '',
      action: '',
      entityType: '',
      entityId: '',
      patientId: '',
      vobRequestId: '',
    };
    this.search(0);
  }

  metadataText(metadata: Record<string, unknown> | undefined): string {
    if (!metadata || !Object.keys(metadata).length) {
      return '-';
    }
    return Object.entries(metadata)
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join(', ');
  }
}
