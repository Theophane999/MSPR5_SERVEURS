package com.futurekawa.child;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepository {

    private static final String LOTS_SELECT =
        "SELECT l.\"ID_LOT\" AS id, l.\"ID_chargement\" AS id_chargement, l.\"ID_exploitation\" AS id_exploitation, " +
            "l.\"lot_reference\" AS lot_reference, c.\"datetime\" AS storage_date, " +
            "l.\"date_peremption\" AS date_peremption, l.\"variete\" AS variete, l.\"process\" AS process, " +
            "l.\"score_sca\" AS score_sca, l.\"poids_kg\" AS poids_kg, l.\"qualite\" AS qualite, l.\"quantite\" AS quantite " +
            "FROM \"lot\" l " +
            "LEFT JOIN \"chargement\" c ON c.\"ID_chargement\" = l.\"ID_chargement\" ";

    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findLotsByEntrepot(int idEntrepot) {
        return jdbc.query(
            LOTS_SELECT +
                "WHERE l.\"ID_ENTREPOT\" = ? " +
                "ORDER BY c.\"datetime\" DESC NULLS LAST, l.\"ID_LOT\" DESC",
            (rs, rowNum) -> mapLotRow(rs),
            idEntrepot
        );
    }

    public Optional<Map<String, Object>> findLotByIdAndEntrepot(long lotId, int idEntrepot) {
        List<Map<String, Object>> rows = jdbc.query(
            LOTS_SELECT + "WHERE l.\"ID_LOT\" = ? AND l.\"ID_ENTREPOT\" = ?",
            (rs, rowNum) -> mapLotRow(rs),
            lotId,
            idEntrepot
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(rows.get(0));
    }

    public Map<String, Object> createLot(int idEntrepot, LotPayload payload) {
        LocalDate storageDate = parseStorageDate(payload.storageDate());
        Integer chargementId = jdbc.queryForObject(
            "INSERT INTO \"chargement\" (\"datetime\") VALUES (?) RETURNING \"ID_chargement\"",
            Integer.class,
            storageDate
        );

        Integer lotId = jdbc.queryForObject(
            "INSERT INTO \"lot\" (\"ID_ENTREPOT\", \"ID_exploitation\", \"ID_chargement\", \"lot_reference\", \"date_peremption\", \"variete\", \"process\", \"score_sca\", \"poids_kg\", \"qualite\", \"quantite\") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING \"ID_LOT\"",
            Integer.class,
            idEntrepot,
            payload.idExploitation(),
            chargementId,
            payload.lotReference(),
            payload.datePeremption(),
            payload.variete(),
            payload.process(),
            payload.scoreSca(),
            payload.poidsKg(),
            payload.qualite(),
            payload.quantite()
        );

        return findLotByIdAndEntrepot(lotId, idEntrepot).orElseThrow();
    }

    public void updateLot(long lotId, int idEntrepot, LotPayload payload) {
        Map<String, Object> existing = findLotByIdAndEntrepot(lotId, idEntrepot).orElseThrow();
        int currentChargementId = asInt(existing.get("idChargement"), 0);
        int nextChargementId = currentChargementId;

        if (payload.storageDate() != null && !payload.storageDate().isBlank()) {
            LocalDate storageDate = parseStorageDate(payload.storageDate());
            Integer createdChargement = jdbc.queryForObject(
                "INSERT INTO \"chargement\" (\"datetime\") VALUES (?) RETURNING \"ID_chargement\"",
                Integer.class,
                storageDate
            );
            nextChargementId = createdChargement != null ? createdChargement : currentChargementId;
        }

        jdbc.update(
            "UPDATE \"lot\" SET \"ID_exploitation\" = ?, \"ID_chargement\" = ?, \"lot_reference\" = ?, \"date_peremption\" = ?, \"variete\" = ?, \"process\" = ?, \"score_sca\" = ?, \"poids_kg\" = ?, \"qualite\" = ?, \"quantite\" = ? " +
                "WHERE \"ID_LOT\" = ? AND \"ID_ENTREPOT\" = ?",
            payload.idExploitation() != null ? payload.idExploitation() : asNullableInt(existing.get("idExploitation")),
            nextChargementId,
            payload.lotReference() != null ? payload.lotReference() : asString(existing.get("lotReference")),
            payload.datePeremption() != null ? payload.datePeremption() : asInt(existing.get("datePeremption"), 0),
            payload.variete() != null ? payload.variete() : asString(existing.get("variete")),
            payload.process() != null ? payload.process() : asString(existing.get("process")),
            payload.scoreSca() != null ? payload.scoreSca() : asNullableDouble(existing.get("scoreSca")),
            payload.poidsKg() != null ? payload.poidsKg() : asNullableDouble(existing.get("poidsKg")),
            payload.qualite() != null ? payload.qualite() : asString(existing.get("qualite")),
            payload.quantite() != null ? payload.quantite() : asInt(existing.get("quantite"), 0),
            lotId,
            idEntrepot
        );
    }

    public void deleteLotByIdAndEntrepot(long lotId, int idEntrepot) {
        jdbc.update(
            "DELETE FROM \"lot\" WHERE \"ID_LOT\" = ? AND \"ID_ENTREPOT\" = ?",
            lotId,
            idEntrepot
        );
    }

    public List<Map<String, Object>> findExpeditionsByEntrepot(int idEntrepot) {
        return jdbc.query(
            "SELECT DISTINCT e.\"ID_expedition\" AS id, e.\"statut\" AS statut, e.\"destination_pays\" AS destination_pays, " +
                "e.\"destination_ville\" AS destination_ville, e.\"destination_client\" AS destination_client, " +
                "e.\"poids_total_kg\" AS poids_total_kg, e.\"tracking_transporteur\" AS tracking_transporteur, " +
                "e.\"quai_depart\" AS quai_depart, e.\"transporteur\" AS transporteur, e.\"livreur_nom\" AS livreur_nom, " +
                "e.\"livreur_telephone\" AS livreur_telephone, e.\"depart_at\" AS depart_at, e.\"arrivee_estimee_at\" AS arrivee_estimee_at " +
                "FROM \"expedition\" e " +
                "JOIN \"expedition_lot\" el ON el.\"ID_expedition\" = e.\"ID_expedition\" " +
                "JOIN \"lot\" l ON l.\"ID_LOT\" = el.\"ID_LOT\" " +
                "WHERE l.\"ID_ENTREPOT\" = ? " +
                "ORDER BY e.\"depart_at\" DESC",
            (rs, rowNum) -> mapExpeditionRow(rs, idEntrepot),
            idEntrepot
        );
    }

    public Optional<Map<String, Object>> findExpeditionByIdAndEntrepot(long expeditionId, int idEntrepot) {
        List<Map<String, Object>> rows = jdbc.query(
            "SELECT DISTINCT e.\"ID_expedition\" AS id, e.\"statut\" AS statut, e.\"destination_pays\" AS destination_pays, " +
                "e.\"destination_ville\" AS destination_ville, e.\"destination_client\" AS destination_client, " +
                "e.\"poids_total_kg\" AS poids_total_kg, e.\"tracking_transporteur\" AS tracking_transporteur, " +
                "e.\"quai_depart\" AS quai_depart, e.\"transporteur\" AS transporteur, e.\"livreur_nom\" AS livreur_nom, " +
                "e.\"livreur_telephone\" AS livreur_telephone, e.\"depart_at\" AS depart_at, e.\"arrivee_estimee_at\" AS arrivee_estimee_at " +
                "FROM \"expedition\" e " +
                "JOIN \"expedition_lot\" el ON el.\"ID_expedition\" = e.\"ID_expedition\" " +
                "JOIN \"lot\" l ON l.\"ID_LOT\" = el.\"ID_LOT\" " +
                "WHERE e.\"ID_expedition\" = ? AND l.\"ID_ENTREPOT\" = ?",
            (rs, rowNum) -> mapExpeditionRow(rs, idEntrepot),
            expeditionId,
            idEntrepot
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(rows.get(0));
    }

    public Map<String, Object> createExpedition(int idEntrepot, ExpeditionPayload payload) {
        Timestamp departAt = parseTimestamp(payload.departAt());
        Timestamp arriveeAt = parseOptionalTimestamp(payload.arriveeEstimeeAt());

        Integer expeditionId = jdbc.queryForObject(
            "INSERT INTO \"expedition\" (\"depart_at\", \"arrivee_estimee_at\", \"destination_pays\", \"destination_ville\", \"destination_client\", " +
                "\"poids_total_kg\", \"tracking_transporteur\", \"quai_depart\", \"transporteur\", \"livreur_nom\", \"livreur_telephone\", \"statut\") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING \"ID_expedition\"",
            Integer.class,
            departAt,
            arriveeAt,
            payload.destinationPays(),
            payload.destinationVille(),
            payload.destinationClient(),
            payload.poidsTotalKg(),
            payload.trackingTransporteur(),
            payload.quaiDepart(),
            payload.transporteur(),
            payload.livreurNom(),
            payload.livreurTelephone(),
            payload.statut()
        );

        replaceExpeditionLots(expeditionId, idEntrepot, payload.lots());
        return findExpeditionByIdAndEntrepot(expeditionId, idEntrepot).orElseThrow();
    }

    public Map<String, Object> updateExpedition(long expeditionId, int idEntrepot, ExpeditionPayload payload) {
        if (findExpeditionByIdAndEntrepot(expeditionId, idEntrepot).isEmpty()) {
            throw new IllegalStateException("Expedition introuvable");
        }

        Timestamp departAt = parseTimestamp(payload.departAt());
        Timestamp arriveeAt = parseOptionalTimestamp(payload.arriveeEstimeeAt());

        jdbc.update(
            "UPDATE \"expedition\" SET \"depart_at\" = ?, \"arrivee_estimee_at\" = ?, \"destination_pays\" = ?, \"destination_ville\" = ?, \"destination_client\" = ?, " +
                "\"poids_total_kg\" = ?, \"tracking_transporteur\" = ?, \"quai_depart\" = ?, \"transporteur\" = ?, \"livreur_nom\" = ?, \"livreur_telephone\" = ?, \"statut\" = ? " +
                "WHERE \"ID_expedition\" = ?",
            departAt,
            arriveeAt,
            payload.destinationPays(),
            payload.destinationVille(),
            payload.destinationClient(),
            payload.poidsTotalKg(),
            payload.trackingTransporteur(),
            payload.quaiDepart(),
            payload.transporteur(),
            payload.livreurNom(),
            payload.livreurTelephone(),
            payload.statut(),
            expeditionId
        );

        replaceExpeditionLots(expeditionId, idEntrepot, payload.lots());
        return findExpeditionByIdAndEntrepot(expeditionId, idEntrepot).orElseThrow();
    }

    public void deleteExpeditionByIdAndEntrepot(long expeditionId, int idEntrepot) {
        if (findExpeditionByIdAndEntrepot(expeditionId, idEntrepot).isEmpty()) {
            throw new IllegalStateException("Expedition introuvable");
        }

        jdbc.update("DELETE FROM \"expedition_lot\" WHERE \"ID_expedition\" = ?", expeditionId);
        jdbc.update("DELETE FROM \"expedition\" WHERE \"ID_expedition\" = ?", expeditionId);
    }

    private List<Map<String, Object>> findExpeditionLots(long expeditionId, int idEntrepot) {
        return jdbc.query(
                "SELECT l.\"ID_LOT\" AS lot_id, l.\"lot_reference\" AS lot_reference, el.\"quantite_expediee\" AS quantite_expediee, " +
                "l.\"poids_kg\" AS poids_lot_kg, l.\"quantite\" AS quantite_lot " +
                "FROM \"expedition_lot\" el " +
                "JOIN \"lot\" l ON l.\"ID_LOT\" = el.\"ID_LOT\" " +
                "WHERE el.\"ID_expedition\" = ? AND l.\"ID_ENTREPOT\" = ? " +
                "ORDER BY l.\"ID_LOT\"",
            (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                int quantiteLot = rs.getInt("quantite_lot");
                int quantiteExpediee = rs.getInt("quantite_expediee");
                Double poidsLot = toDouble(rs.getBigDecimal("poids_lot_kg"));
                double poidsExpedie = 0.0;
                if (poidsLot != null && quantiteLot > 0) {
                    poidsExpedie = (quantiteExpediee * poidsLot) / quantiteLot;
                }

                row.put("lotId", rs.getLong("lot_id"));
                row.put("lotReference", rs.getString("lot_reference"));
                row.put("quantiteExpediee", quantiteExpediee);
                row.put("poidsExpedieKg", poidsExpedie);
                return row;
            },
            expeditionId,
            idEntrepot
        );
    }

    private void replaceExpeditionLots(long expeditionId, int idEntrepot, List<ExpeditionLotPayload> lots) {
        jdbc.update("DELETE FROM \"expedition_lot\" WHERE \"ID_expedition\" = ?", expeditionId);

        if (lots == null) {
            return;
        }

        for (ExpeditionLotPayload lot : lots) {
            if (lot == null || lot.lotId() == null || lot.quantiteExpediee() == null) {
                continue;
            }

            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"lot\" WHERE \"ID_LOT\" = ? AND \"ID_ENTREPOT\" = ?",
                Integer.class,
                lot.lotId(),
                idEntrepot
            );

            if (count == null || count == 0) {
                continue;
            }

            jdbc.update(
                "INSERT INTO \"expedition_lot\" (\"ID_expedition\", \"ID_LOT\", \"quantite_expediee\") VALUES (?, ?, ?)",
                expeditionId,
                lot.lotId(),
                lot.quantiteExpediee()
            );
        }
    }

    private Map<String, Object> mapExpeditionRow(java.sql.ResultSet rs, int idEntrepot) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("statut", rs.getString("statut"));
        row.put("destinationPays", rs.getString("destination_pays"));
        row.put("destinationVille", rs.getString("destination_ville"));
        row.put("destinationClient", rs.getString("destination_client"));
        row.put("poidsTotalKg", toDouble(rs.getBigDecimal("poids_total_kg")));
        row.put("trackingTransporteur", rs.getString("tracking_transporteur"));
        row.put("quaiDepart", rs.getString("quai_depart"));
        row.put("transporteur", rs.getString("transporteur"));
        row.put("livreurNom", rs.getString("livreur_nom"));
        row.put("livreurTelephone", rs.getString("livreur_telephone"));
        Timestamp departAt = rs.getTimestamp("depart_at");
        Timestamp arriveeAt = rs.getTimestamp("arrivee_estimee_at");
        row.put("departAt", departAt != null ? departAt.toInstant().toString() : "");
        row.put("arriveeEstimeeAt", arriveeAt != null ? arriveeAt.toInstant().toString() : "");
        row.put("lots", findExpeditionLots(rs.getLong("id"), idEntrepot));
        return row;
    }

    private Timestamp parseTimestamp(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) {
            return Timestamp.valueOf(LocalDateTime.now());
        }

        return Timestamp.valueOf(LocalDateTime.parse(rawDateTime));
    }

    private Timestamp parseOptionalTimestamp(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) {
            return null;
        }

        return Timestamp.valueOf(LocalDateTime.parse(rawDateTime));
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private Map<String, Object> mapLotRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("idChargement", rs.getInt("id_chargement"));
        int idExploitation = rs.getInt("id_exploitation");
        row.put("idExploitation", rs.wasNull() ? null : idExploitation);
        row.put("lotReference", rs.getString("lot_reference"));
        row.put("storageDate", rs.getDate("storage_date") != null ? rs.getDate("storage_date").toString() : "");
        row.put("datePeremption", rs.getInt("date_peremption"));
        row.put("variete", rs.getString("variete"));
        row.put("process", rs.getString("process"));
        row.put("scoreSca", toDouble(rs.getBigDecimal("score_sca")));
        row.put("poidsKg", toDouble(rs.getBigDecimal("poids_kg")));
        row.put("qualite", rs.getString("qualite"));
        row.put("quantite", rs.getInt("quantite"));
        return row;
    }

    private LocalDate parseStorageDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(rawDate);
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Integer asNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double asNullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
