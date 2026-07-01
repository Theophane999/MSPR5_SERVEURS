export interface CapteurData {
  idEntrepot: number;
  temperature: number;
  humidite: number;
  date: string;
  available: boolean;
}

export interface HistoryPoint {
  id: number;
  temperature: number;
  humidite: number;
  date: string;
  idEntrepot: number;
}

export interface LotView {
  id: string;
  lotReference?: string | null;
  storageDate: string;
  status: 'ok' | 'warning' | 'critical';
  temperature: number | null;
  humidite: number | null;
  variete?: string | null;
  process?: string | null;
  scoreSca?: number | null;
  poidsKg?: number | null;
  qualite?: string | null;
  quantite?: number | null;
  datePeremption?: number | null;
}

export interface ExpeditionLotView {
  lotReference: string;
  quantiteExpediee: number | null;
  poidsExpedieKg: number | null;
}

export interface ExpeditionView {
  id: string;
  statut: string;
  destinationPays: string;
  destinationVille: string;
  destinationClient: string;
  poidsTotalKg: number | null;
  trackingTransporteur: string;
  quaiDepart: string;
  transporteur: string;
  livreurNom: string;
  livreurTelephone: string;
  departAt: string;
  arriveeEstimeeAt: string;
  lots: ExpeditionLotView[];
}

export interface StockState {
  totalLots: number;
  healthyLots: number;
  warningLots: number;
  criticalLots: number;
  lastStorageDate: string | null;
}

export interface AlertView {
  level: 'warning' | 'critical';
  message: string;
  timestamp: string;
}

export interface ChildStatus {
  name: string;
  url: string;
  available: boolean;
  data?: {
    role: string;
    country: string;
    message: string;
    timestamp: string;
  };
  sensorData?: CapteurData;
  history?: HistoryPoint[];
  lots?: LotView[];
  expeditions?: ExpeditionView[];
  stockState?: StockState;
  alerts?: AlertView[];
  error?: string;
}

export interface DashboardResponse {
  role: string;
  aggregatedAt: string;
  children: ChildStatus[];
}
