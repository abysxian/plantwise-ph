package com.agristep.persistence;

import com.agristep.AppContext;
import com.agristep.MutableClock;
import com.agristep.TestDb;
import com.agristep.domain.*;
import com.agristep.service.ServicesTest;
import com.agristep.weather.WeatherProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the real SQL against H2 in MySQL mode, including a genuine close-and-reopen of a database file. */
class PersistenceTest {

    private static WeatherProvider weather() { return new ServicesTest.CountingWeather(); }

    @Test void seedingIsRepeatableAndCreatesStarterGarden() throws Exception {
        Database db = TestDb.memory();
        AppContext ctx = AppContext.create(db, weather(), Clock.systemUTC());
        ctx.seed();
        ctx.seed();   // second start must not duplicate anything
        ctx.auth.login("demo", "demo1234");
        assertEquals(36, ctx.crops.findAllByOrderByNameAsc().size());
        assertEquals(9, ctx.garden.allPlots().size());
        assertEquals("Manila, Philippines", ctx.settings.current().getLocationName());
    }

    @Test void cropRoundTripKeepsEveryField() {
        AppContext ctx = TestDb.app(weather(), Clock.systemUTC());
        Crop talong = ctx.crops.findByCode("talong").orElseThrow();
        assertNotNull(talong.getId());
        assertEquals("Eggplant", talong.getEnglishName());
        assertTrue(talong.isVerified());
        assertEquals(75, talong.getCycleDays());
        assertEquals(20, talong.getTemperature().getOptMin());
        assertEquals(35, talong.getTemperature().getOptMax());
        assertTrue(talong.getOptimalSoils().contains(SoilType.LOAM));
        assertEquals(5, talong.careTips().size());
        assertEquals(3, talong.pestTips().size());
        assertEquals(7, talong.getTaskTemplates().size());
        assertEquals("🍆", talong.getEmoji());
    }

    @Test void plantingSavesTheBedAndItsTasksAndTickingPersists() {
        AppContext ctx = TestDb.app(weather(), Clock.systemUTC());
        Plot bed = ctx.garden.allPlots().get(0);
        Crop okra = ctx.crops.findByCode("okra").orElseThrow();
        LocalDate day = ctx.garden.today().minusDays(10);
        ctx.garden.plant(bed.getId(), okra.getId(), day);

        Plot loaded = ctx.garden.getPlot(bed.getId());
        assertEquals("Okra", loaded.getCrop().getName());
        assertEquals(day, loaded.getPlantedOn());
        assertEquals(okra.getTaskTemplates().size(), loaded.getTasks().size());
        assertTrue(loaded.getTasks().stream().allMatch(t -> t.getId() != null));

        CareTask first = loaded.getTasks().get(0);
        ctx.garden.toggleTask(bed.getId(), first.getId());
        assertTrue(ctx.garden.getPlot(bed.getId()).getTasks().get(0).isCompleted());
        ctx.garden.toggleTask(bed.getId(), first.getId());
        assertFalse(ctx.garden.getPlot(bed.getId()).getTasks().get(0).isCompleted());
        assertTrue(ctx.garden.dueTaskCount() > 0);
    }

    @Test void plantingInAFullBedOrInTheFutureIsRejectedAndChangesNothing() {
        AppContext ctx = TestDb.app(weather(), Clock.systemUTC());
        Plot bed = ctx.garden.allPlots().get(0);
        Long okra = ctx.crops.findByCode("okra").orElseThrow().getId();
        ctx.garden.plant(bed.getId(), okra, null);
        assertThrows(PlotOccupiedException.class, () -> ctx.garden.plant(bed.getId(), okra, null));
        Plot other = ctx.garden.allPlots().get(1);
        assertThrows(IllegalArgumentException.class,
                () -> ctx.garden.plant(other.getId(), okra, ctx.garden.today().plusDays(2)));
        assertTrue(ctx.garden.getPlot(other.getId()).isEmpty());
        assertThrows(NotFoundException.class, () -> ctx.garden.getPlot(99999L));
    }

    @Test void harvestLogsHistoryAndEmptiesTheBedTogether() {
        AppContext ctx = TestDb.app(weather(), Clock.systemUTC());
        Plot bed = ctx.garden.allPlots().get(0);
        Crop kangkong = ctx.crops.findByCode("kangkong").orElseThrow();
        ctx.garden.plant(bed.getId(), kangkong.getId(), ctx.garden.today().minusDays(31));
        assertEquals(1, ctx.garden.readyPlotCount());

        HarvestRecord record = ctx.garden.harvest(bed.getId());
        assertEquals("Kangkong", record.getCropName());
        assertTrue(ctx.garden.getPlot(bed.getId()).isEmpty());
        List<HarvestRecord> history = ctx.garden.recentHarvests();
        assertEquals(1, history.size());
        assertEquals("🥬", history.get(0).getCropEmoji());
        assertEquals(31, history.get(0).daysGrown());
        assertEquals(0, ctx.garden.readyPlotCount());
    }

    @Test void clearEmptiesTheBedWithoutHistoryAndAddPlotNumbersOnward() {
        AppContext ctx = TestDb.app(weather(), Clock.systemUTC());
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), ctx.crops.findByCode("okra").orElseThrow().getId(), null);
        ctx.garden.clear(bed.getId());
        assertTrue(ctx.garden.getPlot(bed.getId()).isEmpty());
        assertTrue(ctx.garden.recentHarvests().isEmpty());
        Plot added = ctx.garden.addPlot();
        assertEquals(10, added.getSlotNumber());
        assertEquals("Bed 10", added.getName());
        assertEquals(10, ctx.garden.allPlots().size());
    }

    @Test void historyKeepsOnlyTheTenNewest() {
        AppContext ctx = TestDb.app(weather(), Clock.systemUTC());
        Plot bed = ctx.garden.allPlots().get(0);
        Long kangkong = ctx.crops.findByCode("kangkong").orElseThrow().getId();
        for (int i = 0; i < 12; i++) {
            ctx.garden.plant(bed.getId(), kangkong, ctx.garden.today().minusDays(31));
            ctx.garden.harvest(bed.getId());
        }
        assertEquals(10, ctx.garden.recentHarvests().size());
    }

    @Test void everythingSurvivesClosingAndReopeningTheDatabaseFile(@TempDir Path dir) throws Exception {
        String url = "jdbc:h2:file:" + dir.resolve("garden") + ";MODE=MySQL";
        Long bedId;
        {
            AppContext first = AppContext.create(new Database(url, "sa", ""), weather(), Clock.systemUTC());
            first.seed();
            first.auth.login("demo", "demo1234");
            first.settings.changeSoil(SoilType.SANDY);
            first.settings.changeLocation("Baguio", 16.4, 120.6);
            Plot bed = first.garden.allPlots().get(2);
            bedId = bed.getId();
            first.garden.plant(bedId, first.crops.findByCode("sibuyas").orElseThrow().getId(), first.garden.today().minusDays(5));
            first.garden.toggleTask(bedId, first.garden.getPlot(bedId).getTasks().get(0).getId());
            first.database.withConnection(c -> { c.createStatement().execute("SHUTDOWN"); return null; });
        }
        AppContext second = AppContext.create(new Database(url, "sa", ""), weather(), Clock.systemUTC());
        second.seed();   // an ordinary second start
        second.auth.login("demo", "demo1234");
        assertEquals(36, second.crops.findAllByOrderByNameAsc().size());
        assertEquals(9, second.garden.allPlots().size());
        assertEquals(SoilType.SANDY, second.settings.current().getSoil());
        assertEquals("Baguio", second.settings.current().getLocationName());
        Plot bed = second.garden.getPlot(bedId);
        assertEquals("Sibuyas", bed.getCrop().getName());
        assertTrue(bed.getTasks().get(0).isCompleted());
        assertEquals(second.garden.today().minusDays(5), bed.getPlantedOn());
    }

    @Test void climateProfileIsSavedAndUpdatedInPlace() {
        AppContext ctx = TestDb.app(weather(), new MutableClock(Instant.parse("2026-06-01T00:00:00Z")));
        JdbcClimateProfileRepository repo = new JdbcClimateProfileRepository(ctx.database);
        assertTrue(repo.findByLocationKey("1.00,2.00").isEmpty());
        ClimateProfile p = new ClimateProfile("1.00,2.00", "Somewhere", new com.agristep.weather.ClimateStats(26, 2000),
                java.time.LocalDateTime.of(2026, 6, 1, 0, 0));
        repo.save(p);
        ClimateProfile back = repo.findByLocationKey("1.00,2.00").orElseThrow();
        assertEquals(26, back.toStats().avgTemp30dC(), 1e-9);
        back.refresh(new com.agristep.weather.ClimateStats(30, 1500), java.time.LocalDateTime.of(2026, 6, 2, 0, 0));
        repo.save(back);
        assertEquals(30, repo.findByLocationKey("1.00,2.00").orElseThrow().toStats().avgTemp30dC(), 1e-9);
    }
}
