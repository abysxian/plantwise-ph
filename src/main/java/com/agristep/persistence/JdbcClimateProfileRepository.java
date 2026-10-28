package com.agristep.persistence;

import com.agristep.domain.ClimateProfile;
import com.agristep.repository.ClimateProfileRepository;
import com.agristep.weather.ClimateStats;

import java.util.List;
import java.util.Optional;

public class JdbcClimateProfileRepository implements ClimateProfileRepository {
    private static final String SELECT =
            "SELECT id, location_key, location_name, avg_temp_30d_c, annual_rain_mm, fetched_at FROM climate_profiles ";

    private final Database db;

    public JdbcClimateProfileRepository(Database db) {
        this.db = db;
    }

    @Override
    public ClimateProfile save(ClimateProfile p) {
        db.withConnection(c -> {
            ClimateStats stats = p.toStats();
            if (p.getId() == null) {
                p.assignId(Sql.insert(c,
                        "INSERT INTO climate_profiles (location_key, location_name, avg_temp_30d_c, annual_rain_mm, fetched_at) VALUES (?,?,?,?,?)",
                        p.getLocationKey(), p.getLocationName(), stats.avgTemp30dC(), stats.annualRainMm(), p.getFetchedAt()));
            } else {
                Sql.update(c, "UPDATE climate_profiles SET location_name = ?, avg_temp_30d_c = ?, annual_rain_mm = ?, fetched_at = ? WHERE id = ?",
                        p.getLocationName(), stats.avgTemp30dC(), stats.annualRainMm(), p.getFetchedAt(), p.getId());
            }
            return null;
        });
        return p;
    }

    @Override public Optional<ClimateProfile> findByLocationKey(String key) { return query("WHERE location_key = ?", key).stream().findFirst(); }

    @Override public Optional<ClimateProfile> findById(Long id) { return query("WHERE id = ?", id).stream().findFirst(); }

    @Override public List<ClimateProfile> findAll() { return query("ORDER BY id"); }

    @Override public long count() { return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM climate_profiles")); }

    private List<ClimateProfile> query(String tail, Object... params) {
        return db.withConnection(c -> Sql.query(c, SELECT + tail, rs -> {
            ClimateProfile p = new ClimateProfile(rs.getString("location_key"), rs.getString("location_name"),
                    new ClimateStats(rs.getDouble("avg_temp_30d_c"), rs.getDouble("annual_rain_mm")), Sql.dateTime(rs, "fetched_at"));
            p.assignId(rs.getLong("id"));
            return p;
        }, params));
    }
}
