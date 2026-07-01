import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface AppConfig {
  production: boolean;
  backendMotherUrl: string;
  apiTimeout: number;
  refreshInterval: number;
}

interface WindowWithEnv extends Window {
  __env?: Partial<AppConfig>;
}

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private config: AppConfig = {
    production: false,
    backendMotherUrl: '/api',
    apiTimeout: 30000,
    refreshInterval: 300
  };

  constructor(private http: HttpClient) {
    // Essaye de charger depuis window.__env (Docker)
    const windowEnv = (window as WindowWithEnv).__env;
    if (windowEnv?.backendMotherUrl) {
      this.config.backendMotherUrl = windowEnv.backendMotherUrl;
    }
  }

  async loadConfig(): Promise<AppConfig> {
    try {
      // Étape 1: Essayer de charger depuis env.json (développement)
      const jsonConfig = await firstValueFrom(this.http.get<Partial<AppConfig>>('/assets/config/env.json'));
      this.config = { ...this.config, ...jsonConfig };
    } catch {
      // Fallback sur window.__env ou config par défaut
      const windowEnv = (window as WindowWithEnv).__env;
      if (windowEnv?.backendMotherUrl) {
        this.config.backendMotherUrl = windowEnv.backendMotherUrl;
      }
    }

    return this.config;
  }

  getConfig(): AppConfig {
    return this.config;
  }

  get backendMotherUrl(): string {
    return this.config.backendMotherUrl;
  }

  get apiTimeout(): number {
    return this.config.apiTimeout;
  }

  get refreshInterval(): number {
    return this.config.refreshInterval;
  }
}
