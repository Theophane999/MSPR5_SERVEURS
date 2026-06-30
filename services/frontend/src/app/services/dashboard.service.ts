import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardResponse } from '../models/dashboard.model';

const FALLBACK_URL = 'http://localhost:3200';

export interface LotUpsertPayload {
  lotReference?: string;
  datePeremption?: number;
  variete?: string;
  process?: string;
  scoreSca?: number;
  poidsKg?: number;
  qualite?: string;
  quantite?: number;
  storageDate?: string;
  idExploitation?: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = ((window as Window & { __env?: { backendMotherUrl?: string } }).__env?.backendMotherUrl ?? FALLBACK_URL).replace(/\/$/, '');

  loadDashboard() {
    return this.http.get<DashboardResponse>(`${this.baseUrl}/api/children`);
  }

  createLot(childBaseUrl: string, payload: LotUpsertPayload) {
    return this.http.post(`${childBaseUrl.replace(/\/$/, '')}/api/lots`, payload);
  }

  updateLot(childBaseUrl: string, lotId: string, payload: LotUpsertPayload) {
    return this.http.put(`${childBaseUrl.replace(/\/$/, '')}/api/lots/${encodeURIComponent(lotId)}`, payload);
  }

  deleteLot(childBaseUrl: string, lotId: string) {
    return this.http.delete(`${childBaseUrl.replace(/\/$/, '')}/api/lots/${encodeURIComponent(lotId)}`);
  }

  motherUrl() {
    return this.baseUrl;
  }
}
