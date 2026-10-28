package com.agristep.persistence;

import com.agristep.domain.JournalEntry;
import com.agristep.repository.JournalRepository;
import com.agristep.service.Session;

import java.util.List;
import java.util.Optional;

public class JdbcJournalRepository implements JournalRepository {
    private static final String SELECT =
            "SELECT id, author_name, plot_name, crop_name, entry_date, note, photo_file FROM journal_entries WHERE user_id = ? ";

    private final Database db;
    private final Session session;

    public JdbcJournalRepository(Database db, Session session) {
        this.db = db;
        this.session = session;
    }

    @Override public JournalEntry save(JournalEntry e) {
        if (e.getId() != null) return e;
        db.withConnection(c -> {
            e.assignId(Sql.insert(c,
                    "INSERT INTO journal_entries (user_id, author_name, plot_name, crop_name, entry_date, note, photo_file) VALUES (?,?,?,?,?,?,?)",
                    session.requireGardenId(), e.getAuthorName(), e.getPlotName(), e.getCropName(), e.getDate(), e.getNote(), e.getPhotoFile()));
            return null;
        });
        return e;
    }

    @Override public List<JournalEntry> findAllNewestFirst() { return query("ORDER BY entry_date DESC, id DESC"); }

    @Override public Optional<JournalEntry> findById(long id) { return query("AND id = ?", id).stream().findFirst(); }

    @Override public void deleteById(long id) {
        db.withConnection(c -> Sql.update(c, "DELETE FROM journal_entries WHERE id = ? AND user_id = ?", id, session.requireGardenId()));
    }

    private List<JournalEntry> query(String tail, Object... params) {
        Object[] all = new Object[params.length + 1];
        all[0] = session.requireGardenId();
        System.arraycopy(params, 0, all, 1, params.length);
        return db.withConnection(c -> Sql.query(c, SELECT + tail, rs -> {
            JournalEntry e = new JournalEntry(rs.getString("author_name"), rs.getString("plot_name"), rs.getString("crop_name"),
                    Sql.date(rs, "entry_date"), rs.getString("note"), rs.getString("photo_file"));
            e.assignId(rs.getLong("id"));
            return e;
        }, all));
    }
}
