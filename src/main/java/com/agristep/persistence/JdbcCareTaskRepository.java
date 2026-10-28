package com.agristep.persistence;

import com.agristep.domain.CareTask;
import com.agristep.repository.CareTaskRepository;
import com.agristep.service.Session;

import java.time.LocalDate;

public class JdbcCareTaskRepository implements CareTaskRepository {
    private final Database db;
    private final Session session;

    public JdbcCareTaskRepository(Database db, Session session) {
        this.db = db;
        this.session = session;
    }

    @Override
    public CareTask save(CareTask task) {
        if (task.getId() == null) {
            throw new IllegalStateException("Tasks are created by saving their bed.");
        }
        db.withConnection(c -> Sql.update(c, "UPDATE care_tasks SET completed = ?, completed_at = ? WHERE id = ? "
                        + "AND plot_id IN (SELECT id FROM plots WHERE user_id = ?)",
                task.isCompleted(), task.getCompletedAt(), task.getId(), session.requireUserId()));
        return task;
    }

    @Override
    public long countByCompletedFalseAndDueDateLessThanEqual(LocalDate date) {
        return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM care_tasks t JOIN plots p ON p.id = t.plot_id "
                + "WHERE p.user_id = ? AND t.completed = ? AND t.due_date <= ?", session.requireUserId(), false, date));
    }
}
