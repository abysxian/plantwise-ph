package com.agristep.domain;

/**
 * Value object: the range of a growing condition a crop can live in (FAO EcoCrop style).
 * "Optimal" is where the crop thrives; "absolute" is the edge where it can still survive.
 * Encapsulation: the ordering rule (absMin <= optMin <= optMax <= absMax) is enforced here.
 */
public final class ClimateRange {
    private final double absMin;
    private final double optMin;
    private final double optMax;
    private final double absMax;

    public ClimateRange(double absMin, double optMin, double optMax, double absMax) {
        if (!(absMin <= optMin && optMin <= optMax && optMax <= absMax)) {
            throw new IllegalArgumentException(
                    "Range must satisfy absMin <= optMin <= optMax <= absMax but was "
                            + absMin + ", " + optMin + ", " + optMax + ", " + absMax);
        }
        this.absMin = absMin;
        this.optMin = optMin;
        this.optMax = optMax;
        this.absMax = absMax;
    }

    /** 1.0 inside the optimal band, 0.0 outside the survivable band, linear in between. */
    public double fit(double value) {
        if (value < absMin || value > absMax) return 0.0;
        if (value >= optMin && value <= optMax) return 1.0;
        if (value < optMin) return (value - absMin) / (optMin - absMin);
        return (absMax - value) / (absMax - optMax);
    }

    public String describeOptimal(String unit) { return describe(optMin, optMax, unit); }

    public String describeAbsolute(String unit) { return describe(absMin, absMax, unit); }

    private static String describe(double lo, double hi, String unit) {
        String text = format(lo) + "–" + format(hi);
        return unit == null || unit.isBlank() ? text : text + " " + unit;
    }

    private static String format(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(Math.round(v * 10) / 10.0);
    }

    public double getAbsMin() { return absMin; }
    public double getOptMin() { return optMin; }
    public double getOptMax() { return optMax; }
    public double getAbsMax() { return absMax; }
}
