import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardResponse } from '../models/dashboard.model';

const FALLBACK_URL = '/api';
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

export interface ExpeditionLotUpsertPayload {
  lotId?: number;
  quantiteExpediee?: number;
}

export interface ExpeditionUpsertPayload {
  departAt?: string;
  arriveeEstimeeAt?: string;
  destinationPays?: string;
  destinationVille?: string;
  destinationClient?: string;
  poidsTotalKg?: number;
  trackingTransporteur?: string;
  quaiDepart?: string;
  transporteur?: string;
  livreurNom?: string;
  livreurTelephone?: string;
  statut?: string;
  lots?: ExpeditionLotUpsertPayload[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = ((window as Window & { __env?: { backendMotherUrl?: string } }).__env?.backendMotherUrl ?? FALLBACK_URL).replace(/\/$/, '');

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
    return this.http.get<DashboardResponse>(this.dashboardUrl());
  }

  dashboardUrl() {
    return this.baseUrl.endsWith('/api') ? `${this.baseUrl}/children` : `${this.baseUrl}/api/children`;
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

  createExpedition(childBaseUrl: string, payload: ExpeditionUpsertPayload) {
    return this.http.post(`${this.resolveChildBaseUrl(childBaseUrl)}/api/expeditions`, payload);
  }

  updateExpedition(childBaseUrl: string, expeditionId: string, payload: ExpeditionUpsertPayload) {
    return this.http.put(`${this.resolveChildBaseUrl(childBaseUrl)}/api/expeditions/${encodeURIComponent(expeditionId)}`, payload);
  }

  deleteExpedition(childBaseUrl: string, expeditionId: string) {
    return this.http.delete(`${this.resolveChildBaseUrl(childBaseUrl)}/api/expeditions/${encodeURIComponent(expeditionId)}`);
  }

  motherUrl() {
    return this.baseUrl;
  }
}
