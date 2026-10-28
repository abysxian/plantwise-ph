package com.agristep.persistence;

import com.agristep.domain.GardenSettings;
import com.agristep.domain.SoilType;
import com.agristep.repository.GardenSettingsRepository;
import com.agristep.service.Session;

import java.util.Optional;

/** One settings row per user. The row's id is the owner's user id. */
public class JdbcSettingsRepository implements GardenSettingsRepository {
    private final Database db;
    private final Session session;

    public JdbcSettingsRepository(Database db, Session session) {
        this.db = db;
        this.session = session;
    }

    @Override
    public GardenSettings save(GardenSettings s) {
        long userId = session.requireGardenId();
        db.run(() -> db.withConnection(c -> {
            int changed = Sql.update(c, "UPDATE garden_settings SET location_name = ?, latitude = ?, longitude = ?, soil = ?, grid_columns = ? WHERE user_id = ?",
                    s.getLocationName(), s.getLatitude(), s.getLongitude(), s.getSoil().name(), s.getGridColumns(), userId);
            if (changed == 0) {
                Sql.update(c, "INSERT INTO garden_settings (id, user_id, location_name, latitude, longitude, soil, grid_columns) VALUES (?,?,?,?,?,?,?)",
                        userId, userId, s.getLocationName(), s.getLatitude(), s.getLongitude(), s.getSoil().name(), s.getGridColumns());
            }
            return null;
        }));
        return s;
    }

    @Override
    public Optional<GardenSettings> findCurrent() {
        long userId = session.requireGardenId();
        return db.withConnection(c -> Sql.query(c,
                "SELECT location_name, latitude, longitude, soil, grid_columns FROM garden_settings WHERE user_id = ?",
                rs -> {
                    GardenSettings g = new GardenSettings(rs.getString("location_name"), rs.getDouble("latitude"),
                            rs.getDouble("longitude"), SoilType.valueOf(rs.getString("soil")));
                    g.useGridColumns(rs.getInt("grid_columns"));
                    return g;
                }, userId)).stream().findFirst();
    }
}
