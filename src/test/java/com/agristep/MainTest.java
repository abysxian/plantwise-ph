package com.agristep;

import com.agristep.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** The start-up errors a student is most likely to hit must say what to do. */
class MainTest {
    private static DatabaseConfig mysql() {
        Properties p = new Properties();
        p.setProperty("db.type", "mysql");
        return DatabaseConfig.from(p);
    }

    @Test void mysqlDownTellsThemToStartXamppOrSwitchToH2() {
        String text = Main.explain(new RuntimeException("Communications link failure"), mysql());
        assertTrue(text.contains("XAMPP"));
        assertTrue(text.contains("db.type"));
        assertTrue(text.contains("h2"));
    }

    @Test void lockedH2FileMeansAnotherWindowIsOpen() {
        String text = Main.explain(new RuntimeException("Database may be already in use: Locked by another process"),
                DatabaseConfig.from(new Properties()));
        assertTrue(text.contains("already open"));
    }

    @Test void badConfigValueIsShownAsIs() {
        assertEquals("db.type must be wrong", Main.explain(new IllegalArgumentException("db.type must be wrong"), null));
    }

    @Test void otherErrorsStillGiveAReadableMessage() {
        String text = Main.explain(new RuntimeException("boom"), DatabaseConfig.from(new Properties()));
        assertTrue(text.contains("boom"));
    }
}
