package com.agristep.domain;

import com.agristep.weather.ClimateStats;

import java.time.Duration;
import java.time.LocalDateTime;

/** Saved summary of a place's last-year climate, so the app works offline and spares the weather API. */
public class ClimateProfile {
    private Long id;
    private final String locationKey;
    private final String locationName;
    private double avgTemp30dC;
    private double annualRainMm;
    private LocalDateTime fetchedAt;

    public ClimateProfile(String locationKey, String locationName, ClimateStats stats, LocalDateTime fetchedAt) {
        this.locationKey = locationKey;
        this.locationName = locationName;
        refresh(stats, fetchedAt);
    }

    public void refresh(ClimateStats stats, LocalDateTime now) {
        this.avgTemp30dC = stats.avgTemp30dC();
        this.annualRainMm = stats.annualRainMm();
        this.fetchedAt = now;
    }

    public boolean isFresh(LocalDateTime now, Duration maxAge) {
        return Duration.between(fetchedAt, now).compareTo(maxAge) < 0;
    }

    public ClimateStats toStats() { return new ClimateStats(avgTemp30dC, annualRainMm); }

    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("This profile already has id " + this.id);
        }
        this.id = id;
    }

    public Long getId() { return id; }
    public String getLocationKey() { return locationKey; }
    public String getLocationName() { return locationName; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
}
