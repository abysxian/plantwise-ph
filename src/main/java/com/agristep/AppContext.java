package com.agristep;

import com.agristep.config.DataSeeder;
import com.agristep.persistence.*;
import com.agristep.repository.*;
import com.agristep.service.*;
import com.agristep.weather.WeatherProvider;

import java.io.IOException;
import java.nio.file.Path;
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
    public final PlantingCalendar calendar;
    public final PlantingAdvisor planting;
    public final JournalService journal;
    public final ExportService export;
    public final SharingService sharing;
    public final AccountService account;
    private final DataSeeder seeder;

    private AppContext(Database db, WeatherProvider provider, Clock clock, Path photoFolder) {
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
        SharingRepository sharingRepo = new JdbcSharingRepository(db);
        this.auth = new AuthService(new JdbcUserRepository(db), new PasswordHasher(), session,
                new GardenProvisioner(plots, settings), new JdbcLegacyData(db), db, clock, sharingRepo);
        this.calendar = new PlantingCalendar(new ClimateSuitabilityScorer());
        this.planting = new PlantingAdvisor(crops);
        PhotoStore photos = new PhotoStore(photoFolder);
        this.journal = new JournalService(new JdbcJournalRepository(db, session), photos, garden, session, clock);
        this.export = new ExportService(garden, settings, session, clock);
        this.sharing = new SharingService(sharingRepo, new JdbcUserRepository(db), session, db, clock);
        this.account = new AccountService(new JdbcUserRepository(db), new PasswordHasher(), session, new JdbcAccountData(db), photos, db);
        this.seeder = new DataSeeder(crops, db);
    }

    /** Photos go to a fresh temporary folder (used by tests and quick experiments). */
    public static AppContext create(Database db, WeatherProvider provider, Clock clock) {
        try {
            return new AppContext(db, provider, clock, java.nio.file.Files.createTempDirectory("plantwise-photos"));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** @param photoFolder where journal pictures are kept */
    public static AppContext create(Database db, WeatherProvider provider, Clock clock, Path photoFolder) {
        return new AppContext(db, provider, clock, photoFolder);
    }

    /** The folder journal pictures are normally kept in: {@code ~/.agristep/journal}. */
    public static Path defaultPhotoFolder() {
        return Path.of(System.getProperty("user.home", "."), ".agristep", "journal");
    }

    /** Creates or upgrades the tables, adds missing crops, and makes the demo account on a brand-new database. */
    public void seed() throws IOException {
        database.initSchema();
        seeder.run();
        auth.ensureDemoAccount();
    }
}
