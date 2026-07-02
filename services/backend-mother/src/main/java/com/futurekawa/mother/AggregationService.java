package com.futurekawa.mother;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AggregationService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<ChildTarget> childTargets;
    private final double tempMin;
    private final double tempMax;
    private final double humiditeMin;
    private final double humiditeMax;

    @Autowired
    public AggregationService(
        @Value("${app.children-services:brazil=http://localhost:3101,ecuador=http://localhost:3102,colombia=http://localhost:3103}") String childrenServices,
        @Value("${app.alert.threshold.temp.min:15}") double tempMin,
        @Value("${app.alert.threshold.temp.max:30}") double tempMax,
        @Value("${app.alert.threshold.humidite.min:45}") double humiditeMin,
        @Value("${app.alert.threshold.humidite.max:75}") double humiditeMax,
        ObjectMapper objectMapper
    ) {
        this(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
            objectMapper,
            parseChildrenServices(childrenServices),
            tempMin,
            tempMax,
            humiditeMin,
            humiditeMax
        );
    }

    AggregationService(
        HttpClient httpClient,
        ObjectMapper objectMapper,
        List<ChildTarget> childTargets,
        double tempMin,
        double tempMax,
        double humiditeMin,
        double humiditeMax
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.childTargets = childTargets;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.humiditeMin = humiditeMin;
        this.humiditeMax = humiditeMax;
    }

    public AggregatedResponse aggregate() {
        List<ChildAggregate> children = new ArrayList<>();

        for (ChildTarget childTarget : childTargets) {
            children.add(fetchChild(childTarget));
        }

        return new AggregatedResponse("mother", Instant.now().toString(), children);
    }

    public Optional<String> resolveChildBaseUrl(String childName) {
        if (childName == null || childName.isBlank()) {
            return Optional.empty();
        }

        String normalizedName = childName.trim();
        return childTargets.stream()
            .filter(target -> target.name().equalsIgnoreCase(normalizedName))
            .map(ChildTarget::url)
            .findFirst();
    }

    private ChildAggregate fetchChild(ChildTarget childTarget) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(childTarget.url() + "/api/info"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            Map<String, Object> sensorData = fetchSensorData(childTarget);
            List<Map<String, Object>> history = fetchSensorHistory(childTarget);
            List<Map<String, Object>> childLots = fetchLots(childTarget);
            List<LotView> lots = buildLots(childLots, sensorData, history);
            StockState stockState = buildStockState(lots);
            List<AlertView> alerts = buildAlerts(childTarget, response.statusCode() < 400, sensorData, lots);
            List<ExpeditionView> expeditions = buildExpeditions(fetchExpeditions(childTarget));

            return new ChildAggregate(
                childTarget.name(),
                childTarget.url(),
                response.statusCode() < 400,
                payload,
                sensorData,
                history,
                lots,
                stockState,
                alerts,
                expeditions,
                null
            );
        } catch (Exception exception) {
            return new ChildAggregate(
                childTarget.name(),
                childTarget.url(),
                false,
                null,
                null,
                List.of(),
                List.of(),
                new StockState(0, 0, 0, 0, null),
                List.of(new AlertView("critical", withZone(childTarget, "Backend pays indisponible"), Instant.now().toString())),
                List.of(),
                exception.getMessage()
            );
        }
    }

    private Map<String, Object> fetchSensorData(ChildTarget childTarget) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(childTarget.url() + "/api/capteurs/latest"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("available", false);
        }
    }

    private List<Map<String, Object>> fetchSensorHistory(ChildTarget childTarget) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(childTarget.url() + "/api/capteurs"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> raw = objectMapper.readValue(response.body(), new TypeReference<>() {});
            return raw.stream().map(this::normalizePoint).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> fetchLots(ChildTarget childTarget) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(childTarget.url() + "/api/lots"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> fetchExpeditions(ChildTarget childTarget) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(childTarget.url() + "/api/expeditions"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> normalizePoint(Map<String, Object> point) {
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("id", point.get("id"));
        normalized.put("date", point.get("date"));
        normalized.put("temperature", asDouble(point.get("temperature")).orElse(null));
        normalized.put("humidite", asDouble(point.get("humidite")).orElse(null));
        normalized.put("idEntrepot", point.get("idEntrepot"));
        return normalized;
    }

    private List<LotView> buildLots(List<Map<String, Object>> childLots, Map<String, Object> sensorData, List<Map<String, Object>> history) {
        if (childLots.isEmpty()) {
            return buildLotsFromHistory(history);
        }

        Double latestTemp = asDouble(sensorData.get("temperature")).orElse(null);
        Double latestHum = asDouble(sensorData.get("humidite")).orElse(null);
        String status = computeStatus(latestTemp, latestHum);

        List<LotView> lots = new ArrayList<>();
        for (Map<String, Object> point : childLots) {
            lots.add(new LotView(
                String.valueOf(point.getOrDefault("id", "lot-unknown")),
                valueAsString(point.get("lotReference")),
                String.valueOf(point.getOrDefault("storageDate", "")),
                status,
                latestTemp,
                latestHum,
                valueAsString(point.get("variete")),
                valueAsString(point.get("process")),
                asDouble(point.get("scoreSca")).orElse(null),
                asDouble(point.get("poidsKg")).orElse(null),
                valueAsString(point.get("qualite")),
                asInteger(point.get("quantite")),
                asInteger(point.get("datePeremption"))
            ));
        }
        lots.sort(Comparator.comparing(LotView::storageDate).reversed());
        return lots;
    }

    private List<LotView> buildLotsFromHistory(List<Map<String, Object>> history) {
        List<LotView> lots = new ArrayList<>();
        for (Map<String, Object> point : history) {
            String status = computeStatus(asDouble(point.get("temperature")).orElse(null), asDouble(point.get("humidite")).orElse(null));
            lots.add(new LotView(
                String.valueOf(point.getOrDefault("id", "lot-unknown")),
                null,
                String.valueOf(point.getOrDefault("date", "")),
                status,
                asDouble(point.get("temperature")).orElse(null),
                asDouble(point.get("humidite")).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ));
        }
        lots.sort(Comparator.comparing(LotView::storageDate).reversed());
        return lots;
    }

    private List<ExpeditionView> buildExpeditions(List<Map<String, Object>> rawExpeditions) {
        List<ExpeditionView> expeditions = new ArrayList<>();
        for (Map<String, Object> expedition : rawExpeditions) {
            List<ExpeditionLotView> lots = new ArrayList<>();
            Object rawLots = expedition.get("lots");
            if (rawLots instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> rawItem) {
                        lots.add(new ExpeditionLotView(
                            asInteger(rawItem.get("lotId")),
                            valueAsString(rawItem.get("lotReference")),
                            asInteger(rawItem.get("quantiteExpediee")),
                            asDouble(rawItem.get("poidsExpedieKg")).orElse(null)
                        ));
                    }
                }
            }

            expeditions.add(new ExpeditionView(
                valueAsString(expedition.get("id")),
                valueAsString(expedition.get("statut")),
                valueAsString(expedition.get("destinationPays")),
                valueAsString(expedition.get("destinationVille")),
                valueAsString(expedition.get("destinationClient")),
                asDouble(expedition.get("poidsTotalKg")).orElse(null),
                valueAsString(expedition.get("trackingTransporteur")),
                valueAsString(expedition.get("quaiDepart")),
                valueAsString(expedition.get("transporteur")),
                valueAsString(expedition.get("livreurNom")),
                valueAsString(expedition.get("livreurTelephone")),
                valueAsString(expedition.get("departAt")),
                valueAsString(expedition.get("arriveeEstimeeAt")),
                lots
            ));
        }
        return expeditions;
    }

    private StockState buildStockState(List<LotView> lots) {
        int healthy = 0;
        int warning = 0;
        int critical = 0;

        for (LotView lot : lots) {
            switch (lot.status()) {
                case "critical" -> critical++;
                case "warning" -> warning++;
                default -> healthy++;
            }
        }

        String lastStorageDate = lots.isEmpty() ? null : lots.get(0).storageDate();
        return new StockState(lots.size(), healthy, warning, critical, lastStorageDate);
    }

    private List<AlertView> buildAlerts(
        ChildTarget childTarget,
        boolean available,
        Map<String, Object> sensorData,
        List<LotView> lots
    ) {
        List<AlertView> alerts = new ArrayList<>();
        if (!available) {
            alerts.add(new AlertView("critical", withZone(childTarget, "Pays injoignable"), Instant.now().toString()));
            return alerts;
        }

        if (lots.isEmpty()) {
            alerts.add(new AlertView("warning", withZone(childTarget, "Aucune mesure historique disponible"), Instant.now().toString()));
        }

        if (sensorData == null || !Boolean.TRUE.equals(sensorData.get("available"))) {
            alerts.add(new AlertView("warning", withZone(childTarget, "Capteurs indisponibles"), Instant.now().toString()));
        }

        for (LotView lot : lots.stream().limit(5).toList()) {
            if ("critical".equals(lot.status())) {
                alerts.add(new AlertView("critical", withZone(childTarget, "Lot " + lot.id() + " hors seuil critique"), Instant.now().toString()));
            } else if ("warning".equals(lot.status())) {
                alerts.add(new AlertView("warning", withZone(childTarget, "Lot " + lot.id() + " hors seuil de vigilance"), Instant.now().toString()));
            }
        }

        return alerts;
    }

    private String withZone(ChildTarget childTarget, String message) {
        String zone = childTarget.name() != null ? childTarget.name() : "inconnue";
        return "[Zone " + zone + "] " + message;
    }

    private String lotLabel(LotView lot) {
        return lot.lotReference() != null && !lot.lotReference().isBlank() ? lot.lotReference() : lot.id();
    }

    private String computeStatus(Double temperature, Double humidite) {
        if (temperature == null || humidite == null) {
            return "warning";
        }

        boolean tempCritical = temperature < tempMin - 2 || temperature > tempMax + 2;
        boolean humCritical = humidite < humiditeMin - 5 || humidite > humiditeMax + 5;
        if (tempCritical || humCritical) {
            return "critical";
        }

        boolean tempWarning = temperature < tempMin || temperature > tempMax;
        boolean humWarning = humidite < humiditeMin || humidite > humiditeMax;
        return (tempWarning || humWarning) ? "warning" : "ok";
    }

    private Optional<Double> asDouble(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return Optional.of(number.doubleValue());
        }
        try {
            return Optional.of(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<ChildTarget> parseChildrenServices(String childrenServices) {
        return Stream.of(childrenServices.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isBlank())
            .map(AggregationService::toChildTarget)
            .toList();
    }

    private static ChildTarget toChildTarget(String entry) {
        int separatorIndex = entry.indexOf('=');

        if (separatorIndex > 0) {
            String configuredName = entry.substring(0, separatorIndex).trim();
            String configuredUrl = entry.substring(separatorIndex + 1).trim();
            String effectiveName = configuredName.isBlank() ? defaultNameFromUrl(configuredUrl) : configuredName;
            return new ChildTarget(effectiveName, configuredUrl);
        }

        return new ChildTarget(defaultNameFromUrl(entry), entry);
    }

    private static String defaultNameFromUrl(String url) {
        try {
            String host = URI.create(url).getHost();

            if (host == null || host.isBlank()) {
                return "child-" + Math.abs(url.hashCode());
            }

            return host.startsWith("backend-") ? host.substring("backend-".length()) : host;
        } catch (Exception exception) {
            return "child-" + Math.abs(url.hashCode());
        }
    }

    public record ChildTarget(String name, String url) {
    }

    public record AggregatedResponse(String role, String aggregatedAt, List<ChildAggregate> children) {
    }

    public record ChildAggregate(
        String name,
        String url,
        boolean available,
        Map<String, Object> data,
        Map<String, Object> sensorData,
        List<Map<String, Object>> history,
        List<LotView> lots,
        StockState stockState,
        List<AlertView> alerts,
        List<ExpeditionView> expeditions,
        String error
    ) {
    }

    public record LotView(
        String id,
        String lotReference,
        String storageDate,
        String status,
        Double temperature,
        Double humidite,
        String variete,
        String process,
        Double scoreSca,
        Double poidsKg,
        String qualite,
        Integer quantite,
        Integer datePeremption
    ) {
    }

    public record StockState(int totalLots, int healthyLots, int warningLots, int criticalLots, String lastStorageDate) {
    }

    public record AlertView(String level, String message, String timestamp) {
    }

    public record ExpeditionView(
        String id,
        String statut,
        String destinationPays,
        String destinationVille,
        String destinationClient,
        Double poidsTotalKg,
        String trackingTransporteur,
        String quaiDepart,
        String transporteur,
        String livreurNom,
        String livreurTelephone,
        String departAt,
        String arriveeEstimeeAt,
        List<ExpeditionLotView> lots
    ) {
    }

    public record ExpeditionLotView(Integer lotId, String lotReference, Integer quantiteExpediee, Double poidsExpedieKg) {
    }
}
