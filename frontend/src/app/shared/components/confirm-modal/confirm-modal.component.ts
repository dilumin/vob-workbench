import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ModalComponent } from '../modal/modal.component';

@Component({
  selector: 'app-confirm-modal',
  imports: [ModalComponent],
  templateUrl: './confirm-modal.component.html',
})
export class ConfirmModalComponent {
  @Input() open = false;
  @Input() title = 'Confirm action';
  @Input() message = 'Are you sure?';
  @Input() confirmLabel = 'Confirm';
  @Output() cancelled = new EventEmitter<void>();
  @Output() confirmed = new EventEmitter<void>();
}
