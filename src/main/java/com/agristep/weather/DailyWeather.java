package com.agristep.weather;

import java.time.LocalDate;

/** One day of forecast. */
public record DailyWeather(LocalDate date, double tempMaxC, double tempMinC, double rainMm, int weatherCode) {}
