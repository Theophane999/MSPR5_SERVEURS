package com.futurekawa.child;

import java.util.List;

public record ExpeditionPayload(
    String departAt,
    String arriveeEstimeeAt,
    String destinationPays,
    String destinationVille,
    String destinationClient,
    Double poidsTotalKg,
    String trackingTransporteur,
    String quaiDepart,
    String transporteur,
    String livreurNom,
    String livreurTelephone,
    String statut,
    List<ExpeditionLotPayload> lots
) {
}
