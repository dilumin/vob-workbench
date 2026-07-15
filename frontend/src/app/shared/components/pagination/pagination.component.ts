import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-pagination',
  imports: [CommonModule, FormsModule],
  templateUrl: './pagination.component.html',
})
export class PaginationComponent {
  @Input() page = 0;
  @Input() size = 20;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Input() disabled = false;

  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  readonly pageSizeOptions = [10, 20, 50, 100];

  get firstItem(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get lastItem(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  previous(): void {
    if (this.page > 0 && !this.disabled) {
      this.pageChange.emit(this.page - 1);
    }
  }

  next(): void {
    if (this.page < this.totalPages - 1 && !this.disabled) {
      this.pageChange.emit(this.page + 1);
    }
  }

  updateSize(value: number | string): void {
    if (!this.disabled) {
      this.sizeChange.emit(Number(value));
    }
  }
}
