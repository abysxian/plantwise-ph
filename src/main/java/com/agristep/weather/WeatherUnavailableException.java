package com.agristep.weather;

/** The weather service could not be reached or returned something unusable. */
public class WeatherUnavailableException extends RuntimeException {
    public WeatherUnavailableException(String message) { super(message); }
    public WeatherUnavailableException(String message, Throwable cause) { super(message, cause); }
}
