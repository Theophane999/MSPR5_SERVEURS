package com.futurekawa.child;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CapteurMetricsPublisher {

    private final CapteurRepository capteurRepository;
    private final MeterRegistry meterRegistry;
    private final int entrepotId;

    private final AtomicReference<Double> latestTemperature = new AtomicReference<>(Double.NaN);
    private final AtomicReference<Double> latestHumidite = new AtomicReference<>(Double.NaN);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CapteurMetricsPublisher(
            CapteurRepository capteurRepository,
            MeterRegistry meterRegistry,
            @Value("${app.entrepot-id:1}") int entrepotId) {
        this.capteurRepository = capteurRepository;
        this.meterRegistry = meterRegistry;
        this.entrepotId = entrepotId;
    }

    @PostConstruct
    public void start() {
        Gauge.builder("futurekawa_capteur_temperature_celsius", latestTemperature, ref -> sanitize(ref.get()))
            .description("Derniere temperature capteur en degres Celsius")
            .tag("entrepot_id", String.valueOf(entrepotId))
            .register(meterRegistry);

        Gauge.builder("futurekawa_capteur_humidite_percent", latestHumidite, ref -> sanitize(ref.get()))
            .description("Derniere humidite capteur en pourcentage")
            .tag("entrepot_id", String.valueOf(entrepotId))
            .register(meterRegistry);

        refreshFromDatabase();
        scheduler.scheduleAtFixedRate(this::refreshFromDatabase, 30, 30, TimeUnit.SECONDS);
    }

    private void refreshFromDatabase() {
        Optional<CapteurRecord> latest = capteurRepository.findLatestByEntrepot(entrepotId);
        if (latest.isPresent()) {
            CapteurRecord capteur = latest.get();
            latestTemperature.set(capteur.temperature() != null ? capteur.temperature() : Double.NaN);
            latestHumidite.set(capteur.humidite() != null ? capteur.humidite() : Double.NaN);
            return;
        }

        latestTemperature.set(Double.NaN);
        latestHumidite.set(Double.NaN);
    }

    private double sanitize(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return Double.NaN;
        }
        return value;
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }
}
