package com.agristep.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Remembers the chosen language ("en" or "fil") in the person's home folder. Problems are ignored. */
public class LanguagePreference {
    private final Path file;

    public LanguagePreference(Path file) { this.file = file; }

    public static LanguagePreference atDefaultLocation() {
        return new LanguagePreference(Path.of(System.getProperty("user.home", "."), ".agristep", "language.txt"));
    }

    public String read() {
        try {
            return Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8).trim() : "";
        } catch (IOException e) {
            return "";
        }
    }

    public void write(String code) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, code, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            // best effort
        }
    }
}
