package com.agristep.persistence;

import com.agristep.domain.*;
import com.agristep.repository.CropRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Crops rarely change while the app runs, so they are read once and kept in memory.
 * The cache is cleared whenever a new crop is saved.
 */
public class JdbcCropRepository implements CropRepository {

    private final Database db;
    private volatile List<Crop> cache;

    public JdbcCropRepository(Database db) {
        this.db = db;
    }

    @Override
    public Crop save(Crop crop) {
        if (crop.getId() != null) return crop;          // crops are added, not edited, at runtime
        db.run(() -> db.withConnection(c -> { insert(c, crop); return null; }));
        cache = null;
        return crop;
    }

    @Override public Optional<Crop> findById(Long id) { return all().stream().filter(c -> id.equals(c.getId())).findFirst(); }

    @Override public Optional<Crop> findByCode(String code) { return all().stream().filter(c -> c.getCode().equals(code)).findFirst(); }

    @Override public List<Crop> findAll() { return all(); }

    @Override public List<Crop> findAllByOrderByNameAsc() {
        return all().stream().sorted(Comparator.comparing(Crop::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override public long count() { return all().size(); }

    private List<Crop> all() {
        List<Crop> loaded = cache;
        if (loaded == null) {
            loaded = List.copyOf(db.withConnection(this::load));
            cache = loaded;
        }
        return loaded;
    }

    // ---- writing ----

    private void insert(Connection c, Crop crop) throws SQLException {
        long id = Sql.insert(c,
                "INSERT INTO crops (code, name, english_name, scientific_name, emoji, cycle_days, sunlight, spacing, verified,"
                        + " source_note, source_url, temp_abs_min, temp_opt_min, temp_opt_max, temp_abs_max,"
                        + " rain_abs_min, rain_opt_min, rain_opt_max, rain_abs_max, ph_abs_min, ph_opt_min, ph_opt_max, ph_abs_max)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                crop.getCode(), crop.getName(), crop.getEnglishName(), crop.getScientificName(), crop.getEmoji(),
                crop.getCycleDays(), crop.getSunlight(), crop.getSpacing(), crop.isVerified(),
                crop.getSourceNote(), crop.getSourceUrl(),
                crop.getTemperature().getAbsMin(), crop.getTemperature().getOptMin(), crop.getTemperature().getOptMax(), crop.getTemperature().getAbsMax(),
                crop.getRainfall().getAbsMin(), crop.getRainfall().getOptMin(), crop.getRainfall().getOptMax(), crop.getRainfall().getAbsMax(),
                crop.getPh().getAbsMin(), crop.getPh().getOptMin(), crop.getPh().getOptMax(), crop.getPh().getAbsMax());
        crop.assignId(id);
        for (SoilType soil : crop.getOptimalSoils()) {
            Sql.update(c, "INSERT INTO crop_optimal_soils (crop_id, soil) VALUES (?, ?)", id, soil.name());
        }
        for (SoilType soil : crop.getTolerableSoils()) {
            Sql.update(c, "INSERT INTO crop_tolerable_soils (crop_id, soil) VALUES (?, ?)", id, soil.name());
        }
        for (CropTip tip : crop.getTips()) {
            Sql.update(c, "INSERT INTO crop_tips (crop_id, category, content) VALUES (?, ?, ?)",
                    id, tip.getCategory().name(), tip.getContent());
        }
        for (TaskTemplate t : crop.getTaskTemplates()) {
            Sql.update(c, "INSERT INTO task_templates (crop_id, kind, day_offset, amount, detail, extra) VALUES (?,?,?,?,?,?)",
                    id, t.getKind().name(), t.getDayOffset(), t.getAmount(), t.getDetail(), t.getExtra());
        }
    }

    // ---- reading ----

    private List<Crop> load(Connection c) throws SQLException {
        Map<Long, Set<SoilType>> optimal = soils(c, "crop_optimal_soils");
        Map<Long, Set<SoilType>> tolerable = soils(c, "crop_tolerable_soils");

        Map<Long, Crop> byId = new LinkedHashMap<>();
        for (Crop crop : Sql.query(c, "SELECT * FROM crops ORDER BY id", rs -> {
            Crop built = new Crop(rs.getString("code"), rs.getString("name"), rs.getString("english_name"),
                    rs.getString("scientific_name"), rs.getString("emoji"), rs.getInt("cycle_days"),
                    range(rs, "temp"), range(rs, "rain"), range(rs, "ph"))
                    .withGrowingNotes(rs.getString("sunlight"), rs.getString("spacing"))
                    .withSource(rs.getBoolean("verified"), rs.getString("source_note"), rs.getString("source_url"));
            long id = rs.getLong("id");
            built.withSoils(optimal.getOrDefault(id, Set.of()), tolerable.getOrDefault(id, Set.of()));
            built.assignId(id);
            return built;
        })) {
            byId.put(crop.getId(), crop);
        }

        Sql.query(c, "SELECT crop_id, category, content FROM crop_tips ORDER BY id", rs -> {
            Crop crop = byId.get(rs.getLong("crop_id"));
            if (crop != null) crop.addTip(TipCategory.valueOf(rs.getString("category")), rs.getString("content"));
            return null;
        });
        Sql.query(c, "SELECT crop_id, kind, day_offset, amount, detail, extra FROM task_templates ORDER BY day_offset, id", rs -> {
            Crop crop = byId.get(rs.getLong("crop_id"));
            if (crop != null) {
                crop.addTaskTemplate(TaskKind.valueOf(rs.getString("kind")), rs.getInt("day_offset"),
                        Sql.nullableDouble(rs, "amount"), rs.getString("detail"), rs.getString("extra"));
            }
            return null;
        });
        return new ArrayList<>(byId.values());
    }

    private static ClimateRange range(java.sql.ResultSet rs, String prefix) throws SQLException {
        return new ClimateRange(rs.getDouble(prefix + "_abs_min"), rs.getDouble(prefix + "_opt_min"),
                rs.getDouble(prefix + "_opt_max"), rs.getDouble(prefix + "_abs_max"));
    }

    private static Map<Long, Set<SoilType>> soils(Connection c, String table) throws SQLException {
        Map<Long, Set<SoilType>> map = new HashMap<>();
        Sql.query(c, "SELECT crop_id, soil FROM " + table, rs -> {
            map.computeIfAbsent(rs.getLong("crop_id"), k -> new HashSet<>()).add(SoilType.valueOf(rs.getString("soil")));
            return null;
        });
        return map;
    }
}
