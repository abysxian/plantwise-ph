package com.agristep.service;

import com.agristep.domain.GardenSettings;
import com.agristep.domain.SoilType;
import com.agristep.repository.GardenSettingsRepository;

/** Reads and changes where the garden is and what soil it has. */
public class SettingsService {

    private final GardenSettingsRepository repository;

    public SettingsService(GardenSettingsRepository repository) {
        this.repository = repository;
    }

    /** Returns the saved settings, creating a Manila / garden-soil default on first use. */
    public GardenSettings current() {
        return repository.findCurrent().orElseGet(() ->
                repository.save(new GardenSettings("Manila, Philippines", 14.5995, 120.9842, SoilType.LOAM)));
    }

    public GardenSettings changeSoil(SoilType soil) {
        GardenSettings settings = current();
        settings.useSoil(soil);
        return repository.save(settings);
    }

    public GardenSettings changeLocation(String name, double latitude, double longitude) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("The place needs a name.");
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Those coordinates are not on Earth.");
        }
        GardenSettings settings = current();
        settings.moveTo(name.trim(), latitude, longitude);
        return repository.save(settings);
    }
}
