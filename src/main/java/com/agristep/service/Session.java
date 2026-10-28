package com.agristep.service;

import com.agristep.domain.User;

import java.util.Optional;

/**
 * Who is using the app right now. The repositories read this so that every query only ever sees the
 * signed-in person's own beds, history and settings.
 */
public final class Session {
    private volatile User user;

    public Optional<User> user() { return Optional.ofNullable(user); }

    public User require() {
        User u = user;
        if (u == null) throw new NotSignedInException();
        return u;
    }

    public long requireUserId() { return require().getId(); }

    public boolean isSignedIn() { return user != null; }

    void signIn(User user) { this.user = user; }

    void signOut() { this.user = null; }
}
