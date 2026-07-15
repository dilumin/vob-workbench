import { Component, Input } from '@angular/core';

type LoadingMode = 'inline' | 'block' | 'page' | 'table';

@Component({
  selector: 'app-loading-indicator',
  template: `
    @if (mode === 'table') {
      <div class="table-skeleton" role="status" aria-live="polite">
        <div class="loading-copy">
          <span class="spinner" aria-hidden="true"></span>
          <span>{{ label }}</span>
        </div>
        @for (row of rowIndexes; track row) {
          <div class="skeleton-row" [style.--columns]="columns">
            @for (column of columnIndexes; track column) {
              <span></span>
            }
          </div>
        }
      </div>
    } @else {
      <span class="loading-indicator" [class.block]="mode === 'block'" [class.page]="mode === 'page'" role="status" aria-live="polite">
        <span class="spinner" aria-hidden="true"></span>
        <span>{{ label }}</span>
      </span>
    }
  `,
  styles: `
    :host {
      display: contents;
    }

    .loading-indicator,
    .loading-copy {
      align-items: center;
      color: inherit;
      display: inline-flex;
      gap: 8px;
      line-height: 1;
    }

    .loading-indicator.block,
    .loading-indicator.page {
      background: #f8fafc;
      border: 1px solid var(--border);
      border-radius: 8px;
      color: var(--muted);
      font-weight: 700;
      justify-content: center;
      min-height: 86px;
      padding: 18px;
      width: 100%;
    }

    .loading-indicator.page {
      min-height: 240px;
    }

    .spinner {
      animation: spin 0.75s linear infinite;
      border: 2px solid currentColor;
      border-right-color: transparent;
      border-radius: 999px;
      display: inline-block;
      flex: 0 0 auto;
      height: 1em;
      opacity: 0.8;
      width: 1em;
    }

    .table-skeleton {
      background: #f8fafc;
      border: 1px solid var(--border);
      border-radius: 8px;
      display: grid;
      gap: 10px;
      padding: 14px;
    }

    .table-skeleton .loading-copy {
      color: var(--muted);
      font-weight: 700;
      margin-bottom: 2px;
    }

    .skeleton-row {
      display: grid;
      gap: 12px;
      grid-template-columns: repeat(var(--columns), minmax(54px, 1fr));
    }

    .skeleton-row span {
      animation: pulse 1.25s ease-in-out infinite;
      background: linear-gradient(90deg, #e2e8f0, #f8fafc, #e2e8f0);
      background-size: 220% 100%;
      border-radius: 6px;
      min-height: 34px;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @keyframes pulse {
      0% {
        background-position: 100% 0;
      }

      100% {
        background-position: -100% 0;
      }
    }

    @media (max-width: 720px) {
      .skeleton-row {
        grid-template-columns: repeat(2, minmax(72px, 1fr));
      }
    }
  `,
})
export class LoadingIndicatorComponent {
  @Input() label = 'Loading...';
  @Input() mode: LoadingMode = 'inline';
  @Input() rows = 4;
  @Input() columns = 4;

  get rowIndexes(): number[] {
    return Array.from({ length: this.rows }, (_, index) => index);
  }

  get columnIndexes(): number[] {
    return Array.from({ length: this.columns }, (_, index) => index);
  }
}
