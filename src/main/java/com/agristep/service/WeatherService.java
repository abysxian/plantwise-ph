package com.agristep.service;

import com.agristep.domain.ClimateProfile;
import com.agristep.domain.GardenSettings;
import com.agristep.repository.ClimateProfileRepository;
import com.agristep.weather.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives the rest of the app weather without caring where it comes from.
 * Forecasts are kept in memory for 15 minutes. Yearly climate is saved in the database for 24 hours
 * and reused as a fallback if the weather service is unreachable (for example, no Wi-Fi at the presentation).
 */
public class WeatherService {

    static final Duration CLIMATE_FRESH_FOR = Duration.ofHours(24);
    static final Duration FORECAST_FRESH_FOR = Duration.ofMinutes(15);

    /** @param stale true when the weather service was down and an older saved value is being shown */
    public record ClimateResult(ClimateStats stats, boolean stale, LocalDateTime fetchedAt) {}

    private record CachedForecast(WeatherForecast forecast, Instant fetchedAt) {}

    private final WeatherProvider provider;
    private final ClimateProfileRepository profiles;
    private final Clock clock;
    private final Map<String, CachedForecast> forecasts = new ConcurrentHashMap<>();

    public WeatherService(WeatherProvider provider, ClimateProfileRepository profiles, Clock clock) {
        this.provider = provider;
        this.profiles = profiles;
        this.clock = clock;
    }

    public Optional<WeatherForecast> forecast(GardenSettings settings) {
        String key = settings.locationKey();
        Instant now = clock.instant();
        CachedForecast cached = forecasts.get(key);
        if (cached != null && Duration.between(cached.fetchedAt(), now).compareTo(FORECAST_FRESH_FOR) < 0) {
            return Optional.of(cached.forecast());
        }
        try {
            WeatherForecast fresh = provider.fetchForecast(settings.getLatitude(), settings.getLongitude());
            forecasts.put(key, new CachedForecast(fresh, now));
            return Optional.of(fresh);
        } catch (WeatherUnavailableException e) {
            return Optional.ofNullable(cached).map(CachedForecast::forecast);
        }
    }

    public ClimateResult climate(GardenSettings settings) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<ClimateProfile> saved = profiles.findByLocationKey(settings.locationKey());

        if (saved.isPresent() && saved.get().isFresh(now, CLIMATE_FRESH_FOR)) {
            return new ClimateResult(saved.get().toStats(), false, saved.get().getFetchedAt());
        }
        try {
            ClimateStats stats = provider.fetchClimate(settings.getLatitude(), settings.getLongitude());
            ClimateProfile profile = saved.orElseGet(
                    () -> new ClimateProfile(settings.locationKey(), settings.getLocationName(), stats, now));
            profile.refresh(stats, now);
            profiles.save(profile);
            return new ClimateResult(stats, false, now);
        } catch (WeatherUnavailableException e) {
            if (saved.isPresent()) {
                return new ClimateResult(saved.get().toStats(), true, saved.get().getFetchedAt());
            }
            throw e;
        }
    }

    public List<GeoPlace> searchPlaces(String query) {
        if (query == null || query.isBlank()) return List.of();
        return provider.searchPlaces(query.trim());
    }
}
