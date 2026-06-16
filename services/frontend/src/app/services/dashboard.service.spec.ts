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

    const request = http.expectOne('http://example.test:3200/api/children');
    expect(request.request.method).toBe('GET');
    request.flush({ role: 'mother', aggregatedAt: '2026-01-01T00:00:00Z', children: [] });
    http.verify();
  });
});