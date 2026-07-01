package com.futurekawa.child;

public record LotPayload(
    String lotReference,
    Integer datePeremption,
    String variete,
    String process,
    Double scoreSca,
    Double poidsKg,
    String qualite,
    Integer quantite,
    String storageDate,
    Integer idExploitation
) {
}
