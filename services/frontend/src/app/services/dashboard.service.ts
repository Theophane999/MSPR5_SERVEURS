import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardResponse } from '../models/dashboard.model';
import { ConfigService } from './config.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  loadDashboard() {
    const backendUrl = this.configService.backendMotherUrl;
    const url = `${backendUrl}/api/children`;
    return this.http.get<DashboardResponse>(url);
  }

  motherUrl() {
    return this.configService.backendMotherUrl;
  }
}
