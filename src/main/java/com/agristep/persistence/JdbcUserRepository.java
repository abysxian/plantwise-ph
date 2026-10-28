package com.agristep.persistence;

import com.agristep.domain.User;
import com.agristep.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {
    private static final String SELECT =
            "SELECT id, username, display_name, password_hash, created_at FROM users ";

    private final Database db;

    public JdbcUserRepository(Database db) {
        this.db = db;
    }

    @Override
    public User save(User user) {
        if (user.getId() != null) return user;       // accounts are only ever created here
        db.withConnection(c -> {
            user.assignId(Sql.insert(c,
                    "INSERT INTO users (username, username_key, display_name, password_hash, created_at) VALUES (?,?,?,?,?)",
                    user.getUsername(), user.usernameKey(), user.getDisplayName(), user.getPasswordHash(), user.getCreatedAt()));
            return null;
        });
        return user;
    }

    @Override public void updatePasswordHash(long userId, String newHash) {
        db.withConnection(c -> Sql.update(c, "UPDATE users SET password_hash = ? WHERE id = ?", newHash, userId));
    }

    @Override public Optional<User> findById(Long id) { return query("WHERE id = ?", id).stream().findFirst(); }

    @Override public Optional<User> findByUsername(String username) {
        return query("WHERE username_key = ?", User.keyOf(username)).stream().findFirst();
    }

    @Override public List<User> findAll() { return query("ORDER BY id"); }

    @Override public long count() { return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM users")); }

    private List<User> query(String tail, Object... params) {
        return db.withConnection(c -> Sql.query(c, SELECT + tail, rs -> {
            User u = new User(rs.getString("username"), rs.getString("display_name"), rs.getString("password_hash"),
                    Sql.dateTime(rs, "created_at"));
            u.assignId(rs.getLong("id"));
            return u;
        }, params));
    }
}
