package com.agristep.weather;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The parsing is tested with sample answers in Open-Meteo's format, so no internet is needed. */
class OpenMeteoClientTest {
    private static final String FORECAST = """
            {"current":{"temperature_2m":29.4,"weather_code":2,"relative_humidity_2m":78},
             "daily":{"time":["2026-06-01","2026-06-02"],
                      "temperature_2m_max":[31.2,28.0],"temperature_2m_min":[25.1,24.0],
                      "precipitation_sum":[0.0,12.5],"weather_code":[2,63]}}""";

    @Test void parsesForecast() {
        WeatherForecast f = OpenMeteoClient.parseForecast(FORECAST);
        assertEquals(29.4, f.currentTempC(), 1e-9);
        assertEquals(2, f.currentWeatherCode());
        assertEquals(78, f.humidityPercent());
        assertEquals(2, f.days().size());
        assertEquals(LocalDate.of(2026, 6, 1), f.days().get(0).date());
        assertEquals(12.5, f.days().get(1).rainMm(), 1e-9);
        assertEquals(12.5, f.rainInNextDays(2), 1e-9);
        assertEquals(31.2, f.maxTempInNextDays(2), 1e-9);
    }

    @Test void forecastWithoutDaysIsAnError() {
        assertThrows(WeatherUnavailableException.class, () -> OpenMeteoClient.parseForecast("{\"daily\":{}}"));
    }

    @Test void garbageIsAnErrorNotACrash() {
        assertThrows(WeatherUnavailableException.class, () -> OpenMeteoClient.parseForecast("<html>nope</html>"));
        assertThrows(WeatherUnavailableException.class, () -> OpenMeteoClient.parseClimate("not json"));
    }

    @Test void climateAveragesTheLastThirtyDaysAndScalesRainToAYear() {
        StringBuilder temps = new StringBuilder();
        StringBuilder rain = new StringBuilder();
        for (int i = 0; i < 365; i++) {
            if (i > 0) { temps.append(','); rain.append(','); }
            temps.append(i < 335 ? 20 : 30);   // last 30 days are 30 C
            rain.append(i % 2 == 0 ? 10 : 0);   // average 5 mm/day (183 wet days of 365)
        }
        rain.append(",null");                    // a day not available yet must be ignored
        temps.append(",null");
        String json = "{\"daily\":{\"temperature_2m_mean\":[" + temps + "],\"precipitation_sum\":[" + rain + "]}}";
        ClimateStats stats = OpenMeteoClient.parseClimate(json);
        assertEquals(30.0, stats.avgTemp30dC(), 1e-9);
        assertEquals(1830 + 5, stats.annualRainMm(), 10);
    }

    @Test void climateWithNoNumbersIsAnError() {
        assertThrows(WeatherUnavailableException.class,
                () -> OpenMeteoClient.parseClimate("{\"daily\":{\"temperature_2m_mean\":[null],\"precipitation_sum\":[null]}}"));
    }

    @Test void parsesPlacesAndBuildsLabels() {
        List<GeoPlace> places = OpenMeteoClient.parsePlaces("""
                {"results":[{"name":"Baguio","admin1":"Benguet","country":"Philippines","latitude":16.41,"longitude":120.59},
                            {"name":"Baguio Village","latitude":7.1,"longitude":125.5}]}""");
        assertEquals(2, places.size());
        assertEquals("Baguio, Benguet, Philippines", places.get(0).label());
        assertEquals("Baguio Village", places.get(1).label());
        assertTrue(OpenMeteoClient.parsePlaces("{}").isEmpty());
    }

    @Test void urlsCarryCoordinatesAndEncodeNames() {
        String url = OpenMeteoClient.forecastUrl(14.5995, 120.9842);
        assertTrue(url.startsWith("https://api.open-meteo.com/"));
        assertTrue(url.contains("latitude=14.5995") && url.contains("longitude=120.9842"));
        assertTrue(OpenMeteoClient.geocodingUrl("San Jose del Monte").contains("San%20Jose") ||
                OpenMeteoClient.geocodingUrl("San Jose del Monte").contains("San+Jose"));
        assertTrue(OpenMeteoClient.archiveUrl(1, 2, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).contains("2025-01-01"));
    }

    @Test void weatherCodesHaveWordsAndEmoji() {
        for (int code : new int[] {0, 1, 2, 3, 45, 51, 61, 71, 80, 95, 999}) {
            assertFalse(WeatherCodes.describe(code).isBlank());
            assertFalse(WeatherCodes.emoji(code).isBlank());
        }
    }
}
