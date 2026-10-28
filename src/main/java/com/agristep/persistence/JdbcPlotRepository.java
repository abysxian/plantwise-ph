package com.agristep.persistence;

import com.agristep.domain.CareTask;
import com.agristep.domain.Plot;
import com.agristep.domain.NotFoundException;
import com.agristep.domain.TaskKind;
import com.agristep.repository.CropRepository;
import com.agristep.repository.PlotRepository;
import com.agristep.service.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Saves beds together with their tasks (a bed owns its tasks, so they are saved and deleted with it).
 * Every query is limited to the signed-in user's own beds.
 */
public class JdbcPlotRepository implements PlotRepository {

    private record Row(long id, int slot, String name, Long cropId, LocalDate plantedOn) {}

    private static final String TASK_COLUMNS =
            "id, plot_id, task_type, day_offset, due_date, amount, detail, extra, completed, completed_at";

    private final Database db;
    private final CropRepository crops;
    private final Session session;

    public JdbcPlotRepository(Database db, CropRepository crops, Session session) {
        this.db = db;
        this.crops = crops;
        this.session = session;
    }

    @Override
    public Plot save(Plot plot) {
        if (!plot.isEmpty() && plot.getCrop().getId() == null) {
            throw new IllegalStateException("The crop must be saved before it can be planted.");
        }
        return db.run(() -> db.withConnection(c -> {
            Long cropId = plot.isEmpty() ? null : plot.getCrop().getId();
            if (plot.getId() == null) {
                plot.assignId(Sql.insert(c, "INSERT INTO plots (user_id, slot_number, name, crop_id, planted_on) VALUES (?,?,?,?,?)",
                        session.requireUserId(), plot.getSlotNumber(), plot.getName(), cropId, plot.getPlantedOn()));
            } else {
                int changed = Sql.update(c, "UPDATE plots SET name = ?, crop_id = ?, planted_on = ? WHERE id = ? AND user_id = ?",
                        plot.getName(), cropId, plot.getPlantedOn(), plot.getId(), session.requireUserId());
                if (changed == 0) throw new NotFoundException("That bed does not exist.");
            }
            saveTasks(c, plot);
            return plot;
        }));
    }

    private void saveTasks(Connection c, Plot plot) throws SQLException {
        Set<Long> keep = new HashSet<>();
        for (CareTask task : plot.getTasks()) {
            if (task.getId() == null) {
                task.assignId(Sql.insert(c,
                        "INSERT INTO care_tasks (plot_id, task_type, day_offset, due_date, amount, detail, extra, completed, completed_at)"
                                + " VALUES (?,?,?,?,?,?,?,?,?)",
                        plot.getId(), task.getKind().name(), task.getDayOffset(), task.getDueDate(), task.getAmount(),
                        task.getDetail(), task.getExtra(), task.isCompleted(), task.getCompletedAt()));
            } else {
                Sql.update(c, "UPDATE care_tasks SET completed = ?, completed_at = ? WHERE id = ?",
                        task.isCompleted(), task.getCompletedAt(), task.getId());
            }
            keep.add(task.getId());
        }
        // tasks that were removed from the bed (it was cleared) are deleted from the database
        for (Long existing : Sql.query(c, "SELECT id FROM care_tasks WHERE plot_id = ?", rs -> rs.getLong(1), plot.getId())) {
            if (!keep.contains(existing)) {
                Sql.update(c, "DELETE FROM care_tasks WHERE id = ?", existing);
            }
        }
    }

    @Override public Optional<Plot> findById(Long id) { return load("AND id = ?", "", id).stream().findFirst(); }

    @Override public List<Plot> findAll() { return findAllByOrderBySlotNumberAsc(); }

    @Override public List<Plot> findAllByOrderBySlotNumberAsc() { return load("", "ORDER BY slot_number"); }

    @Override public Optional<Plot> findTopByOrderBySlotNumberDesc() {
        return load("", "ORDER BY slot_number DESC LIMIT 1").stream().findFirst();
    }

    @Override public void delete(Plot plot) {
        long userId = session.requireUserId();
        db.run(() -> db.withConnection(c -> {
            Sql.update(c, "DELETE FROM care_tasks WHERE plot_id IN (SELECT id FROM plots WHERE id = ? AND user_id = ?)", plot.getId(), userId);
            Sql.update(c, "DELETE FROM plots WHERE id = ? AND user_id = ?", plot.getId(), userId);
            return null;
        }));
    }

    @Override public long count() {
        return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM plots WHERE user_id = ?", session.requireUserId()));
    }

    /** {@code extra} is appended to the "belongs to the signed-in user" condition, e.g. "AND id = ?". */
    private List<Plot> load(String extra, String orderBy, Object... params) {
        long userId = session.requireUserId();
        Object[] all = new Object[params.length + 1];
        all[0] = userId;
        System.arraycopy(params, 0, all, 1, params.length);
        return db.withConnection(c -> {
            List<Row> rows = Sql.query(c, "SELECT id, slot_number, name, crop_id, planted_on FROM plots WHERE user_id = ? "
                            + extra + " " + orderBy,
                    rs -> new Row(rs.getLong("id"), rs.getInt("slot_number"), rs.getString("name"),
                            rs.getObject("crop_id", Long.class), Sql.date(rs, "planted_on")), all);
            if (rows.isEmpty()) return List.<Plot>of();

            Map<Long, List<CareTask>> tasksByPlot = loadTasks(c, rows);
            List<Plot> plots = new ArrayList<>();
            for (Row row : rows) {
                var crop = row.cropId() == null ? null : crops.findById(row.cropId()).orElseThrow(
                        () -> new DataAccessException("Bed " + row.slot() + " refers to a crop that no longer exists.", null));
                plots.add(Plot.restore(row.id(), row.slot(), row.name(), crop, row.plantedOn(),
                        tasksByPlot.getOrDefault(row.id(), List.of())));
            }
            return plots;
        });
    }

    private Map<Long, List<CareTask>> loadTasks(Connection c, List<Row> rows) throws SQLException {
        String marks = String.join(",", Collections.nCopies(rows.size(), "?"));
        Object[] ids = rows.stream().map(Row::id).toArray();
        Map<Long, List<CareTask>> byPlot = new HashMap<>();
        Sql.query(c, "SELECT " + TASK_COLUMNS + " FROM care_tasks WHERE plot_id IN (" + marks + ") ORDER BY due_date, id", rs -> {
            CareTask task = CareTask.restore(rs.getLong("id"), TaskKind.valueOf(rs.getString("task_type")),
                    rs.getInt("day_offset"), Sql.date(rs, "due_date"), Sql.nullableDouble(rs, "amount"),
                    rs.getString("detail"), rs.getString("extra"), rs.getBoolean("completed"), Sql.dateTime(rs, "completed_at"));
            byPlot.computeIfAbsent(rs.getLong("plot_id"), k -> new ArrayList<>()).add(task);
            return null;
        }, ids);
        return byPlot;
    }
}
