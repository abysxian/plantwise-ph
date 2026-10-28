package com.agristep.service;

import com.agristep.domain.GardenSettings;
import com.agristep.repository.CropRepository;
import com.agristep.weather.ClimateStats;
import com.agristep.weather.WeatherUnavailableException;

import java.util.Comparator;
import java.util.List;

/** Ranks every crop in the database for the garden's place and soil. */
public class RecommendationService {

    private final CropRepository crops;
    private final WeatherService weather;
    private final SuitabilityScorer scorer;

    public RecommendationService(CropRepository crops, WeatherService weather, SuitabilityScorer scorer) {
        this.crops = crops;
        this.weather = weather;
        this.scorer = scorer;
    }

    public RecommendationResult recommend(GardenSettings settings) {
        ClimateStats stats = null;
        boolean stale = false;
        try {
            WeatherService.ClimateResult climate = weather.climate(settings);
            stats = climate.stats();
            stale = climate.stale();
        } catch (WeatherUnavailableException e) {
            // keep going with soil-only scoring; the screen tells the user
        }
        final ClimateStats usable = stats;
        List<CropRecommendation> ranked = crops.findAllByOrderByNameAsc().stream()
                .map(crop -> scorer.score(crop, usable, settings.getSoil()))
                .sorted(Comparator.comparingInt(CropRecommendation::score).reversed()
                        .thenComparing(r -> r.crop().getName()))
                .toList();
        return new RecommendationResult(ranked, usable == null, stale);
    }
}
