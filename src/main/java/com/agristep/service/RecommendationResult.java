package com.agristep.service;

import java.util.List;

/**
 * @param degraded true when weather was unavailable and ranking uses soil only
 * @param stale    true when saved (older) climate data was used
 */
public record RecommendationResult(List<CropRecommendation> items, boolean degraded, boolean stale) {}
