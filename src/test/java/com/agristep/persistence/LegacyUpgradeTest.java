package com.agristep.persistence;

import com.agristep.AppContext;
import com.agristep.TestDb;
import com.agristep.service.ServicesTest;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

/** A database made by the single-garden version must open, keep its data, and give it to the first account. */
class LegacyUpgradeTest {
    private static Database legacyDatabase() throws Exception {
        Database db = TestDb.memory();
        String script;
        try (InputStream in = LegacyUpgradeTest.class.getResourceAsStream("/legacy-schema.sql")) {
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        db.withConnection(c -> {
            for (String part : script.split(";\\s*\\n")) {
                String sql = part.lines().filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").trim();
                if (sql.isEmpty()) continue;
                try (Statement st = c.createStatement()) { st.execute(sql); }
            }
            try (Statement st = c.createStatement()) {
                st.execute("INSERT INTO plots (slot_number, name) VALUES (1, 'Old bed one')");
                st.execute("INSERT INTO plots (slot_number, name) VALUES (2, 'Old bed two')");
                st.execute("INSERT INTO harvest_records (crop_name, crop_emoji, plot_name, planted_on, harvested_on, completion_percent)"
                        + " VALUES ('Okra', 'x', 'Old bed one', '2026-01-01', '2026-03-01', 100)");
                st.execute("INSERT INTO garden_settings (id, location_name, latitude, longitude, soil)"
                        + " VALUES (1, 'Old Town', 10.0, 120.0, 'SANDY')");
            }
            return null;
        });
        return db;
    }

    @Test void oldGardenBecomesTheFirstAccountsGarden() throws Exception {
        Database db = legacyDatabase();
        AppContext ctx = AppContext.create(db, new ServicesTest.CountingWeather(), Clock.systemUTC());
        ctx.seed();
        ctx.auth.login("demo", "demo1234");
        assertEquals(2, ctx.garden.allPlots().size());
        assertEquals("Old bed one", ctx.garden.allPlots().get(0).getName());
        assertEquals(1, ctx.garden.recentHarvests().size());
        assertEquals("Old Town", ctx.settings.current().getLocationName());
        ctx.garden.addPlot();   // slot numbers continue from the old ones
        assertEquals(3, ctx.garden.allPlots().get(2).getSlotNumber());

        ctx.auth.logout();
        ctx.auth.register("newbie", "Newbie", "secret1", "secret1");
        assertEquals(9, ctx.garden.allPlots().size());   // slot 1 is allowed again for another user
        assertEquals(0, ctx.garden.recentHarvests().size());
    }

    @Test void upgradingTwiceChangesNothing() throws Exception {
        Database db = legacyDatabase();
        AppContext first = AppContext.create(db, new ServicesTest.CountingWeather(), Clock.systemUTC());
        first.seed();
        AppContext again = AppContext.create(db, new ServicesTest.CountingWeather(), Clock.systemUTC());
        again.seed();
        again.auth.login("demo", "demo1234");
        assertEquals(2, again.garden.allPlots().size());
    }
}
