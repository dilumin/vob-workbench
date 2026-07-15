import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { PageResponse, Patient, PatientPatch, PatientWrite } from '../models/api.types';
import { API_BASE_URL } from './api-base-url';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  search(q = '', page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size).set('q', q);
    return this.http.get<PageResponse<Patient>>(`${this.baseUrl}/patients`, { params });
  }

  get(id: string) {
    return this.http.get<Patient>(`${this.baseUrl}/patients/${id}`);
  }

  create(request: PatientWrite) {
    return this.http.post<Patient>(`${this.baseUrl}/patients`, request);
  }

  patch(id: string, request: PatientPatch) {
    return this.http.patch<Patient>(`${this.baseUrl}/patients/${id}`, request);
  }
}
