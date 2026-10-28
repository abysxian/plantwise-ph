package com.agristep.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Real {@link WeatherProvider} backed by Open-Meteo (https://open-meteo.com), a free weather API
 * that needs no key. Data is licensed CC BY 4.0, free for non-commercial use.
 * <p>
 * Network code and JSON parsing are separate: the parse methods are static and tested with sample responses.
 */
public class OpenMeteoClient implements WeatherProvider {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Clock clock;

    public OpenMeteoClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public WeatherForecast fetchForecast(double latitude, double longitude) {
        return parseForecast(get(forecastUrl(latitude, longitude)));
    }

    /** Summarises the last year of observed weather (the archive lags a few days behind real time). */
    @Override
    public ClimateStats fetchClimate(double latitude, double longitude) {
        LocalDate end = LocalDate.now(clock).minusDays(7);
        LocalDate start = end.minusDays(364);
        return parseClimate(get(archiveUrl(latitude, longitude, start, end)));
    }

    @Override
    public List<GeoPlace> searchPlaces(String query) {
        return parsePlaces(get(geocodingUrl(query)));
    }

    // ---- URLs ----

    static String forecastUrl(double lat, double lon) {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + plain(lat) + "&longitude=" + plain(lon)
                + "&current=temperature_2m,relative_humidity_2m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_gusts_10m_max"
                + "&forecast_days=7&timezone=auto";
    }

    static String archiveUrl(double lat, double lon, LocalDate start, LocalDate end) {
        return "https://archive-api.open-meteo.com/v1/archive?latitude=" + plain(lat) + "&longitude=" + plain(lon)
                + "&start_date=" + start + "&end_date=" + end
                + "&daily=temperature_2m_mean,precipitation_sum&timezone=auto";
    }

    static String geocodingUrl(String name) {
        return "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&count=6&language=en&format=json";
    }

    /** Dot-decimal text for a coordinate, whatever the computer's language settings are. */
    private static String plain(double v) {
        return BigDecimal.valueOf(v).toPlainString();
    }

    // ---- parsing ----

    static WeatherForecast parseForecast(String json) {
        JsonNode root = read(json);
        JsonNode current = root.path("current");
        JsonNode daily = root.path("daily");
        JsonNode times = daily.path("time");
        if (!times.isArray() || times.isEmpty()) {
            throw new WeatherUnavailableException("The forecast response had no daily data.");
        }
        List<DailyWeather> days = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            days.add(new DailyWeather(
                    LocalDate.parse(times.get(i).asText()),
                    daily.path("temperature_2m_max").path(i).asDouble(0),
                    daily.path("temperature_2m_min").path(i).asDouble(0),
                    daily.path("precipitation_sum").path(i).asDouble(0),
                    daily.path("weather_code").path(i).asInt(0),
                    daily.path("wind_gusts_10m_max").path(i).asDouble(0)));
        }
        return new WeatherForecast(
                current.path("temperature_2m").asDouble(days.get(0).tempMaxC()),
                current.path("weather_code").asInt(days.get(0).weatherCode()),
                current.path("relative_humidity_2m").asInt(0),
                List.copyOf(days));
    }

    static ClimateStats parseClimate(String json) {
        JsonNode daily = read(json).path("daily");
        JsonNode times = daily.path("time");
        List<Double> temps = numbers(daily.path("temperature_2m_mean"));
        List<Double> rain = numbers(daily.path("precipitation_sum"));
        if (temps.isEmpty() || rain.isEmpty()) {
            throw new WeatherUnavailableException("The history response had no usable data.");
        }
        List<Double> lastMonth = temps.subList(Math.max(0, temps.size() - 30), temps.size());
        double avgTemp = lastMonth.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double annualRain = rain.stream().mapToDouble(Double::doubleValue).sum() / rain.size() * 365.0;

        // Average temperature and typical rain total for each calendar month, from the same year of data.
        double[] tempSum = new double[12];
        int[] tempDays = new int[12];
        double[] rainSum = new double[12];
        int[] rainDays = new int[12];
        JsonNode tNode = daily.path("temperature_2m_mean");
        JsonNode rNode = daily.path("precipitation_sum");
        for (int i = 0; i < times.size(); i++) {
            int m;
            try {
                m = LocalDate.parse(times.get(i).asText()).getMonthValue() - 1;
            } catch (RuntimeException e) {
                continue;
            }
            if (tNode.path(i).isNumber()) { tempSum[m] += tNode.get(i).asDouble(); tempDays[m]++; }
            if (rNode.path(i).isNumber()) { rainSum[m] += rNode.get(i).asDouble(); rainDays[m]++; }
        }
        double[] monthlyTemp = new double[12];
        double[] monthlyRain = new double[12];
        boolean complete = true;
        for (int m = 0; m < 12; m++) {
            if (tempDays[m] == 0 || rainDays[m] == 0) { complete = false; break; }
            monthlyTemp[m] = tempSum[m] / tempDays[m];
            monthlyRain[m] = rainSum[m] / rainDays[m] * java.time.Month.of(m + 1).length(false);
        }
        return complete ? new ClimateStats(avgTemp, annualRain, monthlyTemp, monthlyRain) : new ClimateStats(avgTemp, annualRain);
    }

    static List<GeoPlace> parsePlaces(String json) {
        List<GeoPlace> places = new ArrayList<>();
        for (JsonNode r : read(json).path("results")) {
            places.add(new GeoPlace(
                    r.path("name").asText(""),
                    r.path("admin1").asText(null),
                    r.path("country").asText(null),
                    r.path("latitude").asDouble(),
                    r.path("longitude").asDouble()));
        }
        return places;
    }

    private static List<Double> numbers(JsonNode array) {
        List<Double> values = new ArrayList<>();
        for (JsonNode n : array) {
            if (n.isNumber()) values.add(n.asDouble());   // skips nulls for days not yet available
        }
        return values;
    }

    private static JsonNode read(String json) {
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException e) {
            throw new WeatherUnavailableException("The weather service sent an unreadable answer.", e);
        }
    }

    // ---- network ----

    private String get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WeatherUnavailableException("The weather service answered with status " + response.statusCode() + ".");
            }
            return response.body();
        } catch (IOException e) {
            throw new WeatherUnavailableException("Could not reach the weather service.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherUnavailableException("The weather request was interrupted.", e);
        }
    }
}
