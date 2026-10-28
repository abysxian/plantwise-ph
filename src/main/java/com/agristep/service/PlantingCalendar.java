package com.agristep.service;

import com.agristep.domain.Crop;
import com.agristep.domain.SoilType;
import com.agristep.weather.ClimateStats;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * A month-by-month view of the planting score. Each month is scored with that month's average temperature from
 * the last year of weather at the garden's location (the same scoring used on the Today screen).
 */
public class PlantingCalendar {
    private final SuitabilityScorer scorer;

    public PlantingCalendar(SuitabilityScorer scorer) {
        this.scorer = scorer;
    }

    public record MonthFit(int month, int score, Fit fit) {
        public String monthName() { return PlantingCalendar.monthName(month); }
    }

    public static String monthName(int month) { return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH); }

    public static String shortMonthName(int month) { return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH); }

    /** True when the saved climate has month-by-month figures. */
    public boolean available(ClimateStats climate) { return climate != null && climate.hasMonthly(); }

    /** The twelve months for one crop. */
    public List<MonthFit> yearFor(Crop crop, ClimateStats climate, SoilType soil) {
        requireMonthly(climate);
        return IntStream.rangeClosed(1, 12).mapToObj(m -> {
            CropRecommendation r = scorer.score(crop, climate.forMonth(m), soil);
            return new MonthFit(m, r.score(), r.fit());
        }).toList();
    }

    /** All crops ranked for one month (1 to 12), best first. */
    public List<CropRecommendation> bestFor(int month, List<Crop> crops, ClimateStats climate, SoilType soil) {
        requireMonthly(climate);
        if (month < 1 || month > 12) throw new IllegalArgumentException("Month must be 1 to 12.");
        ClimateStats forMonth = climate.forMonth(month);
        return crops.stream().map(c -> scorer.score(c, forMonth, soil))
                .sorted(Comparator.comparingInt(CropRecommendation::score).reversed()
                        .thenComparing(r -> r.crop().getName())).toList();
    }

    /** "About 27.5 °C and wet (around 380 mm of rain)". */
    public String describeMonth(ClimateStats climate, int month) {
        requireMonthly(climate);
        double temp = climate.monthlyTempC()[month - 1];
        double rain = climate.monthlyRainMm()[month - 1];
        String kind = rain < 100 ? "dry" : rain < 250 ? "moderately wet" : "wet";
        return String.format(Locale.ROOT, "About %.1f °C and %s (around %.0f mm of rain).", temp, kind, rain);
    }

    private static void requireMonthly(ClimateStats climate) {
        if (climate == null || !climate.hasMonthly()) throw new IllegalStateException("Month-by-month weather is not available yet.");
    }
}
