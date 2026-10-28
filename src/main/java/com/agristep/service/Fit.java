package com.agristep.service;

/** How well a crop matches the garden's conditions. */
public enum Fit {
    GREAT("Great fit"),
    GOOD("Good fit"),
    POSSIBLE("Possible"),
    POOR("Not recommended");

    private final String label;

    Fit(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static Fit fromScore(int score) {
        if (score >= 75) return GREAT;
        if (score >= 55) return GOOD;
        if (score >= 30) return POSSIBLE;
        return POOR;
    }
}
