package com.agristep;

import com.agristep.config.DataSeeder;
import com.agristep.persistence.*;
import com.agristep.repository.*;
import com.agristep.service.*;
import com.agristep.weather.WeatherProvider;

import java.io.IOException;
import java.time.Clock;

/**
 * The composition root: builds every repository and service once and wires them together.
 * The screens only ever talk to the services held here.
 */
public final class AppContext {
    public final Database database;
    public final Session session;
    public final AuthService auth;
    public final CropRepository crops;
    public final SettingsService settings;
    public final WeatherService weather;
    public final RecommendationService recommendations;
    public final GardenService garden;
    public final GardenAdvisor advisor;
    public final GardenGridService grid;
    private final DataSeeder seeder;

    private AppContext(Database db, WeatherProvider provider, Clock clock) {
        this.database = db;
        this.session = new Session();
        this.crops = new JdbcCropRepository(db);
        PlotRepository plots = new JdbcPlotRepository(db, crops, session);
        CareTaskRepository tasks = new JdbcCareTaskRepository(db, session);
        HarvestRecordRepository harvests = new JdbcHarvestRecordRepository(db, session);
        this.settings = new SettingsService(new JdbcSettingsRepository(db, session));
        this.weather = new WeatherService(provider, new JdbcClimateProfileRepository(db), clock);
        this.recommendations = new RecommendationService(crops, weather, new ClimateSuitabilityScorer());
        this.garden = new GardenService(plots, crops, tasks, harvests, new TaskPlanner(), clock, db);
        this.grid = new GardenGridService(plots, settings, db);
        this.advisor = new GardenAdvisor();
        this.auth = new AuthService(new JdbcUserRepository(db), new PasswordHasher(), session,
                new GardenProvisioner(plots, settings), new JdbcLegacyData(db), db, clock);
        this.seeder = new DataSeeder(crops, db);
    }

    public static AppContext create(Database db, WeatherProvider provider, Clock clock) {
        return new AppContext(db, provider, clock);
    }

    /** Creates or upgrades the tables, adds missing crops, and makes the demo account on a brand-new database. */
    public void seed() throws IOException {
        database.initSchema();
        seeder.run();
        auth.ensureDemoAccount();
    }
}
