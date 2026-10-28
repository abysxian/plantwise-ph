package com.agristep.service;

import com.agristep.domain.Crop;

import java.util.List;

/** A crop with its 0-100 score, the fit band, and plain-language reasons. */
public record CropRecommendation(Crop crop, int score, Fit fit, List<String> reasons) {}
