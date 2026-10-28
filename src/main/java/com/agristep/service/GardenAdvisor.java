package com.agristep.service;

import com.agristep.weather.DailyWeather;
import com.agristep.weather.WeatherForecast;

import java.util.Locale;
import java.util.Optional;

/** Writes the one-sentence "what should I do today?" message at the top of the Today screen. */
public class GardenAdvisor {

    public String todayAdvice(Optional<WeatherForecast> forecast, long dueTasks, long readyBeds) {
        StringBuilder text = new StringBuilder(weatherSentence(forecast));
        if (dueTasks > 0) {
            text.append(' ').append(dueTasks == 1 ? "1 task is due" : dueTasks + " tasks are due").append(" in your garden.");
        }
        if (readyBeds > 0) {
            text.append(' ').append(readyBeds == 1 ? "1 bed is ready to harvest." : readyBeds + " beds are ready to harvest.");
        }
        return text.toString();
    }

    private String weatherSentence(Optional<WeatherForecast> forecast) {
        if (forecast.isEmpty()) return "Weather is not available right now.";
        WeatherForecast f = forecast.get();
        DailyWeather today = f.today().orElseThrow();
        if (today.rainMm() >= 10) {
            return String.format(Locale.ROOT, "Rain today (about %.0f mm). Skip watering and hold off on sowing seeds.", today.rainMm());
        }
        if (today.tempMaxC() >= 33) {
            return String.format(Locale.ROOT, "Hot day ahead, up to %.0f °C. Water early in the morning or late afternoon.", today.tempMaxC());
        }
        if (f.rainInNextDays(3) < 1) {
            return "A dry spell is coming. Check the soil with your finger every day.";
        }
        return "Fair weather for garden chores.";
    }
}
