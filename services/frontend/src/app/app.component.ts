import { DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { finalize } from 'rxjs';
import { ChildStatus, DashboardResponse } from './models/dashboard.model';
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

  protected loading = true;
  protected apiUrl = `${this.dashboardService.motherUrl()}/api/children`;
  protected aggregatedAt?: string;
  protected children: ChildStatus[] = [];
  protected errorMessage?: string;

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
        },
        error: (error: { message?: string }) => {
          this.errorMessage = error.message ?? 'Erreur inconnue';
        },
      });
  }
}
