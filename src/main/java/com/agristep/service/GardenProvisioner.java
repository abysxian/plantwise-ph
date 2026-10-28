package com.agristep.service;

import com.agristep.domain.Plot;
import com.agristep.repository.PlotRepository;

/** Gives the signed-in person a fresh garden: nine empty beds and default settings. */
public class GardenProvisioner {
    static final int STARTER_BEDS = 9;

    private final PlotRepository plots;
    private final SettingsService settings;

    public GardenProvisioner(PlotRepository plots, SettingsService settings) {
        this.plots = plots;
        this.settings = settings;
    }

    public void provision() {
        if (plots.count() == 0) {
            for (int slot = 1; slot <= STARTER_BEDS; slot++) plots.save(new Plot(slot));
        }
        settings.current();   // creates the default settings if they don't exist yet
    }
}
