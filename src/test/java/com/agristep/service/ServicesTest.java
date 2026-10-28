package com.agristep.service;

import com.agristep.MutableClock;
import com.agristep.TestDb;
import com.agristep.AppContext;
import com.agristep.domain.Crop;
import com.agristep.domain.GardenSettings;
import com.agristep.domain.SoilType;
import com.agristep.persistence.JdbcClimateProfileRepository;
import com.agristep.weather.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ServicesTest {

    /** Counts calls and can be switched off. */
    public static final class CountingWeather implements WeatherProvider {
        final AtomicInteger forecasts = new AtomicInteger();
        final AtomicInteger climates = new AtomicInteger();
        boolean offline;
        double rainToday = 0, maxToday = 30;
        ClimateStats climate = new ClimateStats(27.8, 2300);

        @Override public WeatherForecast fetchForecast(double lat, double lon) {
            forecasts.incrementAndGet();
            if (offline) throw new WeatherUnavailableException("offline");
            return new WeatherForecast(29, 1, 70, List.of(new DailyWeather(LocalDate.now(), maxToday, 24, rainToday, 1)));
        }

        @Override public ClimateStats fetchClimate(double lat, double lon) {
            climates.incrementAndGet();
            if (offline) throw new WeatherUnavailableException("offline");
            return climate;
        }

        @Override public List<GeoPlace> searchPlaces(String q) { return List.of(new GeoPlace(q, null, "PH", 1, 2)); }
    }

    private static Crop crop(AppContext ctx, String code) { return ctx.crops.findByCode(code).orElseThrow(); }

    // ---- scoring ----

    @Test void idealConditionsScoreGreatAndTooColdIsCappedLow() {
        AppContext ctx = TestDb.app(new CountingWeather(), Clock.systemUTC());
        ClimateSuitabilityScorer scorer = new ClimateSuitabilityScorer();
        CropRecommendation ideal = scorer.score(crop(ctx, "kangkong"), new ClimateStats(28, 2300), SoilType.LOAM);
        assertEquals(Fit.GREAT, ideal.fit());
        assertEquals(100, ideal.score());

        CropRecommendation frozen = scorer.score(crop(ctx, "okra"), new ClimateStats(5, 1000), SoilType.LOAM);
        assertTrue(frozen.score() <= 15, "score " + frozen.score());
        assertEquals(Fit.POOR, frozen.fit());
    }

    @Test void wrongSoilCapsTheScoreButDoesNotKillIt() {
        AppContext ctx = TestDb.app(new CountingWeather(), Clock.systemUTC());
        Crop kangkong = crop(ctx, "kangkong");
        assertEquals(0.0, kangkong.soilFit(SoilType.SANDY), 1e-9);
        CropRecommendation r = new ClimateSuitabilityScorer().score(kangkong, new ClimateStats(28, 2300), SoilType.SANDY);
        assertTrue(r.score() <= 45 && r.score() > 15, "score " + r.score());
    }

    @Test void withoutClimateTheScoreUsesSoilOnly() {
        AppContext ctx = TestDb.app(new CountingWeather(), Clock.systemUTC());
        CropRecommendation r = new ClimateSuitabilityScorer().score(crop(ctx, "talong"), null, SoilType.LOAM);
        assertEquals(100, r.score());
        assertTrue(r.reasons().get(0).contains("soil"));
    }

    @Test void fitBandsMatchTheThresholds() {
        assertEquals(Fit.GREAT, Fit.fromScore(75));
        assertEquals(Fit.GOOD, Fit.fromScore(74));
        assertEquals(Fit.GOOD, Fit.fromScore(55));
        assertEquals(Fit.POSSIBLE, Fit.fromScore(54));
        assertEquals(Fit.POSSIBLE, Fit.fromScore(30));
        assertEquals(Fit.POOR, Fit.fromScore(29));
    }

    @Test void hotLowlandsRankHeatLoversAboveCoolCropsAndAllThirtySixAreRanked() {
        CountingWeather weather = new CountingWeather();
        AppContext ctx = TestDb.app(weather, Clock.systemUTC());
        RecommendationResult result = ctx.recommendations.recommend(ctx.settings.current());
        assertEquals(36, result.items().size());
        assertFalse(result.degraded());
        int okra = indexOf(result, "okra");
        int repolyo = indexOf(result, "repolyo");
        assertTrue(okra < repolyo, "okra " + okra + " repolyo " + repolyo);
        for (int i = 1; i < result.items().size(); i++) {
            assertTrue(result.items().get(i - 1).score() >= result.items().get(i).score(), "not sorted");
        }
    }

    private static int indexOf(RecommendationResult r, String code) {
        for (int i = 0; i < r.items().size(); i++) if (r.items().get(i).crop().getCode().equals(code)) return i;
        return -1;
    }

    @Test void recommendationsFallBackToSoilOnlyWhenOfflineWithNothingSaved() {
        CountingWeather weather = new CountingWeather();
        weather.offline = true;
        AppContext ctx = TestDb.app(weather, Clock.systemUTC());
        RecommendationResult r = ctx.recommendations.recommend(ctx.settings.current());
        assertTrue(r.degraded());
        assertEquals(36, r.items().size());
    }

    // ---- weather service ----

    @Test void forecastIsCachedForFifteenMinutes() {
        CountingWeather weather = new CountingWeather();
        MutableClock clock = new MutableClock(Instant.parse("2026-06-01T00:00:00Z"));
        AppContext ctx = TestDb.app(weather, clock);
        GardenSettings s = ctx.settings.current();
        ctx.weather.forecast(s);
        ctx.weather.forecast(s);
        assertEquals(1, weather.forecasts.get());
        clock.advance(Duration.ofMinutes(16));
        ctx.weather.forecast(s);
        assertEquals(2, weather.forecasts.get());
    }

    @Test void oldForecastIsKeptWhenTheServiceGoesDownButNoneMeansEmpty() {
        CountingWeather weather = new CountingWeather();
        MutableClock clock = new MutableClock(Instant.parse("2026-06-01T00:00:00Z"));
        AppContext ctx = TestDb.app(weather, clock);
        GardenSettings s = ctx.settings.current();
        weather.offline = true;
        assertEquals(Optional.empty(), ctx.weather.forecast(s));
        weather.offline = false;
        assertTrue(ctx.weather.forecast(s).isPresent());
        weather.offline = true;
        clock.advance(Duration.ofHours(1));
        assertTrue(ctx.weather.forecast(s).isPresent(), "should reuse the earlier forecast");
    }

    @Test void climateIsSavedForADayThenReusedOfflineAsStale() {
        CountingWeather weather = new CountingWeather();
        MutableClock clock = new MutableClock(Instant.parse("2026-06-01T00:00:00Z"));
        AppContext ctx = TestDb.app(weather, clock);
        GardenSettings s = ctx.settings.current();
        WeatherService.ClimateResult first = ctx.weather.climate(s);
        assertFalse(first.stale());
        ctx.weather.climate(s);
        assertEquals(1, weather.climates.get(), "second call within a day must use the saved copy");

        clock.advance(Duration.ofHours(25));
        weather.offline = true;
        WeatherService.ClimateResult stale = ctx.weather.climate(s);
        assertTrue(stale.stale());
        assertEquals(27.8, stale.stats().avgTemp30dC(), 1e-9);
    }

    @Test void climateWithNothingSavedAndNoInternetThrows() {
        CountingWeather weather = new CountingWeather();
        weather.offline = true;
        AppContext ctx = TestDb.app(weather, Clock.systemUTC());
        assertThrows(WeatherUnavailableException.class, () -> ctx.weather.climate(ctx.settings.current()));
    }

    @Test void blankPlaceSearchReturnsNothingWithoutCallingTheProvider() {
        AppContext ctx = TestDb.app(new CountingWeather(), Clock.systemUTC());
        assertTrue(ctx.weather.searchPlaces("  ").isEmpty());
        assertEquals(1, ctx.weather.searchPlaces("Baguio").size());
    }

    // ---- advisor ----

    @Test void advisorWritesOneSentenceFromWeatherAndTasks() {
        GardenAdvisor advisor = new GardenAdvisor();
        LocalDate d = LocalDate.now();
        WeatherForecast rain = new WeatherForecast(27, 63, 90, List.of(new DailyWeather(d, 28, 24, 20, 63)));
        WeatherForecast hot = new WeatherForecast(34, 0, 50, List.of(new DailyWeather(d, 35, 26, 0, 0)));
        WeatherForecast dry = new WeatherForecast(30, 0, 50, List.of(new DailyWeather(d, 31, 25, 0, 0)));
        WeatherForecast fair = new WeatherForecast(30, 0, 50, List.of(new DailyWeather(d, 31, 25, 3, 0)));
        assertTrue(advisor.todayAdvice(Optional.of(rain), 0, 0).startsWith("Rain today"));
        assertTrue(advisor.todayAdvice(Optional.of(hot), 0, 0).startsWith("Hot day"));
        assertTrue(advisor.todayAdvice(Optional.of(dry), 0, 0).startsWith("A dry spell"));
        assertTrue(advisor.todayAdvice(Optional.of(fair), 0, 0).startsWith("Fair weather"));
        assertTrue(advisor.todayAdvice(Optional.empty(), 0, 0).contains("not available"));
        assertTrue(advisor.todayAdvice(Optional.of(fair), 1, 0).contains("1 task is due"));
        assertTrue(advisor.todayAdvice(Optional.of(fair), 3, 2).contains("3 tasks are due"));
        assertTrue(advisor.todayAdvice(Optional.of(fair), 3, 2).contains("2 beds are ready"));
        assertTrue(advisor.todayAdvice(Optional.of(fair), 0, 1).contains("1 bed is ready"));
    }

    // ---- settings ----

    @Test void settingsChangeSoilAndLocationAndPersist() {
        AppContext ctx = TestDb.app(new CountingWeather(), Clock.systemUTC());
        assertEquals(SoilType.LOAM, ctx.settings.current().getSoil());
        ctx.settings.changeSoil(SoilType.CLAY);
        ctx.settings.changeLocation("Baguio", 16.40, 120.60);
        GardenSettings s = ctx.settings.current();
        assertEquals(SoilType.CLAY, s.getSoil());
        assertEquals("Baguio", s.getLocationName());
        assertEquals(16.40, s.getLatitude(), 1e-9);
    }
}
