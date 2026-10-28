package com.agristep.domain;

import java.util.Locale;

/** One person's garden-wide settings: where the garden is and what soil it has. */
public class GardenSettings {
    public static final int DEFAULT_GRID_COLUMNS = 3;

    private String locationName;
    private double latitude;
    private double longitude;
    private SoilType soil;
    private int gridColumns = DEFAULT_GRID_COLUMNS;

    public GardenSettings(String locationName, double latitude, double longitude, SoilType soil) {
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.soil = soil;
    }

    public void moveTo(String locationName, double latitude, double longitude) {
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void useGridColumns(int columns) { this.gridColumns = columns; }

    public void useSoil(SoilType soil) { this.soil = soil; }

    /** Rounded coordinates (about 1 km) used as the cache key for climate data. */
    public String locationKey() {
        return String.format(Locale.ROOT, "%.2f,%.2f", latitude, longitude);
    }

    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public SoilType getSoil() { return soil; }
    public int getGridColumns() { return gridColumns; }
}
