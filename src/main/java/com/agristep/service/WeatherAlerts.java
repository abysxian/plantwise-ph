package com.agristep.service;

import com.agristep.weather.DailyWeather;
import com.agristep.weather.WeatherForecast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Looks through the 7-day forecast for weather that can hurt a garden: very heavy rain, strong wind gusts,
 * thunderstorms and extreme heat. These are rule-of-thumb warnings from forecast numbers. They are not official
 * typhoon or storm bulletins, so the advice always points to PAGASA for those.
 */
public final class WeatherAlerts {
    private WeatherAlerts() {}

    public enum Level { WATCH, WARNING, DANGER }

    public record Alert(Level level, String title, String advice, LocalDate date) {}

    public static final String OFFICIAL = "For official storm and typhoon bulletins, check PAGASA (pagasa.dost.gov.ph).";

    public static List<Alert> from(WeatherForecast forecast) {
        List<Alert> alerts = new ArrayList<>();
        if (forecast == null) return alerts;

        DailyWeather wettest = null, windiest = null, stormy = null, hottest = null;
        for (DailyWeather d : forecast.days()) {
            if (wettest == null || d.rainMm() > wettest.rainMm()) wettest = d;
            if (windiest == null || d.windGustKmh() > windiest.windGustKmh()) windiest = d;
            if (stormy == null && isThunderstorm(d.weatherCode())) stormy = d;
            if (hottest == null || d.tempMaxC() > hottest.tempMaxC()) hottest = d;
        }

        if (wettest != null && wettest.rainMm() >= 50) {
            Level level = wettest.rainMm() >= 100 ? Level.DANGER : Level.WARNING;
            alerts.add(new Alert(level, String.format(Locale.ROOT, "Heavy rain: about %.0f mm expected", wettest.rainMm()),
                    "Harvest anything that is ready before the rain. Clear drainage ditches, lift pots off the ground and "
                            + "skip watering and fertilising. Newly sown seeds can wash away.", wettest.date()));
        }
        if (windiest != null && windiest.windGustKmh() >= 60) {
            Level level = windiest.windGustKmh() >= 90 ? Level.DANGER : Level.WARNING;
            alerts.add(new Alert(level, String.format(Locale.ROOT, "Strong wind: gusts up to %.0f km/h", windiest.windGustKmh()),
                    "Stake or tie tall and climbing crops (sitao, ampalaya, kamatis), cover young seedlings and move pots "
                            + "to a sheltered spot. " + OFFICIAL, windiest.date()));
        }
        if (stormy != null) {
            alerts.add(new Alert(Level.WATCH, "Thunderstorm in the forecast",
                    "Stay out of the garden during lightning and put tools away. Check the beds for damage afterwards.", stormy.date()));
        }
        if (hottest != null && hottest.tempMaxC() >= 36) {
            alerts.add(new Alert(hottest.tempMaxC() >= 38 ? Level.WARNING : Level.WATCH,
                    String.format(Locale.ROOT, "Extreme heat: up to %.0f °C", hottest.tempMaxC()),
                    "Water early in the morning and again in the late afternoon, mulch the soil and shade tender leafy crops "
                            + "(pechay, letsugas) with a net.", hottest.date()));
        }
        alerts.sort(java.util.Comparator.comparing(Alert::level).reversed().thenComparing(Alert::date));
        return alerts;
    }

    private static boolean isThunderstorm(int code) { return code == 95 || code == 96 || code == 99; }
}
