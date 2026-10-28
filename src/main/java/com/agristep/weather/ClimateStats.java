package com.agristep.weather;

/**
 * What the last year of weather looked like at a place: used to judge which crops fit.
 * The two month arrays (index 0 = January) are optional; they feed the planting calendar.
 */
public record ClimateStats(double avgTemp30dC, double annualRainMm, double[] monthlyTempC, double[] monthlyRainMm) {

    public ClimateStats {
        if (monthlyTempC != null && monthlyTempC.length != 12) throw new IllegalArgumentException("monthlyTempC needs 12 values");
        if (monthlyRainMm != null && monthlyRainMm.length != 12) throw new IllegalArgumentException("monthlyRainMm needs 12 values");
    }

    public ClimateStats(double avgTemp30dC, double annualRainMm) {
        this(avgTemp30dC, annualRainMm, null, null);
    }

    public boolean hasMonthly() { return monthlyTempC != null && monthlyRainMm != null; }

    /** The same climate seen through one month's temperature (month 1 to 12): used by the planting calendar. */
    public ClimateStats forMonth(int month) {
        if (!hasMonthly()) return this;
        return new ClimateStats(monthlyTempC[month - 1], annualRainMm, monthlyTempC, monthlyRainMm);
    }
}
