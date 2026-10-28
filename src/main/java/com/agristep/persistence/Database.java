package com.agristep.persistence;

import com.agristep.repository.Transactions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * The one place that knows how to reach the database (H2 or MySQL, chosen by the JDBC url).
 * Repositories borrow a connection through {@link #withConnection}. Inside {@link #run} every
 * repository on the same thread shares one connection, which is committed (or rolled back) together.
 */
public final class Database implements Transactions {

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T value) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public String url() { return url; }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public <R> R withConnection(SqlFunction<Connection, R> work) {
        Connection inTransaction = current.get();
        try {
            if (inTransaction != null) {
                return work.apply(inTransaction);
            }
            try (Connection connection = open()) {
                return work.apply(connection);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T run(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();                       // already inside a transaction: join it
        }
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException | Error e) {
                try { connection.rollback(); } catch (SQLException ignored) { /* the original error matters more */ }
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    /** Creates any missing tables and upgrades an older database. Safe to run on every start. */
    public void initSchema() {
        String script;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql is missing from the application");
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read schema.sql", e);
        }
        withConnection(connection -> {
            for (String statement : script.split(";\\s*\\n")) {
                String sql = statement.lines().filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").trim();
                if (!sql.isEmpty()) {
                    try (Statement st = connection.createStatement()) {
                        st.execute(sql);
                    }
                }
            }
            SchemaMigrator.migrate(connection);
            return null;
        });
    }
}
