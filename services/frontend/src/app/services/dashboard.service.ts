import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardResponse } from '../models/dashboard.model';

const FALLBACK_URL = 'http://localhost:3200';
const CHILD_PUBLIC_PORTS: Record<string, number> = {
  'backend-brazil': 3101,
  'backend-ecuador': 3102,
  'backend-colombia': 3103,
};

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
  private readonly configService = inject(ConfigService);

  private resolveChildBaseUrl(childBaseUrl: string) {
    const sanitizedUrl = childBaseUrl.replace(/\/$/, '');

    try {
      const parsedUrl = new URL(sanitizedUrl);
      const mappedPort = CHILD_PUBLIC_PORTS[parsedUrl.hostname];

      if (!mappedPort) {
        return sanitizedUrl;
      }

      return `${parsedUrl.protocol}//localhost:${mappedPort}`;
    } catch {
      return sanitizedUrl;
    }
  }

  loadDashboard() {
    const backendUrl = this.configService.backendMotherUrl;
    const url = `${backendUrl}/api/children`;
    return this.http.get<DashboardResponse>(url);
  }

  createLot(childBaseUrl: string, payload: LotUpsertPayload) {
    return this.http.post(`${this.resolveChildBaseUrl(childBaseUrl)}/api/lots`, payload);
  }

  updateLot(childBaseUrl: string, lotId: string, payload: LotUpsertPayload) {
    return this.http.put(`${this.resolveChildBaseUrl(childBaseUrl)}/api/lots/${encodeURIComponent(lotId)}`, payload);
  }

  deleteLot(childBaseUrl: string, lotId: string) {
    return this.http.delete(`${this.resolveChildBaseUrl(childBaseUrl)}/api/lots/${encodeURIComponent(lotId)}`);
  }

  motherUrl() {
    return this.configService.backendMotherUrl;
  }
}
