export interface CapteurData {
  idEntrepot: number;
  temperature: number;
  humidite: number;
  date: string;
  available: boolean;
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
  error?: string;
}

export interface DashboardResponse {
  role: string;
  aggregatedAt: string;
  children: ChildStatus[];
}
