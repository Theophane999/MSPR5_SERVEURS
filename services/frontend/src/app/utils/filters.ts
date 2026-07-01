import { HistoryPoint, LotView, ExpeditionView } from '../models/dashboard.model';

export type LotFilter = {
  status?: 'all' | 'ok' | 'warning' | 'critical';
  query?: string; // search by variety or id
  fromDate?: string; // ISO date
  toDate?: string;   // ISO date
};

export type ExpeditionFilter = {
  status?: 'all' | string;
  query?: string; // search by client or destination
  fromDate?: string;
  toDate?: string;
};

export function filterLots(lots: LotView[] = [], filter: LotFilter = {}): LotView[] {
  let res = [...lots];
  if (filter.status && filter.status !== 'all') {
    res = res.filter(l => l.status === filter.status);
  }
  if (filter.query) {
    const q = filter.query.toLowerCase();
    res = res.filter(l => (l.variete ?? '').toLowerCase().includes(q) || l.id.toLowerCase().includes(q));
  }
  if (filter.fromDate) {
    const fd = filter.fromDate;
    res = res.filter(l => l.storageDate >= fd);
  }
  if (filter.toDate) {
    const td = filter.toDate;
    res = res.filter(l => l.storageDate <= td);
  }
  return res.sort((a, b) => b.storageDate.localeCompare(a.storageDate));
}

export function filterExpeditions(exps: ExpeditionView[] = [], filter: ExpeditionFilter = {}): ExpeditionView[] {
  let res = [...exps];
  if (filter.status && filter.status !== 'all') {
    const st = filter.status.toLowerCase();
    res = res.filter(e => (e.statut ?? '').toLowerCase() === st);
  }
  if (filter.query) {
    const q = filter.query.toLowerCase();
    res = res.filter(e => (e.destinationPays ?? '').toLowerCase().includes(q) || (e.destinationVille ?? '').toLowerCase().includes(q) || (e.destinationClient ?? '').toLowerCase().includes(q));
  }
  if (filter.fromDate) {
    const fd = filter.fromDate;
    res = res.filter(e => (e.departAt ?? '') >= fd);
  }
  if (filter.toDate) {
    const td = filter.toDate;
    res = res.filter(e => (e.departAt ?? '') <= td);
  }
  return res.sort((a, b) => (b.departAt ?? '').localeCompare(a.departAt ?? ''));
}
