package com.agristep.service;

import com.agristep.domain.User;

import java.util.Optional;

/**
 * Who is using the app right now. The repositories read this so that every query only ever sees the
 * signed-in person's own beds, history and settings.
 */
public final class Session {
    private volatile User user;
    private volatile Long gardenOwnerId;     // set while working in someone else's shared garden

    public Optional<User> user() { return Optional.ofNullable(user); }

    public User require() {
        User u = user;
        if (u == null) throw new NotSignedInException();
        return u;
    }

    /** The id of the signed-in person. */
    public long requireUserId() { return require().getId(); }

    /** The id of the garden being worked on: the person's own, or the owner's while in a shared garden. */
    public long requireGardenId() {
        User u = require();
        Long owner = gardenOwnerId;
        return owner != null ? owner : u.getId();
    }

    public boolean isInSharedGarden() { return gardenOwnerId != null; }

    void useGardenOf(Long ownerId) { this.gardenOwnerId = ownerId; }

    public boolean isSignedIn() { return user != null; }

    void signIn(User user) { this.user = user; this.gardenOwnerId = null; }

    void signOut() { this.user = null; this.gardenOwnerId = null; }
}
