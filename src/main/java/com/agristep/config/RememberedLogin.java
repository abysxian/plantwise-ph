package com.agristep.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * "Keep me signed in on this computer": remembers only the username, in the person's own home folder.
 * Anyone who opens the app on this computer is signed in as that user until they press Sign out.
 * Problems reading or writing the file are ignored: the person just sees the sign-in screen again.
 */
public class RememberedLogin {
    private final Path file;

    public RememberedLogin(Path file) { this.file = file; }

    public static RememberedLogin atDefaultLocation() {
        return new RememberedLogin(Path.of(System.getProperty("user.home", "."), ".agristep", "remembered-user.txt"));
    }

    public Optional<String> read() {
        try {
            if (!Files.isRegularFile(file)) return Optional.empty();
            String name = Files.readString(file, StandardCharsets.UTF_8).trim();
            return name.isEmpty() ? Optional.empty() : Optional.of(name);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void write(String username) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, username, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            // best effort
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // best effort
        }
    }
}
