package com.futurekawa.child;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class CapteurController {

    private final CapteurRepository capteurRepository;
    private final int entrepotId;

    public CapteurController(
            CapteurRepository capteurRepository,
            @Value("${app.entrepot-id:1}") int entrepotId) {
        this.capteurRepository = capteurRepository;
        this.entrepotId = entrepotId;
    }

    @GetMapping("/api/capteurs")
    public List<CapteurRecord> capteurs() {
        return capteurRepository.findRecentByEntrepot(entrepotId);
    }

    @GetMapping("/api/capteurs/latest")
    public Map<String, Object> latest() {
        Optional<CapteurRecord> latest = capteurRepository.findLatestByEntrepot(entrepotId);
        if (latest.isEmpty()) {
            return Map.of("available", false, "idEntrepot", entrepotId);
        }
        CapteurRecord c = latest.get();
        return Map.of(
            "available", true,
            "idEntrepot", c.idEntrepot(),
            "temperature", c.temperature() != null ? c.temperature() : 0.0,
            "humidite", c.humidite() != null ? c.humidite() : 0.0,
            "date", c.date() != null ? c.date().toString() : ""
        );
    }
}
