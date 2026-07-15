import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  AuditEvent,
  CoverageSummary,
  PageResponse,
  VobRequest,
  VobRequestCreate,
  VobRequestPatch,
  VobStatus,
} from '../models/api.types';
import { API_BASE_URL } from './api-base-url';

export interface VobRequestFilters {
  status?: VobStatus | '';
  assignedTo?: string;
  q?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class VobRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  search(filters: VobRequestFilters) {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.http.get<PageResponse<VobRequest>>(`${this.baseUrl}/vob-requests`, { params });
  }

  create(request: VobRequestCreate) {
    return this.http.post<VobRequest>(`${this.baseUrl}/vob-requests`, this.cleanDeep(request));
  }

  get(id: string) {
    return this.http.get<VobRequest>(`${this.baseUrl}/vob-requests/${id}`);
  }

  patch(id: string, request: VobRequestPatch) {
    return this.http.patch<VobRequest>(`${this.baseUrl}/vob-requests/${id}`, this.cleanDeep(request));
  }

  runEligibility(id: string) {
    return this.http.post<VobRequest>(`${this.baseUrl}/vob-requests/${id}/eligibility-check`, {});
  }

  calculateCoverage(id: string) {
    return this.http.post<CoverageSummary>(`${this.baseUrl}/vob-requests/${id}/calculate-coverage`, {});
  }

  verify(id: string) {
    return this.http.post<VobRequest>(`${this.baseUrl}/vob-requests/${id}/verify`, {});
  }

  reopen(id: string) {
    return this.http.post<VobRequest>(`${this.baseUrl}/vob-requests/${id}/reopen`, {});
  }

  audit(id: string) {
    return this.http.get<AuditEvent[]>(`${this.baseUrl}/vob-requests/${id}/audit`);
  }

  private cleanDeep<T>(value: T): T {
    if (Array.isArray(value)) {
      return value.map((item) => this.cleanDeep(item)) as T;
    }

    if (value && typeof value === 'object') {
      return Object.fromEntries(
        Object.entries(value)
          .filter(([, fieldValue]) => fieldValue !== '')
          .map(([key, fieldValue]) => [key, this.cleanDeep(fieldValue)]),
      ) as T;
    }

    return value;
  }
}
