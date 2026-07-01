import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let originalEnv: { backendMotherUrl?: string } | undefined;

  beforeEach(() => {
    originalEnv = (window as Window & { __env?: { backendMotherUrl?: string } }).__env;
  });

  afterEach(() => {
    (window as Window & { __env?: { backendMotherUrl?: string } }).__env = originalEnv;
  });

  it('loads the dashboard from the configured backend URL', () => {
    (window as Window & { __env?: { backendMotherUrl?: string } }).__env = {
      backendMotherUrl: 'http://example.test:3200/',
    };

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DashboardService],
    });

    const service = TestBed.inject(DashboardService);
    const http = TestBed.inject(HttpTestingController);

    service.loadDashboard().subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/api/children'));
    expect(request.request.method).toBe('GET');
    request.flush({ role: 'mother', aggregatedAt: '2026-01-01T00:00:00Z', children: [] });
    http.verify();
  });

  it('uses the dashboardUrl helper for the children endpoint', () => {
    (window as Window & { __env?: { backendMotherUrl?: string } }).__env = {
      backendMotherUrl: 'http://example.test:3200/',
    };

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DashboardService],
    });

    const service = TestBed.inject(DashboardService);
    expect(service.dashboardUrl()).toBe('http://example.test:3200/api/children');
  });

  it('sends expedition CRUD requests to the child backend', () => {
    (window as Window & { __env?: { backendMotherUrl?: string } }).__env = {
      backendMotherUrl: 'http://example.test:3200/',
    };

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DashboardService],
    });

    const service = TestBed.inject(DashboardService);
    const http = TestBed.inject(HttpTestingController);

    service.createExpedition('http://backend-brazil:3000', {
      departAt: '2026-07-01T06:30',
      destinationPays: 'France',
      destinationVille: 'Le Havre',
      destinationClient: 'Amazon France',
      poidsTotalKg: 480,
      livreurNom: 'Carlos Almeida',
      statut: 'Expédiée',
      lots: [{ lotId: 1, quantiteExpediee: 120 }],
    }).subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/api/expeditions'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body.lots.length).toBe(1);
    request.flush({ id: 99 });
    http.verify();
  });
});