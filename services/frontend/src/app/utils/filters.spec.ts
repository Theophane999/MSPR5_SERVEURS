import { filterLots, filterExpeditions } from './filters';

describe('filters utils', () => {
  it('filters lots by status and query', () => {
    const lots = [
      { id: 'L1', storageDate: '2026-07-01', status: 'ok', variete: 'Arabica' },
      { id: 'L2', storageDate: '2026-06-01', status: 'critical', variete: 'Robusta' },
      { id: 'L3', storageDate: '2026-05-01', status: 'warning', variete: 'Arabica' }
    ] as any;

    const filtered = filterLots(lots, { status: 'critical', query: '' });
    expect(filtered.length).toBe(1);
    expect(filtered[0].id).toBe('L2');

    const filtered2 = filterLots(lots, { status: 'all', query: 'arabica' });
    expect(filtered2.length).toBe(2);
  });

  it('filters expeditions by status and query', () => {
    const exps = [
      { id: 'E1', statut: 'livree', destinationPays: 'France', destinationVille: 'Paris', destinationClient: 'Client A', departAt: '2026-06-01' },
      { id: 'E2', statut: 'en_transit', destinationPays: 'Spain', destinationVille: 'Bilbao', destinationClient: 'Client B', departAt: '2026-05-01' }
    ] as any;

    const f = filterExpeditions(exps, { status: 'en_transit' });
    expect(f.length).toBe(1);
    expect(f[0].id).toBe('E2');

    const f2 = filterExpeditions(exps, { query: 'paris' });
    expect(f2.length).toBe(1);
    expect(f2[0].id).toBe('E1');

    const f3 = filterExpeditions(exps, { fromDate: '2026-05-15', toDate: '2026-06-30' });
    expect(f3.length).toBe(1);
    expect(f3[0].id).toBe('E1');
  });
});
