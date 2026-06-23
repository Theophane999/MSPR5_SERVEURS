package com.futurekawa.child;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CapteurRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<CapteurRecord> ROW_MAPPER = (rs, rowNum) -> new CapteurRecord(
        rs.getLong("ID_capteur"),
        rs.getObject("humidité", Double.class),
        rs.getObject("temperature", Double.class),
        rs.getObject("date", LocalDate.class),
        rs.getInt("ID_entrepot")
    );

    public CapteurRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(double humidite, double temperature, int idEntrepot) {
        jdbc.update(
            "INSERT INTO \"capteur\" (\"humidité\", \"temperature\", \"date\", \"ID_entrepot\") VALUES (?, ?, ?, ?)",
            humidite, temperature, LocalDate.now(), idEntrepot
        );
    }

    public Optional<CapteurRecord> findLatestByEntrepot(int idEntrepot) {
        List<CapteurRecord> results = jdbc.query(
            "SELECT * FROM \"capteur\" WHERE \"ID_entrepot\" = ? ORDER BY \"ID_capteur\" DESC LIMIT 1",
            ROW_MAPPER, idEntrepot
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<CapteurRecord> findRecentByEntrepot(int idEntrepot) {
        return jdbc.query(
            "SELECT * FROM \"capteur\" WHERE \"ID_entrepot\" = ? ORDER BY \"ID_capteur\" DESC LIMIT 100",
            ROW_MAPPER, idEntrepot
        );
    }
}
