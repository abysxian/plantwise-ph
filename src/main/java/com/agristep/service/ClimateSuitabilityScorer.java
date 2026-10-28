package com.agristep.service;

import com.agristep.domain.Crop;
import com.agristep.domain.SoilType;
import com.agristep.weather.ClimateStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Scores a crop from recent temperature (50%), soil (30%) and yearly rainfall (20%). */
public class ClimateSuitabilityScorer implements SuitabilityScorer {

    private static final double TEMP_WEIGHT = 0.5;
    private static final double SOIL_WEIGHT = 0.3;
    private static final double RAIN_WEIGHT = 0.2;

    @Override
    public CropRecommendation score(Crop crop, ClimateStats climate, SoilType soil) {
        double soilFit = crop.soilFit(soil);
        List<String> reasons = new ArrayList<>();

        if (climate == null) {
            reasons.add("Weather data was not available, so this match uses your soil type only.");
            reasons.add(soilReason(soilFit));
            int score = (int) Math.round(soilFit * 100);
            return new CropRecommendation(crop, score, Fit.fromScore(score), reasons);
        }

        double tempFit = crop.getTemperature().fit(climate.avgTemp30dC());
        double rainFit = crop.getRainfall().fit(climate.annualRainMm());

        reasons.add(temperatureReason(crop, climate.avgTemp30dC(), tempFit));
        reasons.add(rainReason(crop, climate.annualRainMm(), rainFit));
        reasons.add(soilReason(soilFit));

        int score = (int) Math.round((TEMP_WEIGHT * tempFit + RAIN_WEIGHT * rainFit + SOIL_WEIGHT * soilFit) * 100);
        if (tempFit == 0.0) {
            score = Math.min(score, 15);       // the plant cannot survive this temperature
        } else if (soilFit == 0.0) {
            score = Math.min(score, 45);       // soil can be improved, so not a hard stop
        }
        return new CropRecommendation(crop, score, Fit.fromScore(score), reasons);
    }

    private String temperatureReason(Crop crop, double temp, double fit) {
        String now = String.format(Locale.ROOT, "%.1f °C", temp);
        if (fit >= 1.0) {
            return "Recent average temperature is " + now + ", right in its ideal range (" + crop.getTemperature().describeOptimal("°C") + ").";
        }
        if (fit > 0.0) {
            return "Recent average temperature is " + now + ". It will grow, but it prefers " + crop.getTemperature().describeOptimal("°C") + ".";
        }
        return "Recent average temperature is " + now + ", outside what it survives (" + crop.getTemperature().describeAbsolute("°C") + ").";
    }

    private String rainReason(Crop crop, double rain, double fit) {
        String now = String.format(Locale.ROOT, "%,.0f mm", rain);
        if (fit >= 1.0) {
            return "Rain over the past year was " + now + ", inside its ideal " + crop.getRainfall().describeOptimal("mm") + ".";
        }
        if (fit > 0.0) {
            return "Rain over the past year was " + now + " (ideal is " + crop.getRainfall().describeOptimal("mm") + "). Adjust your watering to make up the difference.";
        }
        return "Rain over the past year was " + now + ", outside what it tolerates. It would need careful watering or shelter.";
    }

    private String soilReason(double soilFit) {
        if (soilFit >= 1.0) return "Your soil is ideal for this crop.";
        if (soilFit > 0.0) return "Your soil works, but it is not this crop's favourite. Mix in compost.";
        return "This crop struggles in your soil type. Try a raised bed filled with garden soil and compost.";
    }
}
