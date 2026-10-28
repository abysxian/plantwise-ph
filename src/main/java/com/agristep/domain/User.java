package com.agristep.domain;

import java.time.LocalDateTime;

/** A person with an account. Everything in the garden belongs to exactly one user. */
public class User {
    private Long id;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final LocalDateTime createdAt;

    public User(String username, String displayName, String passwordHash, LocalDateTime createdAt) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) throw new IllegalStateException("This user already has id " + this.id);
        this.id = id;
    }

    /** Usernames are compared ignoring case ("Maria" and "maria" are the same account). */
    public static String keyOf(String username) {
        return username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String usernameKey() { return keyOf(username); }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
