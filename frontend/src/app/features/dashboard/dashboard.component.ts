import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DashboardSummary, VobRequest } from '../../core/models/api.types';
import { AdminService } from '../../core/services/admin.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { VobRequestService } from '../../core/services/vob-request.service';
import { handleRequest } from '../../core/utils/request-state';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, CurrencyPipe, RouterLink, PageHeaderComponent, StatusBadgeComponent, EmptyStateComponent, LoadingIndicatorComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly vobService = inject(VobRequestService);
  readonly permissions = inject(PermissionsService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly summary = signal<DashboardSummary | undefined>(undefined);
  readonly urgentRequests = signal<VobRequest[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    handleRequest(
      forkJoin({
        summary: this.adminService.dashboard(),
        worklist: this.vobService.search({ q: '', status: '', assignedTo: '' }),
      }),
      this,
      ({ summary, worklist }) => {
        this.summary.set(summary);
        this.urgentRequests.set(worklist.content.filter((request) => request.priority === 'URGENT').slice(0, 5));
      },
    );
  }
}
