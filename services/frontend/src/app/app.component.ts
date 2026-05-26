import { DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { finalize } from 'rxjs';
import { ChildStatus, DashboardResponse } from './models/dashboard.model';
import { DashboardService } from './services/dashboard.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NgFor, NgIf, NgClass, DatePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  protected loading = true;
  protected apiUrl = `${this.dashboardService.motherUrl()}/api/children`;
  protected aggregatedAt?: string;
  protected children: ChildStatus[] = [];
  protected errorMessage?: string;

  ngOnInit(): void {
    this.refresh();
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
