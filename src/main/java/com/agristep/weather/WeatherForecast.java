package com.agristep.weather;

import java.util.List;
import java.util.Optional;

/** Current conditions plus a short daily forecast. */
public record WeatherForecast(double currentTempC, int currentWeatherCode, int humidityPercent, List<DailyWeather> days) {

    public double rainInNextDays(int n) {
        return days.stream().limit(n).mapToDouble(DailyWeather::rainMm).sum();
    }

    public double maxTempInNextDays(int n) {
        return days.stream().limit(n).mapToDouble(DailyWeather::tempMaxC).max().orElse(currentTempC);
    }

    public Optional<DailyWeather> today() {
        return days.stream().findFirst();
    }
}
