package com.agristep.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RememberedLoginTest {
    @Test void writesReadsAndClears(@TempDir Path dir) {
        RememberedLogin r = new RememberedLogin(dir.resolve("sub").resolve("user.txt"));
        assertTrue(r.read().isEmpty());
        r.write("maria");
        assertEquals("maria", r.read().orElseThrow());
        r.clear();
        assertTrue(r.read().isEmpty());
    }

    @Test void unreadableFileJustMeansNothingIsRemembered(@TempDir Path dir) throws Exception {
        Path blocker = dir.resolve("file");
        Files.writeString(blocker, "x");
        RememberedLogin r = new RememberedLogin(blocker.resolve("inside").resolve("user.txt"));
        r.write("maria");   // cannot create the folder: must not throw
        assertTrue(r.read().isEmpty());
        Files.writeString(dir.resolve("blank.txt"), "  \n");
        assertTrue(new RememberedLogin(dir.resolve("blank.txt")).read().isEmpty());
    }
}
