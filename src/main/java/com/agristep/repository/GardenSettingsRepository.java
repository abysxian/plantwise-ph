package com.agristep.repository;

import com.agristep.domain.GardenSettings;

import java.util.Optional;

/** The signed-in person's garden settings (one per user). */
public interface GardenSettingsRepository {
    Optional<GardenSettings> findCurrent();

    GardenSettings save(GardenSettings settings);
}
