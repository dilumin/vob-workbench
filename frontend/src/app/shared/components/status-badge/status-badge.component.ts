import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="tone">{{ value }}</span>`,
  styles: `
    .badge {
      border-radius: 999px;
      display: inline-flex;
      font-size: 0.75rem;
      font-weight: 700;
      line-height: 1;
      padding: 6px 9px;
      white-space: nowrap;
    }

    .neutral {
      background: #e2e8f0;
      color: #334155;
    }

    .warning {
      background: #fef3c7;
      color: #92400e;
    }

    .active {
      background: #dbeafe;
      color: #1d4ed8;
    }

    .success {
      background: #dcfce7;
      color: #166534;
    }

    .danger {
      background: #fee2e2;
      color: #991b1b;
    }
  `,
})
export class StatusBadgeComponent {
  @Input({ required: true }) value = '';

  get tone(): string {
    if (['VERIFIED', 'COMPLETED', 'true'].includes(this.value)) {
      return 'success';
    }

    if (['URGENT', 'NEEDS_INFO', 'SPECIALIST_REVIEW'].includes(this.value)) {
      return 'warning';
    }

    if (['IN_PROGRESS', 'REOPENED'].includes(this.value)) {
      return 'active';
    }

    if (['UNABLE_TO_VERIFY', 'CANCELLED', 'false'].includes(this.value)) {
      return 'danger';
    }

    return 'neutral';
  }
}
