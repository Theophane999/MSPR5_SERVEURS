package com.futurekawa.child;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    private final String country;

    public InfoController(@Value("${app.country:Unknown}") String country) {
        this.country = country;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "role", "child");
    }

    @GetMapping("/api/info")
    public ChildInfoResponse info() {
        return new ChildInfoResponse(
            "child",
            country,
            "Backend child " + country + " is online",
            Instant.now().toString()
        );
    }

    public record ChildInfoResponse(String role, String country, String message, String timestamp) {
    }
}
