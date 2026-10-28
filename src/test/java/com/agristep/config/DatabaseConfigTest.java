package com.agristep.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    private static Properties props(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) p.setProperty(kv[i], kv[i + 1]);
        return p;
    }

    @Test void h2IsTheDefaultAndNeedsNoInstall() {
        DatabaseConfig c = DatabaseConfig.from(props());
        assertEquals("h2", c.type());
        assertFalse(c.isMySql());
        assertTrue(c.url().startsWith("jdbc:h2:file:./data/agristep"));
        assertTrue(c.url().contains("MODE=MySQL"));
    }

    @Test void mysqlUsesXamppDefaults() {
        DatabaseConfig c = DatabaseConfig.from(props("db.type", "MySQL"));
        assertTrue(c.isMySql());
        assertEquals("root", c.user());
        assertEquals("", c.password());
        assertTrue(c.url().startsWith("jdbc:mysql://localhost:3306/agristep"));
        assertTrue(c.url().contains("createDatabaseIfNotExist=true"));
    }

    @Test void mysqlSettingsCanBeOverridden() {
        DatabaseConfig c = DatabaseConfig.from(props("db.type", "mysql", "mysql.url", "jdbc:mysql://db:3307/x",
                "mysql.user", "farmer", "mysql.password", "secret"));
        assertEquals("jdbc:mysql://db:3307/x", c.url());
        assertEquals("farmer", c.user());
        assertEquals("secret", c.password());
    }

    @Test void unknownTypeGivesAClearMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.from(props("db.type", "oracle")));
        assertTrue(e.getMessage().contains("h2") && e.getMessage().contains("mysql") && e.getMessage().contains("oracle"));
    }

    @Test void theBundledPropertiesFileLoads() throws Exception {
        assertNotNull(DatabaseConfig.load());
    }
}
