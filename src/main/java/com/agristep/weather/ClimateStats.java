package com.agristep.weather;

/** What the last year of weather looked like at a place: used to judge which crops fit. */
public record ClimateStats(double avgTemp30dC, double annualRainMm) {}
