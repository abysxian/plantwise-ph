package com.agristep;

import com.agristep.persistence.Database;
import com.agristep.weather.WeatherProvider;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

/** Test helpers: a fresh in-memory database (MySQL mode, like the real one) per test. */
public final class TestDb {
    private TestDb() {}

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static Database memory() {
        return new Database("jdbc:h2:mem:t" + COUNTER.incrementAndGet() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    public static AppContext app(WeatherProvider weather, Clock clock) {
        try {
            AppContext ctx = AppContext.create(memory(), weather, clock);
            ctx.seed();
            ctx.auth.register("tester", "Tester", "secret1", "secret1");
            return ctx;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
