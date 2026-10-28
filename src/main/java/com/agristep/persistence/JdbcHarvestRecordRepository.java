package com.agristep.persistence;

import com.agristep.domain.HarvestRecord;
import com.agristep.repository.HarvestRecordRepository;
import com.agristep.service.Session;

import java.util.List;
import java.util.Optional;

public class JdbcHarvestRecordRepository implements HarvestRecordRepository {
    private static final String SELECT =
            "SELECT id, crop_name, crop_emoji, plot_name, planted_on, harvested_on, completion_percent FROM harvest_records WHERE user_id = ? ";

    private final Database db;
    private final Session session;

    public JdbcHarvestRecordRepository(Database db, Session session) {
        this.db = db;
        this.session = session;
    }

    @Override
    public HarvestRecord save(HarvestRecord record) {
        if (record.getId() != null) return record;      // history is never edited
        db.withConnection(c -> {
            record.assignId(Sql.insert(c,
                    "INSERT INTO harvest_records (user_id, crop_name, crop_emoji, plot_name, planted_on, harvested_on, completion_percent)"
                            + " VALUES (?,?,?,?,?,?,?)",
                    session.requireUserId(), record.getCropName(), record.getCropEmoji(), record.getPlotName(),
                    record.getPlantedOn(), record.getHarvestedOn(), record.getCompletionPercent()));
            return null;
        });
        return record;
    }

    @Override public Optional<HarvestRecord> findById(Long id) {
        return query("AND id = ?", id).stream().findFirst();
    }

    @Override public List<HarvestRecord> findAll() { return query("ORDER BY id"); }

    @Override public long count() {
        return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM harvest_records WHERE user_id = ?", session.requireUserId()));
    }

    @Override public List<HarvestRecord> findTop10ByOrderByHarvestedOnDescIdDesc() {
        return query("ORDER BY harvested_on DESC, id DESC LIMIT 10");
    }

    /** {@code tail} continues the "belongs to the signed-in user" condition, e.g. "AND id = ?" or "ORDER BY id". */
    private List<HarvestRecord> query(String tail, Object... params) {
        Object[] all = new Object[params.length + 1];
        all[0] = session.requireUserId();
        System.arraycopy(params, 0, all, 1, params.length);
        return db.withConnection(c -> Sql.query(c, SELECT + tail, rs -> {
            HarvestRecord r = new HarvestRecord(rs.getString("crop_name"), rs.getString("crop_emoji"), rs.getString("plot_name"),
                    Sql.date(rs, "planted_on"), Sql.date(rs, "harvested_on"), rs.getInt("completion_percent"));
            r.assignId(rs.getLong("id"));
            return r;
        }, all));
    }
}
