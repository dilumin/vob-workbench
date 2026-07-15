import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuditEventPage } from '../models/api.types';
import { API_BASE_URL } from './api-base-url';

export interface AuditFilters {
  actorUsername?: string;
  action?: string;
  entityType?: string;
  entityId?: string;
  patientId?: string;
  vobRequestId?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  search(filters: AuditFilters) {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20)
      .set('sortBy', 'timestamp')
      .set('sortDirection', 'desc');

    Object.entries(filters).forEach(([key, value]) => {
      if (key === 'page' || key === 'size') {
        return;
      }
      if (value !== undefined && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.http.get<AuditEventPage>(`${this.baseUrl}/audit-events`, { params });
  }
}
