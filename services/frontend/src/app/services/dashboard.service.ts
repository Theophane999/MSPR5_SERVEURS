import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardResponse } from '../models/dashboard.model';

const FALLBACK_URL = '/api';

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

  loadDashboard() {
    return this.http.get<DashboardResponse>(this.dashboardUrl());
  }

  dashboardUrl() {
    return this.baseUrl.endsWith('/api') ? `${this.baseUrl}/children` : `${this.baseUrl}/api/children`;
  }

  createLot(childName: string, payload: LotUpsertPayload) {
    return this.http.post(`${this.motherChildrenUrl()}/${encodeURIComponent(childName)}/lots`, payload);
  }

  updateLot(childName: string, lotId: string, payload: LotUpsertPayload) {
    return this.http.put(`${this.motherChildrenUrl()}/${encodeURIComponent(childName)}/lots/${encodeURIComponent(lotId)}`, payload);
  }

  deleteLot(childName: string, lotId: string) {
    return this.http.delete(`${this.motherChildrenUrl()}/${encodeURIComponent(childName)}/lots/${encodeURIComponent(lotId)}`);
  }

  createExpedition(childName: string, payload: ExpeditionUpsertPayload) {
    return this.http.post(`${this.motherChildrenUrl()}/${encodeURIComponent(childName)}/expeditions`, payload);
  }

  updateExpedition(childName: string, expeditionId: string, payload: ExpeditionUpsertPayload) {
    return this.http.put(`${this.motherChildrenUrl()}/${encodeURIComponent(childName)}/expeditions/${encodeURIComponent(expeditionId)}`, payload);
  }

  deleteExpedition(childName: string, expeditionId: string) {
    return this.http.delete(`${this.motherChildrenUrl()}/${encodeURIComponent(childName)}/expeditions/${encodeURIComponent(expeditionId)}`);
  }

  private motherChildrenUrl() {
    return this.baseUrl.endsWith('/api') ? `${this.baseUrl}/children` : `${this.baseUrl}/api/children`;
  }

  motherUrl() {
    return this.baseUrl;
  }
}
