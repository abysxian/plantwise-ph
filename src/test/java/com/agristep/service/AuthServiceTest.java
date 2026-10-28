package com.agristep.service;

import com.agristep.AppContext;
import com.agristep.MutableClock;
import com.agristep.TestDb;
import com.agristep.domain.Plot;
import com.agristep.domain.SoilType;
import com.agristep.domain.ValidationException;
import com.agristep.persistence.Database;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private static AppContext fresh(MutableClock clock) throws Exception {
        AppContext ctx = AppContext.create(TestDb.memory(), new ServicesTest.CountingWeather(), clock);
        ctx.seed();
        return ctx;
    }

    private static MutableClock clock() { return new MutableClock(Instant.parse("2026-06-01T00:00:00Z")); }

    @Test void demoAccountIsProvidedAndWorks() throws Exception {
        AppContext ctx = fresh(clock());
        assertFalse(ctx.session.isSignedIn());
        ctx.auth.login("demo", "demo1234");
        assertEquals(9, ctx.garden.allPlots().size());
        ctx.auth.logout();
        ctx.seed();   // a second start does not create another demo or sign anyone in
        assertFalse(ctx.session.isSignedIn());
    }

    @Test void registeringValidatesEveryField() throws Exception {
        AppContext ctx = fresh(clock());
        assertTrue(assertThrows(ValidationException.class, () -> ctx.auth.register("ab", "", "secret1", "secret1"))
                .getMessage().startsWith("Username must be 3 to 20"));
        assertTrue(assertThrows(ValidationException.class, () -> ctx.auth.register("bad name", "", "secret1", "secret1"))
                .getMessage().startsWith("Username must be 3 to 20"));
        assertEquals("Password must be at least 6 characters.",
                assertThrows(ValidationException.class, () -> ctx.auth.register("maria", "", "abc", "abc")).getMessage());
        assertEquals("The two passwords don't match.",
                assertThrows(ValidationException.class, () -> ctx.auth.register("maria", "", "secret1", "secret2")).getMessage());
        assertFalse(ctx.session.isSignedIn());
    }

    @Test void usernamesAreUniqueIgnoringCase() throws Exception {
        AppContext ctx = fresh(clock());
        ctx.auth.register("Maria", "Maria", "secret1", "secret1");
        ctx.auth.logout();
        assertEquals("That username is already taken.",
                assertThrows(ValidationException.class, () -> ctx.auth.register("MARIA", "", "secret1", "secret1")).getMessage());
        ctx.auth.login("maria", "secret1");   // sign-in ignores case too
        assertEquals("Maria", ctx.session.require().getDisplayName());
    }

    @Test void passwordsAreStoredHashedNotPlain() throws Exception {
        AppContext ctx = fresh(clock());
        ctx.auth.register("maria", "Maria", "secret1", "secret1");
        String stored = ctx.session.require().getPasswordHash();
        assertTrue(stored.startsWith("pbkdf2-sha256$"));
        assertFalse(stored.contains("secret1"));
        PasswordHasher h = new PasswordHasher(1000);
        assertNotEquals(h.hash("same"), h.hash("same"));   // salted
        assertTrue(h.matches("same", h.hash("same")));
        assertFalse(h.matches("other", h.hash("same")));
    }

    @Test void wrongPasswordIsRefusedWithoutSayingWhichPartWasWrong() throws Exception {
        AppContext ctx = fresh(clock());
        AuthenticationException e1 = assertThrows(AuthenticationException.class, () -> ctx.auth.login("demo", "nope"));
        AuthenticationException e2 = assertThrows(AuthenticationException.class, () -> ctx.auth.login("ghost", "nope"));
        assertEquals("Wrong username or password.", e1.getMessage());
        assertEquals(e1.getMessage(), e2.getMessage());
        assertFalse(ctx.session.isSignedIn());
    }

    @Test void repeatedWrongPasswordsLockSigningInBrieflyThenItRecovers() throws Exception {
        MutableClock clock = clock();
        AppContext ctx = fresh(clock);
        for (int i = 0; i < AuthService.MAX_FAILED_TRIES; i++) {
            assertThrows(AuthenticationException.class, () -> ctx.auth.login("demo", "bad"));
        }
        AuthenticationException locked = assertThrows(AuthenticationException.class, () -> ctx.auth.login("demo", "demo1234"));
        assertTrue(locked.getMessage().startsWith("Too many wrong attempts."));
        clock.advance(Duration.ofSeconds(31));
        ctx.auth.login("demo", "demo1234");
        assertTrue(ctx.session.isSignedIn());
    }

    @Test void everyAccountHasItsOwnGardenHistoryAndLocation() throws Exception {
        AppContext ctx = fresh(clock());
        ctx.auth.register("ana", "Ana", "secret1", "secret1");
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), ctx.crops.findByCode("okra").orElseThrow().getId(), null);
        ctx.garden.addPlot();
        ctx.settings.changeLocation("Baguio", 16.4, 120.6);
        ctx.settings.changeSoil(SoilType.SANDY);
        ctx.auth.logout();

        ctx.auth.register("ben", "Ben", "secret1", "secret1");
        assertEquals(9, ctx.garden.allPlots().size());
        assertTrue(ctx.garden.allPlots().get(0).isEmpty());
        assertEquals("Manila, Philippines", ctx.settings.current().getLocationName());
        assertNotEquals(SoilType.SANDY, ctx.settings.current().getSoil());
        ctx.settings.changeLocation("Davao City", 7.19, 125.46);
        ctx.auth.logout();

        ctx.auth.login("ana", "secret1");   // Ana's saved work is exactly as she left it
        assertEquals(10, ctx.garden.allPlots().size());
        assertEquals("Okra", ctx.garden.allPlots().get(0).getCrop().getName());
        assertEquals("Baguio", ctx.settings.current().getLocationName());
        assertEquals(SoilType.SANDY, ctx.settings.current().getSoil());
    }

    @Test void oneAccountCannotTouchAnothersBed() throws Exception {
        AppContext ctx = fresh(clock());
        ctx.auth.register("ana", "Ana", "secret1", "secret1");
        Long anaBed = ctx.garden.allPlots().get(0).getId();
        ctx.auth.logout();
        ctx.auth.register("ben", "Ben", "secret1", "secret1");
        assertThrows(com.agristep.domain.NotFoundException.class, () -> ctx.garden.getPlot(anaBed));
        assertThrows(RuntimeException.class,
                () -> ctx.garden.plant(anaBed, ctx.crops.findByCode("okra").orElseThrow().getId(), null));
    }

    @Test void gardenCannotBeUsedWhenNobodyIsSignedIn() throws Exception {
        AppContext ctx = fresh(clock());
        assertThrows(NotSignedInException.class, () -> ctx.garden.allPlots());
    }

    @Test void rememberedSignInNeedsAnExistingUser() throws Exception {
        AppContext ctx = fresh(clock());
        assertTrue(ctx.auth.signInRemembered("ghost").isEmpty());
        assertTrue(ctx.auth.signInRemembered("DEMO").isPresent());
        assertTrue(ctx.session.isSignedIn());
    }

    @Test void testHelperGivesASignedInUserWithStarterBeds() {
        AppContext ctx = TestDb.app(new ServicesTest.CountingWeather(), clock());
        assertEquals("Tester", ctx.session.require().getDisplayName());
        assertEquals(9, ctx.garden.allPlots().size());
    }
}
