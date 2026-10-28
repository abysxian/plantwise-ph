package com.agristep.weather;

import java.util.List;

/**
 * Abstraction: the rest of the app depends on this interface, not on a specific weather website.
 * {@link OpenMeteoClient} is the real implementation; tests use a simple fake.
 */
public interface WeatherProvider {
    WeatherForecast fetchForecast(double latitude, double longitude);

    ClimateStats fetchClimate(double latitude, double longitude);

    List<GeoPlace> searchPlaces(String query);
}
