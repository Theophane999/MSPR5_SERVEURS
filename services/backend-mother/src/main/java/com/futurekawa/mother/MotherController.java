package com.futurekawa.mother;

import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class MotherController {

    private final AggregationService aggregationService;

    public MotherController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "role", "mother");
    }

    @GetMapping("/api/children")
    public AggregationService.AggregatedResponse children() {
        return aggregationService.aggregate();
    }
}
