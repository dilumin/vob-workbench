import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthResponse, Role, User } from '../models/api.types';
import { API_BASE_URL } from './api-base-url';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly storageKey = 'vob-workbench-session';
  private readonly sessionState = signal<StoredSession | null>(this.readSession());
  private readonly userState = signal<User | null>(null);

  readonly session = this.sessionState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isLoggedIn = computed(() => !!this.sessionState()?.accessToken);
  readonly displayName = computed(() => this.userState()?.fullName || this.sessionState()?.username || 'Guest');

  login(username: string, password: string) {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, { username, password }).pipe(
      tap((response) => {
        this.saveSession({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          username: response.username,
          role: response.roles[0],
        });
      }),
    );
  }

  loadCurrentUser() {
    return this.http.get<User>(`${this.baseUrl}/auth/me`).pipe(tap((user) => this.userState.set(user)));
  }

  refresh() {
    const refreshToken = this.sessionState()?.refreshToken;
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/refresh`, { refreshToken }).pipe(
      tap((response) => {
        this.saveSession({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          username: response.username,
          role: response.roles[0],
        });
      }),
    );
  }

  logout() {
    const refreshToken = this.sessionState()?.refreshToken;
    this.clearSession();

    if (!refreshToken) {
      return;
    }

    this.http.post(`${this.baseUrl}/auth/logout`, { refreshToken }).subscribe();
  }

  hasRole(allowedRoles: Role[]): boolean {
    const sessionRole = this.sessionState()?.role;
    return !!sessionRole && allowedRoles.includes(sessionRole);
  }

  accessToken(): string {
    return this.sessionState()?.accessToken || '';
  }

  clearSession(): void {
    this.sessionState.set(null);
    this.userState.set(null);
    localStorage.removeItem(this.storageKey);
  }

  private saveSession(session: StoredSession): void {
    this.sessionState.set(session);
    localStorage.setItem(this.storageKey, JSON.stringify(session));
  }

  private readSession(): StoredSession | null {
    const raw = localStorage.getItem(this.storageKey);

    if (!raw) {
      return null;
    }

    try {
      const session = JSON.parse(raw) as StoredSession & { roles?: Role[] };
      if (!session.role && session.roles?.length) {
        return { ...session, role: session.roles[0] };
      }
      return session;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}
