import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardResponse } from '../models/dashboard.model';

const FALLBACK_URL = 'http://localhost:3200';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = ((window as Window & { __env?: { backendMotherUrl?: string } }).__env?.backendMotherUrl ?? FALLBACK_URL).replace(/\/$/, '');

  loadDashboard() {
    return this.http.get<DashboardResponse>(`${this.baseUrl}/api/children`);
  }

  motherUrl() {
    return this.baseUrl;
  }
}
