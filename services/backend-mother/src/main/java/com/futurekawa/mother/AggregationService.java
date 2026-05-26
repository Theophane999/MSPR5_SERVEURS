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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AggregationService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<ChildTarget> childTargets;

    @Autowired
    public AggregationService(
        @Value("${app.children-services:brazil=http://localhost:3101,ecuador=http://localhost:3102,colombia=http://localhost:3103}") String childrenServices,
        ObjectMapper objectMapper
    ) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(), objectMapper, parseChildrenServices(childrenServices));
    }

    AggregationService(HttpClient httpClient, ObjectMapper objectMapper, List<ChildTarget> childTargets) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.childTargets = childTargets;
    }

    public AggregatedResponse aggregate() {
        List<ChildAggregate> children = new ArrayList<>();

        for (ChildTarget childTarget : childTargets) {
            children.add(fetchChild(childTarget));
        }

        return new AggregatedResponse("mother", Instant.now().toString(), children);
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

            return new ChildAggregate(childTarget.name(), childTarget.url(), response.statusCode() < 400, payload, null);
        } catch (Exception exception) {
            return new ChildAggregate(childTarget.name(), childTarget.url(), false, null, exception.getMessage());
        }
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

    public record ChildAggregate(String name, String url, boolean available, Map<String, Object> data, String error) {
    }
}
