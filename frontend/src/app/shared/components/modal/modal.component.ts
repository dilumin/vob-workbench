import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-modal',
  imports: [CommonModule],
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent {
  @Input({ required: true }) title = '';
  @Input() open = false;
  @Input() size: 'medium' | 'large' = 'medium';
  @Output() closed = new EventEmitter<void>();
}
