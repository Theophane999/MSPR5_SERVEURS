import { LotView, ExpeditionView } from '../models/dashboard.model';

export type SortOrder = 'asc' | 'desc';

export type LotSortField =
  | 'storageDate'
  | 'lotReference'
  | 'variete'
  | 'process'
  | 'qualite'
  | 'status'
  | 'quantite'
  | 'poidsKg'
  | 'scoreSca'
  | 'datePeremption';

export type ExpeditionSortField =
  | 'departAt'
  | 'arriveeEstimeeAt'
  | 'statut'
  | 'destinationClient'
  | 'destinationPays'
  | 'destinationVille'
  | 'transporteur'
  | 'livreurNom'
  | 'quaiDepart'
  | 'poidsTotalKg';

export type LotFilter = {
  status?: 'all' | 'ok' | 'warning' | 'critical';
  query?: string; // search by variety or id
  fromDate?: string; // ISO date
  toDate?: string;   // ISO date
  sortBy?: LotSortField;
  sortOrder?: SortOrder;
};

export type ExpeditionFilter = {
  status?: 'all' | string;
  query?: string; // search by client or destination
  fromDate?: string;
  toDate?: string;
  sortBy?: ExpeditionSortField;
  sortOrder?: SortOrder;
};

function compareValues(a: string | number | null | undefined, b: string | number | null | undefined, order: SortOrder): number {
  const direction = order === 'asc' ? 1 : -1;

  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;

  if (typeof a === 'number' && typeof b === 'number') {
    return (a - b) * direction;
  }

  return String(a).localeCompare(String(b), 'fr', { numeric: true, sensitivity: 'base' }) * direction;
}

function sortLots(lots: LotView[], sortBy: LotSortField, sortOrder: SortOrder): LotView[] {
  return lots.sort((a, b) => compareValues(a[sortBy] as string | number | null | undefined, b[sortBy] as string | number | null | undefined, sortOrder));
}

function sortExpeditions(exps: ExpeditionView[], sortBy: ExpeditionSortField, sortOrder: SortOrder): ExpeditionView[] {
  return exps.sort((a, b) => compareValues(a[sortBy] as string | number | null | undefined, b[sortBy] as string | number | null | undefined, sortOrder));
}

export function filterLots(lots: LotView[] = [], filter: LotFilter = {}): LotView[] {
  let res = [...lots];
  if (filter.status && filter.status !== 'all') {
    res = res.filter(l => l.status === filter.status);
  }
  if (filter.query?.trim()) {
    const q = filter.query.trim().toLowerCase();
    res = res.filter(l => {
      const haystack = [l.id, l.lotReference, l.variete, l.process, l.qualite]
        .filter((value): value is string => Boolean(value))
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }
  if (filter.fromDate) {
    const fd = filter.fromDate;
    res = res.filter(l => l.storageDate >= fd);
  }
  if (filter.toDate) {
    const td = filter.toDate;
    res = res.filter(l => l.storageDate <= td);
  }
  const sortBy = filter.sortBy ?? 'storageDate';
  const sortOrder = filter.sortOrder ?? 'desc';
  return sortLots(res, sortBy, sortOrder);
}

export function filterExpeditions(exps: ExpeditionView[] = [], filter: ExpeditionFilter = {}): ExpeditionView[] {
  let res = [...exps];
  if (filter.status && filter.status !== 'all') {
    const st = filter.status.toLowerCase();
    res = res.filter(e => (e.statut ?? '').toLowerCase() === st);
  }
  if (filter.query?.trim()) {
    const q = filter.query.trim().toLowerCase();
    res = res.filter(e => {
      const haystack = [
        e.id,
        e.destinationPays,
        e.destinationVille,
        e.destinationClient,
        e.transporteur,
        e.livreurNom,
        e.trackingTransporteur,
        e.quaiDepart,
      ]
        .filter((value): value is string => Boolean(value))
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }
  if (filter.fromDate) {
    const fd = filter.fromDate;
    res = res.filter(e => (e.departAt ?? '') >= fd);
  }
  if (filter.toDate) {
    const td = filter.toDate;
    res = res.filter(e => (e.departAt ?? '') <= td);
  }
  const sortBy = filter.sortBy ?? 'departAt';
  const sortOrder = filter.sortOrder ?? 'desc';
  return sortExpeditions(res, sortBy, sortOrder);
}
