import { Component, Input, ViewChild, ElementRef, AfterViewInit, OnChanges } from '@angular/core';
import { Chart, ChartConfiguration } from 'chart.js';
import { HistoryPoint } from '../models/dashboard.model';

@Component({
  selector: 'app-temperature-chart',
  standalone: true,
  template: `
    <div class="chart-container">
      <div class="chart-wrapper">
        <canvas #chartCanvas></canvas>
      </div>
    </div>
  `,
  styles: [`
    .chart-container {
      width: 100%;
      height: 400px;
      position: relative;
      margin-bottom: 20px;
    }
    .chart-wrapper {
      position: relative;
      width: 100%;
      height: 100%;
    }
    canvas {
      max-height: 100%;
    }
  `]
})
export class TemperatureChartComponent implements AfterViewInit, OnChanges {
  @Input() historyPoints: HistoryPoint[] = [];
  @ViewChild('chartCanvas', { static: false }) chartCanvas!: ElementRef;

  private chart?: Chart;

  ngAfterViewInit() {
    if (this.historyPoints.length > 1) {
      this.initChart();
    }
  }

  ngOnChanges() {
    if (this.chart) {
      this.updateChart();
    }
  }

  private initChart() {
    const labels = this.historyPoints.map(p => {
      const date = new Date(p.date);
      return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    });

    const tempData = this.historyPoints.map(p => p.temperature);
    const humData = this.historyPoints.map(p => p.humidite);

    const config: ChartConfiguration = {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Température (°C)',
            data: tempData,
            borderColor: '#FF6B6B',
            backgroundColor: 'rgba(255, 107, 107, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            yAxisID: 'y'
          },
          {
            label: 'Humidité (%)',
            data: humData,
            borderColor: '#4ECDC4',
            backgroundColor: 'rgba(78, 205, 196, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false
        },
        plugins: {
          legend: {
            display: true,
            position: 'top'
          },
          title: {
            display: true,
            text: 'Courbes Température et Humidité'
          }
        },
        scales: {
          y: {
            type: 'linear',
            position: 'left',
            title: {
              display: true,
              text: 'Température (°C)'
            }
          },
          y1: {
            type: 'linear',
            position: 'right',
            title: {
              display: true,
              text: 'Humidité (%)'
            },
            grid: {
              drawOnChartArea: false
            }
          }
        }
      }
    };

    const ctx = this.chartCanvas?.nativeElement?.getContext('2d');
    if (ctx) {
      this.chart = new Chart(ctx, config);
    }
  }

  private updateChart() {
    if (!this.chart) return;

    const labels = this.historyPoints.map(p => {
      const date = new Date(p.date);
      return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    });

    const tempData = this.historyPoints.map(p => p.temperature);
    const humData = this.historyPoints.map(p => p.humidite);

    this.chart.data.labels = labels;
    this.chart.data.datasets[0].data = tempData;
    this.chart.data.datasets[1].data = humData;
    this.chart.update();
  }
}
