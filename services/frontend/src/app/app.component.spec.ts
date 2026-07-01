import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { DashboardService } from './services/dashboard.service';
import { DashboardResponse } from './models/dashboard.model';
import { filterExpeditions, filterLots } from './utils/filters';

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
        expeditions: [
          { id: 'E1', statut: 'livree', destinationPays: 'France', destinationVille: 'Paris', destinationClient: 'Client A', departAt: '2026-06-01T08:00:00Z', arriveeEstimeeAt: '2026-06-02T08:00:00Z', poidsTotalKg: 100, trackingTransporteur: 'TRK-1', quaiDepart: 'Q1', transporteur: 'Translog', livreurNom: 'Ana', livreurTelephone: '010203', lots: [] },
          { id: 'E2', statut: 'en_transit', destinationPays: 'France', destinationVille: 'Lyon', destinationClient: 'Client B', departAt: '2026-06-02T08:00:00Z', arriveeEstimeeAt: '2026-06-03T08:00:00Z', poidsTotalKg: 120, trackingTransporteur: 'TRK-2', quaiDepart: 'Q2', transporteur: 'Translog', livreurNom: 'Ben', livreurTelephone: '010204', lots: [] },
          { id: 'E3', statut: 'en_transit', destinationPays: 'Spain', destinationVille: 'Madrid', destinationClient: 'Client C', departAt: '2026-06-03T08:00:00Z', arriveeEstimeeAt: '2026-06-04T08:00:00Z', poidsTotalKg: 150, trackingTransporteur: 'TRK-3', quaiDepart: 'Q3', transporteur: 'ShipIt', livreurNom: 'Caro', livreurTelephone: '010205', lots: [] },
        ],
      },
      {
        name: 'ecuador',
        url: 'http://localhost:3102/api/info',
        available: false,
        error: 'timeout',
      },
    ],
  };

  function createComponentWith(serviceMock: Pick<DashboardService, 'loadDashboard' | 'motherUrl' | 'dashboardUrl'>): ComponentFixture<AppComponent> {
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
      dashboardUrl: () => 'http://localhost:3200/api/children',
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
      dashboardUrl: () => 'http://localhost:3200/api/children',
    });

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Erreur de chargement : mother down');
    expect(element.querySelector('h1')?.textContent).toContain('Perte de visibilite');
  });

  it('keeps the loading state visible while dashboard stream is pending', () => {
    const dashboardSubject = new Subject<DashboardResponse>();
    const fixture = createComponentWith({
      loadDashboard: () => dashboardSubject.asObservable(),
      motherUrl: () => 'http://localhost:3200',
      dashboardUrl: () => 'http://localhost:3200/api/children',
    });

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Chargement des données...');

    dashboardSubject.complete();
  });

  it('filters lots and expeditions with the shared helper utilities', () => {
    const lots = [
      { id: 'L1', lotReference: 'BR-001', storageDate: '2026-07-01', status: 'ok', variete: 'Arabica', process: 'Lave' },
      { id: 'L2', storageDate: '2026-06-01', status: 'critical', variete: 'Robusta' },
      { id: 'L3', storageDate: '2026-05-01', status: 'warning', variete: 'Arabica' },
    ] as any;

    const filteredLots = filterLots(lots, { status: 'critical', query: '' });
    expect(filteredLots.length).toBe(1);
    expect(filteredLots[0].id).toBe('L2');

    const filteredLotsByQuery = filterLots(lots, { status: 'all', query: 'br-001' });
    expect(filteredLotsByQuery.length).toBe(1);
    expect(filteredLotsByQuery[0].id).toBe('L1');

    const sortedLots = filterLots(
      [
        { id: 'L1', lotReference: 'BR-003', storageDate: '2026-07-01', status: 'ok', variete: 'C' },
        { id: 'L2', lotReference: 'BR-001', storageDate: '2026-06-01', status: 'ok', variete: 'A' },
        { id: 'L3', lotReference: 'BR-002', storageDate: '2026-05-01', status: 'ok', variete: 'B' },
      ] as any,
      { sortBy: 'lotReference', sortOrder: 'asc' },
    );
    expect(sortedLots.map((lot) => lot.id)).toEqual(['L2', 'L3', 'L1']);

    const sortedLotsByStatus = filterLots(
      [
        { id: 'L1', storageDate: '2026-07-01', status: 'warning', variete: 'C' },
        { id: 'L2', storageDate: '2026-06-01', status: 'critical', variete: 'A' },
        { id: 'L3', storageDate: '2026-05-01', status: 'ok', variete: 'B' },
      ] as any,
      { sortBy: 'status', sortOrder: 'asc' },
    );
    expect(sortedLotsByStatus.map((lot) => lot.status)).toEqual(['critical', 'ok', 'warning']);

    const expeditions = [
      { id: 'E1', statut: 'livree', destinationPays: 'France', destinationVille: 'Paris', destinationClient: 'Client A', departAt: '2026-06-01' },
      { id: 'E2', statut: 'en_transit', destinationPays: 'Spain', destinationVille: 'Bilbao', destinationClient: 'Client B', departAt: '2026-05-01' },
    ] as any;

    const filteredExpeditions = filterExpeditions(expeditions, { status: 'en_transit' });
    expect(filteredExpeditions.length).toBe(1);
    expect(filteredExpeditions[0].id).toBe('E2');

    const filteredExpeditionsByQuery = filterExpeditions(expeditions, { query: 'client b' });
    expect(filteredExpeditionsByQuery.length).toBe(1);
    expect(filteredExpeditionsByQuery[0].id).toBe('E2');

    const sortedExpeditions = filterExpeditions(
      [
        { id: 'E1', statut: 'livree', destinationPays: 'France', destinationVille: 'Paris', destinationClient: 'Client C', departAt: '2026-06-01' },
        { id: 'E2', statut: 'en_transit', destinationPays: 'Spain', destinationVille: 'Bilbao', destinationClient: 'Client A', departAt: '2026-05-01' },
        { id: 'E3', statut: 'annulee', destinationPays: 'Brazil', destinationVille: 'Recife', destinationClient: 'Client B', departAt: '2026-07-01' },
      ] as any,
      { sortBy: 'destinationClient', sortOrder: 'asc' },
    );
    expect(sortedExpeditions.map((expedition) => expedition.id)).toEqual(['E2', 'E3', 'E1']);

    const sortedExpeditionsByDate = filterExpeditions(
      [
        { id: 'E1', statut: 'livree', destinationPays: 'France', destinationVille: 'Paris', destinationClient: 'Client C', departAt: '2026-06-01' },
        { id: 'E2', statut: 'en_transit', destinationPays: 'Spain', destinationVille: 'Bilbao', destinationClient: 'Client A', departAt: '2026-05-01' },
        { id: 'E3', statut: 'annulee', destinationPays: 'Brazil', destinationVille: 'Recife', destinationClient: 'Client B', departAt: '2026-07-01' },
      ] as any,
      { sortBy: 'departAt', sortOrder: 'desc' },
    );
    expect(sortedExpeditionsByDate.map((expedition) => expedition.id)).toEqual(['E3', 'E1', 'E2']);
  });

  it('paginates expeditions and opens inline editing from the list', () => {
    const fixture = createComponentWith({
      loadDashboard: () => of(dashboardResponse),
      motherUrl: () => 'http://localhost:3200',
      dashboardUrl: () => 'http://localhost:3200/api/children',
    });

    const component = fixture.componentInstance as AppComponent & {
      expeditionPageSize: number;
      expeditionPageIndex: number;
      paginatedSelectedCountryExpeditions: any[];
      expeditionInlineEditId?: string;
      expeditionInlineForm: { destinationClient: string };
      startInlineExpeditionEdit: (expedition: any) => void;
    };

    component.expeditionPageSize = 1;
    component.expeditionPageIndex = 1;

    expect(component.paginatedSelectedCountryExpeditions.map((expedition) => expedition.id)).toEqual(['E2']);

    component.startInlineExpeditionEdit(component.paginatedSelectedCountryExpeditions[0]);

    expect(component.expeditionInlineEditId).toBe('E2');
    expect(component.expeditionInlineForm.destinationClient).toBe('Client B');
  });
});