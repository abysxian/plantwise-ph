package com.agristep.service;

import com.agristep.AppContext;
import com.agristep.MutableClock;
import com.agristep.TestDb;
import com.agristep.domain.Plot;
import com.agristep.domain.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GardenGridServiceTest {
    private static AppContext app() {
        return TestDb.app(new ServicesTest.CountingWeather(), new MutableClock(Instant.parse("2026-06-01T00:00:00Z")));
    }

    @Test void startsAsThreeByThree() {
        AppContext ctx = app();
        assertEquals(new GridShape(3, 3), ctx.grid.shape());
    }

    @Test void growingAddsEmptyBedsAndKeepsPlants() {
        AppContext ctx = app();
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), ctx.crops.findByCode("okra").orElseThrow().getId(), null);
        assertEquals(new GridShape(4, 6), ctx.grid.resize(4, 6, false));
        assertEquals(24, ctx.garden.allPlots().size());
        assertEquals("Bed 24", ctx.garden.allPlots().get(23).getName());
        assertEquals("Okra", ctx.garden.allPlots().get(0).getCrop().getName());
        assertEquals(new GridShape(4, 6), ctx.grid.shape());
    }

    @Test void shrinkingEmptyBedsNeedsNoConfirmation() {
        AppContext ctx = app();
        ctx.grid.resize(2, 2, false);
        assertEquals(4, ctx.garden.allPlots().size());
        assertEquals(new GridShape(2, 2), ctx.grid.shape());
    }

    @Test void shrinkingPlantedBedsAsksFirstThenRemovesTheirTasksToo() {
        AppContext ctx = app();
        Plot last = ctx.garden.allPlots().get(8);
        ctx.garden.plant(last.getId(), ctx.crops.findByCode("okra").orElseThrow().getId(), null);
        GridResizeException e = assertThrows(GridResizeException.class, () -> ctx.grid.resize(2, 2, false));
        assertEquals(java.util.List.of("Bed 9"), e.getPlantedBeds());
        assertEquals(9, ctx.garden.allPlots().size());   // untouched
        ctx.grid.resize(2, 2, true);
        assertEquals(4, ctx.garden.allPlots().size());
        assertEquals(0, ctx.garden.dueTaskCount() + ctx.garden.readyPlotCount());
    }

    @Test void sizeIsKeptPerAccountAndSurvivesSignInAgain() {
        AppContext ctx = app();
        ctx.grid.resize(2, 5, false);
        ctx.auth.logout();
        ctx.auth.register("other", "Other", "secret1", "secret1");
        assertEquals(new GridShape(3, 3), ctx.grid.shape());
        ctx.auth.logout();
        ctx.auth.login("tester", "secret1");
        assertEquals(new GridShape(2, 5), ctx.grid.shape());
        assertEquals(10, ctx.garden.allPlots().size());
    }

    @Test void rejectsSizesOutsideTheLimits() {
        AppContext ctx = app();
        assertThrows(ValidationException.class, () -> ctx.grid.resize(0, 3, false));
        assertThrows(ValidationException.class, () -> ctx.grid.resize(3, 0, false));
        assertThrows(ValidationException.class, () -> ctx.grid.resize(GardenGridService.MAX_ROWS + 1, 3, false));
        assertThrows(ValidationException.class, () -> ctx.grid.resize(3, GardenGridService.MAX_COLUMNS + 1, false));
        assertEquals(9, ctx.garden.allPlots().size());
    }

    @Test void olderGardenWithATenthBedGetsAFourthRow() {
        AppContext ctx = app();
        ctx.garden.addPlot();
        assertEquals(new GridShape(4, 3), ctx.grid.shape());
    }
}
