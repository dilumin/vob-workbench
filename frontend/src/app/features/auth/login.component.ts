import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { handleAction } from '../../core/utils/request-state';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, LoadingIndicatorComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    username: ['specialist1', Validators.required],
    password: ['password123', Validators.required],
  });

  readonly demoUsers = ['receptionist1', 'specialist1', 'admin1', 'viewer1'];

  chooseUser(username: string): void {
    this.form.patchValue({ username, password: 'password123' });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { username, password } = this.form.getRawValue();

    handleAction(this.auth.login(username, password), this, () => {
        this.router.navigateByUrl('/dashboard');
    });
  }
}
