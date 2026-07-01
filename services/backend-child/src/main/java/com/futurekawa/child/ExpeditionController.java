package com.futurekawa.child;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
public class ExpeditionController {

    private final StockRepository stockRepository;
    private final int entrepotId;

    public ExpeditionController(
            StockRepository stockRepository,
            @Value("${app.entrepot-id:1}") int entrepotId) {
        this.stockRepository = stockRepository;
        this.entrepotId = entrepotId;
    }

    @GetMapping("/api/expeditions")
    public List<Map<String, Object>> expeditions() {
        return stockRepository.findExpeditionsByEntrepot(entrepotId);
    }

    @GetMapping("/api/expeditions/{expeditionId}")
    public Map<String, Object> expeditionById(@PathVariable long expeditionId) {
        return stockRepository
            .findExpeditionByIdAndEntrepot(expeditionId, entrepotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expedition introuvable"));
    }

    @PostMapping("/api/expeditions")
    public ResponseEntity<Map<String, Object>> createExpedition(@RequestBody ExpeditionPayload payload) {
        validatePayload(payload);
        try {
            Map<String, Object> created = stockRepository.createExpedition(entrepotId, payload);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PutMapping("/api/expeditions/{expeditionId}")
    public Map<String, Object> updateExpedition(@PathVariable long expeditionId, @RequestBody ExpeditionPayload payload) {
        validatePayload(payload);
        try {
            return stockRepository.updateExpedition(expeditionId, entrepotId, payload);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expedition introuvable");
        }
    }

    @DeleteMapping("/api/expeditions/{expeditionId}")
    public ResponseEntity<Void> deleteExpedition(@PathVariable long expeditionId) {
        try {
            stockRepository.deleteExpeditionByIdAndEntrepot(expeditionId, entrepotId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expedition introuvable");
        }
    }

    private void validatePayload(ExpeditionPayload payload) {
        if (payload == null
            || isBlank(payload.departAt())
            || isBlank(payload.destinationPays())
            || isBlank(payload.destinationVille())
            || isBlank(payload.destinationClient())
            || payload.poidsTotalKg() == null
            || isBlank(payload.livreurNom())
            || isBlank(payload.statut())
            || payload.lots() == null
            || payload.lots().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload expedition invalide ou incomplet");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
