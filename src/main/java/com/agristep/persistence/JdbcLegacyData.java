package com.agristep.persistence;

import com.agristep.repository.LegacyData;

public class JdbcLegacyData implements LegacyData {
    private final Database db;

    public JdbcLegacyData(Database db) {
        this.db = db;
    }

    @Override public boolean exists() {
        return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM plots WHERE user_id IS NULL") > 0);
    }

    @Override public void assignTo(long userId) {
        db.withConnection(c -> {
            Sql.update(c, "UPDATE plots SET user_id = ? WHERE user_id IS NULL", userId);
            Sql.update(c, "UPDATE harvest_records SET user_id = ? WHERE user_id IS NULL", userId);
            // the settings row's id is the owner's id, so it moves along with the owner
            Sql.update(c, "UPDATE garden_settings SET id = ?, user_id = ? WHERE user_id IS NULL", userId, userId);
            return null;
        });
    }
}
