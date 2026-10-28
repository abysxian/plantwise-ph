package com.agristep.weather;

import java.time.LocalDate;

/** One day of forecast. {@code windGustKmh} is the strongest gust expected (0 when unknown). */
public record DailyWeather(LocalDate date, double tempMaxC, double tempMinC, double rainMm, int weatherCode, double windGustKmh) {

    public DailyWeather(LocalDate date, double tempMaxC, double tempMinC, double rainMm, int weatherCode) {
        this(date, tempMaxC, tempMinC, rainMm, weatherCode, 0);
    }
}
