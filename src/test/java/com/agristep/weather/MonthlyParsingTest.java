package com.agristep.weather;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyParsingTest {
    @Test void climateJsonGivesTwelveMonthlyFigures() {
        StringBuilder times = new StringBuilder(), temps = new StringBuilder(), rain = new StringBuilder();
        LocalDate d = LocalDate.of(2025, 6, 1);
        for (int i = 0; i < 365; i++, d = d.plusDays(1)) {
            if (i > 0) { times.append(','); temps.append(','); rain.append(','); }
            times.append('"').append(d).append('"');
            temps.append(20 + d.getMonthValue());           // 21 in January ... 32 in December
            rain.append(d.getMonthValue() == 8 ? 10 : 0);   // all the rain falls in August
        }
        String json = "{\"daily\":{\"time\":[" + times + "],\"temperature_2m_mean\":[" + temps + "],\"precipitation_sum\":[" + rain + "]}}";
        ClimateStats stats = OpenMeteoClient.parseClimate(json);
        assertTrue(stats.hasMonthly());
        assertEquals(21, stats.monthlyTempC()[0], 0.001);
        assertEquals(32, stats.monthlyTempC()[11], 0.001);
        assertEquals(310, stats.monthlyRainMm()[7], 0.001);   // 10 mm x 31 days
        assertEquals(0, stats.monthlyRainMm()[0], 0.001);
    }

    @Test void windGustsAreReadFromTheForecast() {
        String json = "{\"current\":{\"temperature_2m\":28,\"weather_code\":1,\"relative_humidity_2m\":70},\"daily\":{\"time\":[\"2026-06-01\"],"
                + "\"weather_code\":[95],\"temperature_2m_max\":[31],\"temperature_2m_min\":[25],\"precipitation_sum\":[60],\"wind_gusts_10m_max\":[72.5]}}";
        WeatherForecast f = OpenMeteoClient.parseForecast(json);
        assertEquals(72.5, f.days().get(0).windGustKmh(), 0.001);
    }
}
