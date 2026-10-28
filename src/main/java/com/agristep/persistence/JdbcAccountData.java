package com.agristep.persistence;

import com.agristep.repository.AccountData;

import java.util.List;

public class JdbcAccountData implements AccountData {
    private final Database db;

    public JdbcAccountData(Database db) {
        this.db = db;
    }

    @Override public List<String> journalPhotoFiles(long gardenOwnerId) {
        return db.withConnection(c -> Sql.query(c,
                "SELECT photo_file FROM journal_entries WHERE user_id = ? AND photo_file IS NOT NULL",
                rs -> rs.getString(1), gardenOwnerId));
    }

    @Override public void deleteAccount(long userId) {
        db.run(() -> db.withConnection(c -> {
            Sql.update(c, "DELETE FROM care_tasks WHERE plot_id IN (SELECT id FROM plots WHERE user_id = ?)", userId);
            Sql.update(c, "DELETE FROM plots WHERE user_id = ?", userId);
            Sql.update(c, "DELETE FROM harvest_records WHERE user_id = ?", userId);
            Sql.update(c, "DELETE FROM garden_settings WHERE user_id = ? OR id = ?", userId, userId);
            Sql.update(c, "DELETE FROM journal_entries WHERE user_id = ?", userId);
            Sql.update(c, "DELETE FROM garden_members WHERE member_id = ? OR owner_id = ?", userId, userId);
            Sql.update(c, "DELETE FROM garden_invites WHERE owner_id = ? OR invitee_id = ?", userId, userId);
            Sql.update(c, "DELETE FROM users WHERE id = ?", userId);
            return null;
        }));
    }
}
