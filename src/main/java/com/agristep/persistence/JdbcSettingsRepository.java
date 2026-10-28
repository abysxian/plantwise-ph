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
        long userId = session.requireUserId();
        db.run(() -> db.withConnection(c -> {
            int changed = Sql.update(c, "UPDATE garden_settings SET location_name = ?, latitude = ?, longitude = ?, soil = ? WHERE user_id = ?",
                    s.getLocationName(), s.getLatitude(), s.getLongitude(), s.getSoil().name(), userId);
            if (changed == 0) {
                Sql.update(c, "INSERT INTO garden_settings (id, user_id, location_name, latitude, longitude, soil) VALUES (?,?,?,?,?,?)",
                        userId, userId, s.getLocationName(), s.getLatitude(), s.getLongitude(), s.getSoil().name());
            }
            return null;
        }));
        return s;
    }

    @Override
    public Optional<GardenSettings> findCurrent() {
        long userId = session.requireUserId();
        return db.withConnection(c -> Sql.query(c,
                "SELECT location_name, latitude, longitude, soil FROM garden_settings WHERE user_id = ?",
                rs -> new GardenSettings(rs.getString("location_name"), rs.getDouble("latitude"),
                        rs.getDouble("longitude"), SoilType.valueOf(rs.getString("soil"))), userId)).stream().findFirst();
    }
}
