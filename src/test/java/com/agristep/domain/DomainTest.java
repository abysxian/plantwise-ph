package com.agristep.domain;

import com.agristep.service.TaskPlanner;
import com.agristep.weather.DailyWeather;
import com.agristep.weather.WeatherForecast;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private static Crop sampleCrop() {
        return new Crop("test", "Test", "Test crop", "Testus cropus", "🌱", 40,
                new ClimateRange(10, 20, 30, 40), new ClimateRange(500, 800, 1500, 2500), new ClimateRange(5, 6, 7, 8))
                .withGrowingNotes("Sun", "20 cm")
                .withSource(false, "note", "http://example.org")
                .withSoils(EnumSet.of(SoilType.LOAM), EnumSet.of(SoilType.LOAM, SoilType.SANDY))
                .addTaskTemplate(TaskKind.SOWING, 0, 1.0, "Sow it.", "")
                .addTaskTemplate(TaskKind.WATERING, 5, 0.5, "Water it.", "")
                .addTaskTemplate(TaskKind.HARVEST, 40, null, "Ready.", "Cut it.");
    }

    // ---- ClimateRange ----

    @Test void climateRangeFitIsOneInsideIdealZeroOutsideAndRampsBetween() {
        ClimateRange r = new ClimateRange(10, 20, 30, 40);
        assertEquals(1.0, r.fit(25), 1e-9);
        assertEquals(1.0, r.fit(20), 1e-9);
        assertEquals(0.5, r.fit(15), 1e-9);
        assertEquals(0.5, r.fit(35), 1e-9);
        assertEquals(0.0, r.fit(9), 1e-9);
        assertEquals(0.0, r.fit(41), 1e-9);
    }

    @Test void climateRangeRejectsOutOfOrderNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new ClimateRange(20, 10, 30, 40));
    }

    // ---- Crop ----

    @Test void soilFitIsFullForOptimalPartialForTolerableZeroOtherwise() {
        Crop c = sampleCrop();
        assertEquals(1.0, c.soilFit(SoilType.LOAM), 1e-9);
        assertTrue(c.soilFit(SoilType.SANDY) > 0 && c.soilFit(SoilType.SANDY) < 1);
        assertEquals(0.0, c.soilFit(SoilType.CLAY), 1e-9);
    }

    @Test void taskTemplatesComeBackInDayOrder() {
        Crop c = sampleCrop();
        List<TaskTemplate> t = c.getTaskTemplates();
        assertEquals(List.of(0, 5, 40), t.stream().map(TaskTemplate::getDayOffset).toList());
    }

    // ---- TaskPlanner + Plot ----

    @Test void plannerDatesEachTaskFromThePlantingDay() {
        LocalDate planted = LocalDate.of(2026, 3, 1);
        List<CareTask> plan = new TaskPlanner().plan(sampleCrop(), planted);
        assertEquals(3, plan.size());
        assertEquals(planted, plan.get(0).getDueDate());
        assertEquals(planted.plusDays(5), plan.get(1).getDueDate());
        assertEquals(planted.plusDays(40), plan.get(2).getDueDate());
        assertTrue(plan.get(0) instanceof SowingTask);
        assertTrue(plan.get(1) instanceof WateringTask);
        assertTrue(plan.get(2) instanceof HarvestTask);
    }

    @Test void everyTaskKindMakesItsOwnSubclassWithInstructions() {
        for (TaskKind kind : TaskKind.values()) {
            CareTask t = kind.create(3, LocalDate.of(2026, 1, 1), 2.0, "Detail here.", "Extra here.");
            assertEquals(kind, t.getKind());
            assertFalse(t.getInstructions().isBlank(), kind.name());
            assertFalse(t.getTitle().isBlank());
            assertFalse(t.getIcon().isBlank());
        }
    }

    @Test void plotLifecyclePlantProgressHarvestClear() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        Plot plot = new Plot(1);
        assertTrue(plot.isEmpty());
        assertEquals(PlotStatus.EMPTY, plot.status(day));

        plot.plant(sampleCrop(), day, new TaskPlanner().plan(sampleCrop(), day));
        assertFalse(plot.isEmpty());
        assertEquals(PlotStatus.PLANTED, plot.status(day));
        assertEquals(0, plot.progressPercent());

        CareTask first = plot.getTasks().get(0);
        first.toggle(LocalDateTime.of(day, java.time.LocalTime.NOON));
        assertEquals(PlotStatus.GROWING, plot.status(day));
        assertEquals(33, plot.progressPercent());
        first.toggle(LocalDateTime.now());                          // un-ticking works too
        assertFalse(first.isCompleted());
        assertNull(first.getCompletedAt());

        assertEquals(PlotStatus.READY, plot.status(day.plusDays(40)));
        HarvestRecord record = plot.harvest(day.plusDays(41));
        assertEquals("Test", record.getCropName());
        assertEquals(41, record.daysGrown());
        assertTrue(plot.isEmpty());
    }

    @Test void plantingAnOccupiedBedFails() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        Plot plot = new Plot(2);
        plot.plant(sampleCrop(), day, new TaskPlanner().plan(sampleCrop(), day));
        assertThrows(PlotOccupiedException.class,
                () -> plot.plant(sampleCrop(), day, new TaskPlanner().plan(sampleCrop(), day)));
    }

    @Test void harvestingAnEmptyBedFails() {
        assertThrows(IllegalStateException.class, () -> new Plot(3).harvest(LocalDate.now()));
    }

    @Test void clearEmptiesTheBedWithoutLoggingAHarvest() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        Plot plot = new Plot(4);
        plot.plant(sampleCrop(), day, new TaskPlanner().plan(sampleCrop(), day));
        plot.clear();
        assertTrue(plot.isEmpty());
        assertTrue(plot.getTasks().isEmpty());
    }

    @Test void overdueMeansPastDueAndNotDone() {
        LocalDate due = LocalDate.of(2026, 5, 1);
        CareTask t = TaskKind.WATERING.create(0, due, 1.0, "d", "");
        assertFalse(t.isOverdue(due));
        assertTrue(t.isOverdue(due.plusDays(1)));
        t.toggle(LocalDateTime.now());
        assertFalse(t.isOverdue(due.plusDays(5)));
    }

    @Test void wateringHintTellsYouToSkipWhenRainIsComing() {
        CareTask water = TaskKind.WATERING.create(0, LocalDate.now(), 1.0, "d", "");
        WeatherForecast wet = new WeatherForecast(28, 61, 90, List.of(
                new DailyWeather(LocalDate.now(), 28, 24, 15, 63),
                new DailyWeather(LocalDate.now().plusDays(1), 28, 24, 12, 63)));
        assertTrue(water.weatherHint(wet).isPresent());
        assertTrue(water.weatherHint(wet).get().toLowerCase().contains("skip"));
        WeatherForecast dry = new WeatherForecast(28, 0, 50, List.of(
                new DailyWeather(LocalDate.now(), 29, 24, 0, 0),
                new DailyWeather(LocalDate.now().plusDays(1), 29, 24, 0, 0)));
        assertTrue(water.weatherHint(dry).isEmpty() || !water.weatherHint(dry).get().toLowerCase().contains("skip"));
    }

    @Test void soilTypesAllHaveLabelsDescriptionsAndEmoji() {
        for (SoilType s : SoilType.values()) {
            assertFalse(s.getLabel().isBlank());
            assertFalse(s.getDescription().isBlank());
            assertFalse(s.getEmoji().isBlank());
        }
        assertEquals(Set.of(SoilType.values()).size(), 3);
    }
}
