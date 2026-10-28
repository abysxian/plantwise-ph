package com.agristep.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrades a database made by the earlier single-garden version so that beds, history and settings can
 * belong to a user. Rows that already exist keep user_id = NULL until the first account takes them over.
 * A database that already has the user_id columns (a new one) is left alone.
 */
final class SchemaMigrator {
    private SchemaMigrator() {}

    static void migrate(Connection c) throws SQLException {
        boolean upgrading = !hasColumn(c, "plots", "user_id");
        if (hasColumn(c, "harvest_records", "user_id") == false) execute(c, "ALTER TABLE harvest_records ADD COLUMN user_id BIGINT");
        if (hasColumn(c, "garden_settings", "user_id") == false) execute(c, "ALTER TABLE garden_settings ADD COLUMN user_id BIGINT");
        if (!hasColumn(c, "garden_settings", "grid_columns")) execute(c, "ALTER TABLE garden_settings ADD COLUMN grid_columns INT NOT NULL DEFAULT 3");
        if (!hasColumn(c, "climate_profiles", "monthly_temp")) execute(c, "ALTER TABLE climate_profiles ADD COLUMN monthly_temp VARCHAR(300)");
        if (!hasColumn(c, "climate_profiles", "monthly_rain")) execute(c, "ALTER TABLE climate_profiles ADD COLUMN monthly_rain VARCHAR(300)");
        if (upgrading) {
            execute(c, "ALTER TABLE plots ADD COLUMN user_id BIGINT");
            // slot numbers used to be unique across the whole table; now they are unique per user
            for (String drop : dropStatementsForSlotOnlyUniqueness(c)) execute(c, drop);
            execute(c, "ALTER TABLE plots ADD CONSTRAINT uq_plots_user_slot UNIQUE (user_id, slot_number)");
        }
    }

    private static boolean hasColumn(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        for (String t : new String[] {table, table.toUpperCase(), table.toLowerCase()}) {
            for (String col : new String[] {column, column.toUpperCase(), column.toLowerCase()}) {
                try (ResultSet rs = meta.getColumns(null, null, t, col)) {
                    if (rs.next()) return true;
                }
            }
        }
        return false;
    }

    /** Finds the unique key that covers only slot_number and returns the statements that remove it. */
    private static List<String> dropStatementsForSlotOnlyUniqueness(Connection c) throws SQLException {
        List<String> statements = new ArrayList<>();
        boolean h2 = c.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
        if (h2) {
            List<String> names = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                         + "WHERE UPPER(TABLE_NAME) = 'PLOTS' AND CONSTRAINT_TYPE = 'UNIQUE'")) {
                while (rs.next()) names.add(rs.getString(1));
            }
            for (String name : names) {
                List<String> columns = new ArrayList<>();
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                             + "WHERE UPPER(TABLE_NAME) = 'PLOTS' AND CONSTRAINT_NAME = '" + name.replace("'", "''") + "'")) {
                    while (rs.next()) columns.add(rs.getString(1));
                }
                if (columns.size() == 1 && columns.get(0).equalsIgnoreCase("slot_number")) {
                    statements.add("ALTER TABLE plots DROP CONSTRAINT \"" + name.replace("\"", "\"\"") + "\"");
                }
            }
        } else {
            List<String> names = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT INDEX_NAME FROM information_schema.STATISTICS "
                         + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plots' AND NON_UNIQUE = 0 "
                         + "GROUP BY INDEX_NAME HAVING COUNT(*) = 1 AND MIN(COLUMN_NAME) = 'slot_number'")) {
                while (rs.next()) names.add(rs.getString(1));
            }
            for (String name : names) statements.add("ALTER TABLE plots DROP INDEX `" + name.replace("`", "``") + "`");
        }
        return statements;
    }

    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }
}
