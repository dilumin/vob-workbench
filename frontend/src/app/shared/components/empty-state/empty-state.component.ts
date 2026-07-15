import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  template: `
    <section class="empty-state">
      <strong>{{ title }}</strong>
      <p>{{ message }}</p>
    </section>
  `,
  styles: `
    .empty-state {
      align-items: center;
      border: 1px dashed #cbd5e1;
      border-radius: 8px;
      color: #64748b;
      display: grid;
      justify-items: center;
      min-height: 180px;
      padding: 28px;
      text-align: center;
    }

    strong {
      color: #0f172a;
      font-size: 1rem;
    }

    p {
      margin: 6px 0 0;
      max-width: 420px;
    }
  `,
})
export class EmptyStateComponent {
  @Input() title = 'Nothing here yet';
  @Input() message = 'Try adjusting filters or creating a new record.';
}
