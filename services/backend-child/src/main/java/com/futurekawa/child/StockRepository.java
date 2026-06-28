package com.futurekawa.child;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepository {

    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findLotsByEntrepot(int idEntrepot) {
        return jdbc.query(
            "SELECT l.\"ID_LOT\" AS id, l.\"lot_reference\" AS lot_reference, c.\"datetime\" AS storage_date, " +
                "l.\"date_peremption\" AS date_peremption, l.\"variete\" AS variete, l.\"process\" AS process, " +
                "l.\"score_sca\" AS score_sca, l.\"poids_kg\" AS poids_kg, l.\"qualite\" AS qualite, l.\"quantite\" AS quantite " +
                "FROM \"lot\" l " +
                "LEFT JOIN \"chargement\" c ON c.\"ID_chargement\" = l.\"ID_chargement\" " +
                "WHERE l.\"ID_ENTREPOT\" = ? " +
                "ORDER BY c.\"datetime\" DESC NULLS LAST, l.\"ID_LOT\" DESC",
            (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
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
            },
            idEntrepot
        );
    }

    public List<Map<String, Object>> findExpeditionsByEntrepot(int idEntrepot) {
        List<Map<String, Object>> expeditions = jdbc.query(
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
            (rs, rowNum) -> {
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
            },
            idEntrepot
        );

        return expeditions;
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

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
