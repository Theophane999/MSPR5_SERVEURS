package com.futurekawa.child;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/api/expeditions")
    public List<Map<String, Object>> expeditions() {
        return stockRepository.findExpeditionsByEntrepot(entrepotId);
    }
}
