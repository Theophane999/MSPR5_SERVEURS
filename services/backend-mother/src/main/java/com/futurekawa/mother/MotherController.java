package com.futurekawa.mother;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "*")
public class MotherController {

    private final AggregationService aggregationService;
    private final HttpClient httpClient;

    public MotherController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "role", "mother");
    }

    @GetMapping("/api/children")
    public AggregationService.AggregatedResponse children() {
        return aggregationService.aggregate();
    }

    @PostMapping("/api/children/{childName}/lots")
    public ResponseEntity<String> createLot(@PathVariable String childName, @RequestBody String payload) {
        return proxyToChild(childName, "POST", "/api/lots", payload);
    }

    @PutMapping("/api/children/{childName}/lots/{lotId}")
    public ResponseEntity<String> updateLot(@PathVariable String childName, @PathVariable String lotId, @RequestBody String payload) {
        return proxyToChild(childName, "PUT", "/api/lots/" + lotId, payload);
    }

    @DeleteMapping("/api/children/{childName}/lots/{lotId}")
    public ResponseEntity<String> deleteLot(@PathVariable String childName, @PathVariable String lotId) {
        return proxyToChild(childName, "DELETE", "/api/lots/" + lotId, null);
    }

    @PostMapping("/api/children/{childName}/expeditions")
    public ResponseEntity<String> createExpedition(@PathVariable String childName, @RequestBody String payload) {
        return proxyToChild(childName, "POST", "/api/expeditions", payload);
    }

    @PutMapping("/api/children/{childName}/expeditions/{expeditionId}")
    public ResponseEntity<String> updateExpedition(@PathVariable String childName, @PathVariable String expeditionId, @RequestBody String payload) {
        return proxyToChild(childName, "PUT", "/api/expeditions/" + expeditionId, payload);
    }

    @DeleteMapping("/api/children/{childName}/expeditions/{expeditionId}")
    public ResponseEntity<String> deleteExpedition(@PathVariable String childName, @PathVariable String expeditionId) {
        return proxyToChild(childName, "DELETE", "/api/expeditions/" + expeditionId, null);
    }

    private ResponseEntity<String> proxyToChild(String childName, String method, String path, String payload) {
        String childBaseUrl = aggregationService
            .resolveChildBaseUrl(childName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Backend enfant inconnu: " + childName));

        String normalizedBaseUrl = childBaseUrl.endsWith("/") ? childBaseUrl.substring(0, childBaseUrl.length() - 1) : childBaseUrl;
        URI uri = URI.create(normalizedBaseUrl + path);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json");

        HttpRequest request;
        switch (method) {
            case "POST" -> {
                request = requestBuilder
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload == null ? "" : payload, StandardCharsets.UTF_8))
                    .build();
            }
            case "PUT" -> {
                request = requestBuilder
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload == null ? "" : payload, StandardCharsets.UTF_8))
                    .build();
            }
            case "DELETE" -> request = requestBuilder.DELETE().build();
            default -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Methode proxy non supportee: " + method);
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.statusCode());

            response.headers().firstValue("Content-Type").ifPresent(contentType -> responseBuilder.header("Content-Type", contentType));

            String responseBody = response.body();
            if (responseBody == null || responseBody.isBlank()) {
                return responseBuilder.build();
            }

            return responseBuilder.body(responseBody);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Proxy interrompt", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Proxy vers backend enfant indisponible", exception);
        }
    }
}
