import { DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { finalize } from 'rxjs';
import { ChildStatus, DashboardResponse, ExpeditionView, HistoryPoint, LotView } from './models/dashboard.model';
import { DashboardService } from './services/dashboard.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NgFor, NgIf, NgClass, DatePipe, DecimalPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private refreshTimer?: ReturnType<typeof setInterval>;

  protected readonly chartWidth = 560;
  protected readonly chartHeight = 220;

  protected loading = true;
  protected apiUrl = `${this.dashboardService.motherUrl()}/api/children`;
  protected aggregatedAt?: string;
  protected children: ChildStatus[] = [];
  protected errorMessage?: string;
  protected selectedCountryName?: string;
  protected selectedLotId?: string;

  ngOnInit(): void {
    this.refresh();
    this.refreshTimer = setInterval(() => this.refresh(), 300_000);
  }

  ngOnDestroy(): void {
    clearInterval(this.refreshTimer);
  }

  protected get onlineCount(): number {
    return this.children.filter((child) => child.available).length;
  }

  protected get offlineCount(): number {
    return this.children.filter((child) => !child.available).length;
  }

  protected get totalCount(): number {
    return this.children.length;
  }

  protected get availabilityPercent(): number {
    if (!this.totalCount) {
      return 0;
    }

    return Math.round((this.onlineCount / this.totalCount) * 100);
  }

  protected get cockpitTone(): 'stable' | 'warning' | 'critical' {
    if (this.offlineCount === 0 && this.totalCount > 0) {
      return 'stable';
    }

    if (this.onlineCount === 0 && this.totalCount > 0) {
      return 'critical';
    }

    return 'warning';
  }

  protected get cockpitHeadline(): string {
    if (this.loading) {
      return 'Synchronisation du reseau en cours';
    }

    if (this.errorMessage) {
      return 'Perte de visibilite sur l agregation';
    }

    if (this.cockpitTone === 'stable') {
      return 'Plateforme stable sur l ensemble des pays';
    }

    if (this.cockpitTone === 'critical') {
      return 'Tous les noeuds enfants sont hors ligne';
    }

    return 'Supervision degradee sur une partie du reseau';
  }

  protected get cockpitMessage(): string {
    if (this.loading) {
      return 'Le backend mere collecte les etats regionaux avant affichage.';
    }

    if (this.errorMessage) {
      return `Erreur remontee: ${this.errorMessage}`;
    }

    if (this.cockpitTone === 'stable') {
      return 'Tous les backends enfants repondent et la chaine de supervision est operationnelle.';
    }

    if (this.cockpitTone === 'critical') {
      return 'Aucun backend enfant ne remonte actuellement de signal exploitable.';
    }

    return `${this.offlineCount} noeud(x) demandent une intervention tandis que ${this.onlineCount} restent disponibles.`;
  }

  protected get motherStatusLabel(): string {
    if (this.loading) {
      return 'SCAN';
    }

    if (this.errorMessage) {
      return 'ALERTE';
    }

    return this.cockpitTone === 'stable' ? 'NOMINAL' : 'PARTIEL';
  }

  protected displayCountry(child: ChildStatus): string {
    if (child.data?.country) {
      return child.data.country;
    }

    return child.name.charAt(0).toUpperCase() + child.name.slice(1);
  }

  protected selectCountry(countryName: string): void {
    this.selectedCountryName = countryName;
    this.selectedLotId = this.selectedCountryLots[0]?.id;
  }

  protected selectLot(lotId: string): void {
    this.selectedLotId = lotId;
  }

  protected get selectedCountry(): ChildStatus | undefined {
    if (!this.children.length) {
      return undefined;
    }

    if (!this.selectedCountryName) {
      return this.children[0];
    }

    return this.children.find((child) => child.name === this.selectedCountryName) ?? this.children[0];
  }

  protected get selectedCountryDisplayName(): string {
    const selected = this.selectedCountry;
    return selected ? this.displayCountry(selected) : 'N/A';
  }

  protected get selectedCountryLots(): LotView[] {
    return [...(this.selectedCountry?.lots ?? [])].sort((a, b) => b.storageDate.localeCompare(a.storageDate));
  }

  protected get selectedLot(): LotView | undefined {
    const lots = this.selectedCountryLots;
    if (!lots.length) {
      return undefined;
    }

    if (!this.selectedLotId) {
      return lots[0];
    }

    return lots.find((lot) => lot.id === this.selectedLotId) ?? lots[0];
  }

  protected get selectedCountryAlerts() {
    return this.selectedCountry?.alerts ?? [];
  }

  protected get selectedCountryExpeditions(): ExpeditionView[] {
    return [...(this.selectedCountry?.expeditions ?? [])].sort((a, b) => b.departAt.localeCompare(a.departAt));
  }

  protected get selectedCountryStockState() {
    return this.selectedCountry?.stockState;
  }

  protected get stockBarOkPercent(): number {
    const stock = this.selectedCountryStockState;
    if (!stock || stock.totalLots <= 0) {
      return 0;
    }
    return (stock.healthyLots / stock.totalLots) * 100;
  }

  protected get stockBarWarningPercent(): number {
    const stock = this.selectedCountryStockState;
    if (!stock || stock.totalLots <= 0) {
      return 0;
    }
    return (stock.warningLots / stock.totalLots) * 100;
  }

  protected get stockBarCriticalPercent(): number {
    const stock = this.selectedCountryStockState;
    if (!stock || stock.totalLots <= 0) {
      return 0;
    }
    return (stock.criticalLots / stock.totalLots) * 100;
  }

  protected get selectedHistoryPoints(): HistoryPoint[] {
    return [...(this.selectedCountry?.history ?? [])].sort((a, b) => a.date.localeCompare(b.date));
  }

  protected get selectedHistoryPreview(): HistoryPoint[] {
    return this.selectedHistoryPoints.slice(-6).reverse();
  }

  protected get selectedDataDetails() {
    const history = this.selectedHistoryPoints;
    const lots = this.selectedCountryLots;
    const stock = this.selectedCountryStockState;
    const alerts = this.selectedCountryAlerts;
    const expeditions = this.selectedCountryExpeditions;
    const lastPoint = history.length ? history[history.length - 1] : undefined;

    return {
      historyCount: history.length,
      lotsCount: lots.length,
      alertsCount: alerts.length,
      expeditionsCount: expeditions.length,
      healthyLots: stock?.healthyLots ?? 0,
      warningLots: stock?.warningLots ?? 0,
      criticalLots: stock?.criticalLots ?? 0,
      lastPoint,
    };
  }

  protected toChartPath(kind: 'temperature' | 'humidite'): string {
    const points = this.selectedHistoryPoints;
    if (points.length < 2) {
      return '';
    }

    const values = points
      .map((point) => (kind === 'temperature' ? point.temperature : point.humidite))
      .filter((value): value is number => value !== null && value !== undefined);

    if (values.length < 2) {
      return '';
    }

    const min = Math.min(...values);
    const max = Math.max(...values);
    const span = max - min || 1;
    const stepX = this.chartWidth / (points.length - 1);

    return points
      .map((point, index) => {
        const value = kind === 'temperature' ? point.temperature : point.humidite;
        const safeValue = value ?? min;
        const x = index * stepX;
        const y = this.chartHeight - ((safeValue - min) / span) * this.chartHeight;
        return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
      })
      .join(' ');
  }

  protected lotStatusLabel(status: 'ok' | 'warning' | 'critical'): string {
    if (status === 'critical') {
      return 'Critique';
    }
    if (status === 'warning') {
      return 'Vigilance';
    }
    return 'Nominal';
  }

  protected trackByName(_index: number, child: ChildStatus): string {
    return child.name;
  }

  protected get sensorActiveCount(): number {
    return this.children.filter(c => c.sensorData?.available).length;
  }

  protected get avgTemperature(): number | null {
    const values = this.children
      .filter(c => c.sensorData?.available && c.sensorData.temperature != null)
      .map(c => c.sensorData!.temperature);
    return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
  }

  protected get avgHumidite(): number | null {
    const values = this.children
      .filter(c => c.sensorData?.available && c.sensorData.humidite != null)
      .map(c => c.sensorData!.humidite);
    return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
  }

  protected refresh(): void {
    this.loading = true;
    this.errorMessage = undefined;

    this.dashboardService
      .loadDashboard()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (payload: DashboardResponse) => {
          this.children = payload.children;
          this.aggregatedAt = payload.aggregatedAt;

          if (!this.children.length) {
            this.selectedCountryName = undefined;
            this.selectedLotId = undefined;
            return;
          }

          const selectedExists = this.children.some((child) => child.name === this.selectedCountryName);
          if (!selectedExists) {
            this.selectedCountryName = this.children[0].name;
          }

          const selectedLots = this.selectedCountryLots;
          const selectedLotExists = selectedLots.some((lot) => lot.id === this.selectedLotId);
          if (!selectedLotExists) {
            this.selectedLotId = selectedLots[0]?.id;
          }
        },
        error: (error: { message?: string }) => {
          this.errorMessage = error.message ?? 'Erreur inconnue';
        },
      });
  }
}
