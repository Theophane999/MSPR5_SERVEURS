package com.futurekawa.child;

import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
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
public class StockController {

    private final StockRepository stockRepository;
    private final int entrepotId;

    public StockController(
            StockRepository stockRepository,
            @Value("${app.entrepot-id:1}") int entrepotId) {
        this.stockRepository = stockRepository;
        this.entrepotId = entrepotId;
    }

    @GetMapping("/api/lots")
    public List<Map<String, Object>> lots() {
        return stockRepository.findLotsByEntrepot(entrepotId);
    }

    @GetMapping("/api/lots/{lotId}")
    public Map<String, Object> lotById(@PathVariable long lotId) {
        return stockRepository
            .findLotByIdAndEntrepot(lotId, entrepotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot introuvable"));
    }

    @PostMapping("/api/lots")
    public ResponseEntity<Map<String, Object>> createLot(@RequestBody LotPayload payload) {
        validateCreatePayload(payload);
        Map<String, Object> created = stockRepository.createLot(entrepotId, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/lots/{lotId}")
    public Map<String, Object> updateLot(@PathVariable long lotId, @RequestBody LotPayload payload) {
        if (stockRepository.findLotByIdAndEntrepot(lotId, entrepotId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot introuvable");
        }

        stockRepository.updateLot(lotId, entrepotId, payload);
        return stockRepository
            .findLotByIdAndEntrepot(lotId, entrepotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot introuvable"));
    }

    @DeleteMapping("/api/lots/{lotId}")
    public ResponseEntity<Void> deleteLot(@PathVariable long lotId) {
        if (stockRepository.findLotByIdAndEntrepot(lotId, entrepotId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot introuvable");
        }

        try {
            stockRepository.deleteLotByIdAndEntrepot(lotId, entrepotId);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer un lot deja lie a une expedition");
        }
    }

    private void validateCreatePayload(LotPayload payload) {
        if (payload == null
            || isBlank(payload.lotReference())
            || payload.datePeremption() == null
            || isBlank(payload.variete())
            || isBlank(payload.process())
            || payload.poidsKg() == null
            || isBlank(payload.qualite())
            || payload.quantite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload lot invalide ou incomplet");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
