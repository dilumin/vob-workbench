import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/services/auth.service';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator.component';

@Component({
  selector: 'app-shell',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, LoadingIndicatorComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly auth = inject(AuthService);
  readonly navigating = signal(false);

  readonly navItems = [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Patients', route: '/patients' },
    { label: 'VOB Requests', route: '/vobs' },
    { label: 'Admin', route: '/admin', adminOnly: true },
    { label: 'Audit', route: '/audit', adminOnly: true },
  ];

  ngOnInit(): void {
    if (this.auth.isLoggedIn() && !this.auth.user()) {
      this.auth.loadCurrentUser().subscribe();
    }

    this.router.events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.navigating.set(true);
      }

      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError) {
        this.navigating.set(false);
      }
    });
  }

  canShow(adminOnly?: boolean): boolean {
    return !adminOnly || this.auth.hasRole(['SUPERVISOR_ADMIN']);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
