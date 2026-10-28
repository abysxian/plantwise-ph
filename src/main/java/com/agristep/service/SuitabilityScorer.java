package com.agristep.service;

import com.agristep.domain.Crop;
import com.agristep.domain.SoilType;
import com.agristep.weather.ClimateStats;

/**
 * Abstraction (Strategy pattern): callers ask "how well does this crop fit?" without knowing the
 * formula. A different scoring method can be swapped in by writing another implementation.
 */
public interface SuitabilityScorer {
    /** @param climate may be null when weather data could not be fetched; scoring then uses soil only. */
    CropRecommendation score(Crop crop, ClimateStats climate, SoilType soil);
}
