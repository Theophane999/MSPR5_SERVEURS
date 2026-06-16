import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { DashboardService } from './services/dashboard.service';
import { DashboardResponse } from './models/dashboard.model';

describe('AppComponent', () => {
  const dashboardResponse: DashboardResponse = {
    role: 'mother',
    aggregatedAt: '2026-01-01T00:00:00Z',
    children: [
      {
        name: 'brazil',
        url: 'http://localhost:3101/api/info',
        available: true,
        data: {
          role: 'child',
          country: 'Brazil',
          message: 'Backend child Brazil is online',
          timestamp: '2026-01-01T00:00:00Z',
        },
      },
      {
        name: 'ecuador',
        url: 'http://localhost:3102/api/info',
        available: false,
        error: 'timeout',
      },
    ],
  };

  function createComponentWith(serviceMock: Pick<DashboardService, 'loadDashboard' | 'motherUrl'>): ComponentFixture<AppComponent> {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: DashboardService, useValue: serviceMock }],
    });

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders cockpit metrics and regional cards from dashboard data', () => {
    const fixture = createComponentWith({
      loadDashboard: () => of(dashboardResponse),
      motherUrl: () => 'http://localhost:3200',
    });

    const component = fixture.componentInstance as AppComponent & {
      onlineCount: number;
      offlineCount: number;
      availabilityPercent: number;
    };

    expect(component.onlineCount).toBe(1);
    expect(component.offlineCount).toBe(1);
    expect(component.availabilityPercent).toBe(50);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Supervision degradee');
    expect(element.textContent).toContain('Brazil');
    expect(element.textContent).toContain('Ecuador');
    expect(element.textContent).toContain('ONLINE');
    expect(element.textContent).toContain('OFFLINE');
  });

  it('shows an error state when dashboard loading fails', () => {
    const fixture = createComponentWith({
      loadDashboard: () => throwError(() => new Error('mother down')),
      motherUrl: () => 'http://localhost:3200',
    });

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Erreur de chargement: mother down');
    expect(element.querySelector('h1')?.textContent).toContain('Perte de visibilite');
  });

  it('keeps the loading state visible while dashboard stream is pending', () => {
    const dashboardSubject = new Subject<DashboardResponse>();
    const fixture = createComponentWith({
      loadDashboard: () => dashboardSubject.asObservable(),
      motherUrl: () => 'http://localhost:3200',
    });

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Chargement des donnees...');

    dashboardSubject.complete();
  });
});