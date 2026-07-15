import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { DashboardSummary, MockData, PageResponse, User, UserCreate } from '../models/api.types';
import { API_BASE_URL } from './api-base-url';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  dashboard() {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/dashboard/summary`);
  }

  users(page = 0, size = 20) {
    return this.http.get<PageResponse<User>>(`${this.baseUrl}/admin/users`, {
      params: { page, size },
    });
  }

  createUser(request: UserCreate) {
    return this.http.post<User>(`${this.baseUrl}/admin/users`, request);
  }

  mockData() {
    return this.http.get<MockData>(`${this.baseUrl}/admin/mock-data`);
  }

  saveMockData(request: MockData) {
    return this.http.put<MockData>(`${this.baseUrl}/admin/mock-data`, request);
  }
}
