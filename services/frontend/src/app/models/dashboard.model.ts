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
  error?: string;
}

export interface DashboardResponse {
  role: string;
  aggregatedAt: string;
  children: ChildStatus[];
}
