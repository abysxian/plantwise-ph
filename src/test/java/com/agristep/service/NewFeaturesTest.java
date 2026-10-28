package com.agristep.service;

import com.agristep.AppContext;
import com.agristep.MutableClock;
import com.agristep.TestDb;
import com.agristep.domain.*;
import com.agristep.weather.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Calendar, alerts, reminders, companions, stats, export, journal, account and sharing. */
class NewFeaturesTest {
    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

    private static AppContext app() {
        return TestDb.app(new ServicesTest.CountingWeather(), new MutableClock(NOW));
    }

    private static Crop crop(AppContext ctx, String code) { return ctx.crops.findByCode(code).orElseThrow(); }

    // ---- monthly climate parsing ----

    @Test void monthlyClimateIsSavedAndReloaded() {
        AppContext ctx = app();
        ClimateStats first = ctx.weather.climate(ctx.settings.current()).stats();
        assertTrue(first.hasMonthly());
        ClimateStats again = ctx.weather.climate(ctx.settings.current()).stats();   // served from the database
        assertArrayEquals(first.monthlyTempC(), again.monthlyTempC(), 0.0001);
        assertArrayEquals(first.monthlyRainMm(), again.monthlyRainMm(), 0.0001);
    }

    // ---- calendar ----

    @Test void calendarScoresEveryMonthAndRanksCrops() {
        AppContext ctx = app();
        ClimateStats climate = ctx.weather.climate(ctx.settings.current()).stats();
        List<PlantingCalendar.MonthFit> year = ctx.calendar.yearFor(crop(ctx, "okra"), climate, SoilType.LOAM);
        assertEquals(12, year.size());
        assertEquals("January", year.get(0).monthName());
        List<CropRecommendation> june = ctx.calendar.bestFor(6, ctx.crops.findAll(), climate, SoilType.LOAM);
        assertEquals(36, june.size());
        for (int i = 1; i < june.size(); i++) assertTrue(june.get(i - 1).score() >= june.get(i).score());
        assertTrue(ctx.calendar.describeMonth(climate, 8).contains("wet"));
        assertTrue(ctx.calendar.describeMonth(climate, 1).contains("dry"));
    }

    @Test void calendarNeedsMonthlyFigures() {
        AppContext ctx = app();
        assertFalse(ctx.calendar.available(new ClimateStats(27, 2000)));
        assertThrows(IllegalStateException.class, () -> ctx.calendar.yearFor(crop(ctx, "okra"), new ClimateStats(27, 2000), SoilType.LOAM));
    }

    // ---- alerts ----

    private static WeatherForecast forecast(double rain, double gust, int code, double max) {
        return new WeatherForecast(28, 1, 70, List.of(new DailyWeather(LocalDate.of(2026, 6, 1), max, 24, rain, code, gust)));
    }

    @Test void calmWeatherGivesNoAlerts() {
        assertTrue(WeatherAlerts.from(forecast(5, 20, 1, 31)).isEmpty());
        assertTrue(WeatherAlerts.from(null).isEmpty());
    }

    @Test void severeWeatherGivesRankedAlerts() {
        List<WeatherAlerts.Alert> alerts = WeatherAlerts.from(forecast(120, 95, 95, 37));
        assertEquals(4, alerts.size());
        assertEquals(WeatherAlerts.Level.DANGER, alerts.get(0).level());
        assertTrue(alerts.stream().anyMatch(a -> a.title().startsWith("Heavy rain") && a.level() == WeatherAlerts.Level.DANGER));
        assertTrue(alerts.stream().anyMatch(a -> a.title().startsWith("Strong wind") && a.advice().contains("PAGASA")));
        assertTrue(alerts.stream().anyMatch(a -> a.title().startsWith("Thunderstorm")));
        assertTrue(alerts.stream().anyMatch(a -> a.title().startsWith("Extreme heat")));
        assertEquals(WeatherAlerts.Level.WARNING, WeatherAlerts.from(forecast(60, 0, 1, 30)).get(0).level());
    }

    // ---- reminders ----

    @Test void remindersListOverdueAndUpcomingTasksSoonestFirst() {
        AppContext ctx = app();
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), crop(ctx, "pechay").getId(), LocalDate.of(2026, 5, 25));   // started a week ago
        List<GardenService.Reminder> soon = ctx.garden.reminders(3);
        assertFalse(soon.isEmpty());
        for (int i = 1; i < soon.size(); i++) assertTrue(soon.get(i - 1).daysAway() <= soon.get(i).daysAway());
        assertTrue(soon.stream().allMatch(r -> r.daysAway() <= 3 && !r.task().isCompleted()));
        assertTrue(ctx.garden.reminders(60).size() >= soon.size());
        assertEquals(0, ctx.garden.reminders(0).stream().filter(r -> r.daysAway() > 0).count());
    }

    // ---- companions and rotation ----

    @Test void neighbouringBedsGiveCompanionAndAvoidHints() {
        AppContext ctx = app();
        List<Plot> beds = ctx.garden.allPlots();            // 3 columns: bed 1 touches beds 2, 4 and 5
        ctx.garden.plant(beds.get(1).getId(), crop(ctx, "sibuyas").getId(), null);
        ctx.garden.plant(beds.get(3).getId(), crop(ctx, "repolyo").getId(), null);
        beds = ctx.garden.allPlots();
        List<PlantingAdvisor.Hint> tomato = ctx.planting.hintsFor(crop(ctx, "kamatis"), beds.get(0), beds, 3, List.of(), LocalDate.of(2026, 6, 1));
        assertTrue(tomato.stream().anyMatch(h -> h.kind() == PlantingAdvisor.Kind.GOOD && h.text().contains("Sibuyas")));
        assertTrue(tomato.stream().anyMatch(h -> h.kind() == PlantingAdvisor.Kind.AVOID && h.text().contains("Repolyo")));
        // bed 9 is far from everything planted
        assertTrue(ctx.planting.hintsFor(crop(ctx, "kamatis"), beds.get(8), beds, 3, List.of(), LocalDate.of(2026, 6, 1)).isEmpty());
    }

    @Test void sameFamilyNeighboursDoNotBreakTheHints() {
        AppContext ctx = app();
        List<Plot> beds = ctx.garden.allPlots();
        ctx.garden.plant(beds.get(1).getId(), crop(ctx, "talong").getId(), null);
        beds = ctx.garden.allPlots();
        List<PlantingAdvisor.Hint> hints = ctx.planting.hintsFor(crop(ctx, "kamatis"), beds.get(0), beds, 3, List.of(), LocalDate.of(2026, 6, 1));
        assertTrue(hints.isEmpty());      // two nightshades are neither companions nor enemies in the rules
    }

    @Test void rotationWarnsAboutTheSameCropOrFamilyInTheSameBed() {
        AppContext ctx = app();
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), crop(ctx, "talong").getId(), LocalDate.of(2026, 2, 1));
        ctx.garden.harvest(bed.getId());
        List<HarvestRecord> history = ctx.garden.allHarvests();
        List<Plot> beds = ctx.garden.allPlots();
        LocalDate today = LocalDate.of(2026, 6, 1);
        List<PlantingAdvisor.Hint> same = ctx.planting.hintsFor(crop(ctx, "talong"), beds.get(0), beds, 3, history, today);
        assertTrue(same.stream().anyMatch(h -> h.kind() == PlantingAdvisor.Kind.ROTATE && h.text().contains("Talong")));
        List<PlantingAdvisor.Hint> family = ctx.planting.hintsFor(crop(ctx, "kamatis"), beds.get(0), beds, 3, history, today);
        assertTrue(family.stream().anyMatch(h -> h.kind() == PlantingAdvisor.Kind.ROTATE && h.text().contains("same plant family")));
        assertTrue(ctx.planting.hintsFor(crop(ctx, "pechay"), beds.get(0), beds, 3, history, today).isEmpty());
        assertTrue(ctx.planting.hintsFor(crop(ctx, "talong"), beds.get(0), beds, 3, history, today.plusDays(400)).isEmpty());
    }

    @Test void everyCatalogueCropHasAFamily() {
        AppContext ctx = app();
        for (Crop c : ctx.crops.findAll()) assertNotNull(CropRelations.familyOf(c.getCode()), c.getCode());
    }

    // ---- harvest statistics ----

    @Test void statsCountHarvestsPerMonthAndCrop() {
        List<HarvestRecord> records = List.of(
                new HarvestRecord("Okra", "x", "Bed 1", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31), 100),
                new HarvestRecord("Okra", "x", "Bed 2", LocalDate.of(2026, 4, 11), LocalDate.of(2026, 5, 31), 80),
                new HarvestRecord("Pechay", "y", "Bed 3", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), 60));
        HarvestStats.Summary s = HarvestStats.summarise(records, LocalDate.of(2026, 6, 1), 6);
        assertEquals(3, s.total());
        assertEquals("Okra", s.topCrop());
        assertEquals(2, s.perMonth().get(YearMonth.of(2026, 5)));
        assertEquals(1, s.perMonth().get(YearMonth.of(2026, 6)));
        assertEquals(6, s.perMonth().size());
        assertEquals(80, s.averageCompletion());
        assertEquals((60 + 50 + 31) / 3.0, s.averageDaysToHarvest(), 0.001);
        assertNull(HarvestStats.summarise(List.of(), LocalDate.now(), 6).topCrop());
    }

    // ---- export ----

    @Test void harvestsExportToCsvAndGuardAgainstFormulas() throws Exception {
        AppContext ctx = app();
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), crop(ctx, "okra").getId(), LocalDate.of(2026, 4, 1));
        ctx.garden.harvest(bed.getId());
        Path file = Files.createTempFile("harvests", ".csv");
        assertEquals(1, ctx.export.harvestsToCsv(file));
        String csv = Files.readString(file);
        assertTrue(csv.contains("Okra") && csv.contains("Bed 1") && csv.contains("2026-04-01"));
        assertEquals("\"'=SUM(A1)\"", ExportService.cell("=SUM(A1)"));
        assertEquals("\"say \"\"hi\"\"\"", ExportService.cell("say \"hi\""));
        ctx.garden.plant(ctx.garden.allPlots().get(1).getId(), crop(ctx, "pechay").getId(), null);
        assertTrue(ctx.export.careTasksToCsv(file) > 0);
        assertTrue(Files.readString(file).contains("Pechay"));
    }

    @Test void gardenReportIsAWellFormedPdf() throws Exception {
        AppContext ctx = app();
        ctx.garden.plant(ctx.garden.allPlots().get(0).getId(), crop(ctx, "okra").getId(), LocalDate.of(2026, 4, 1));
        ctx.garden.plant(ctx.garden.allPlots().get(1).getId(), crop(ctx, "pechay").getId(), null);
        Path file = Files.createTempFile("report", ".pdf");
        ctx.export.reportToPdf(file);
        byte[] bytes = Files.readAllBytes(file);
        String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(text.startsWith("%PDF-1.4") && text.trim().endsWith("%%EOF"));
        assertTrue(text.contains("Garden report") && text.contains("Okra"));
        int xref = Integer.parseInt(text.substring(text.lastIndexOf("startxref") + 10).trim().split("\\s")[0]);
        assertTrue(text.startsWith("xref", xref), "startxref must point at the xref table");
        // a long report spills onto more pages
        SimplePdf big = new SimplePdf();
        for (int i = 0; i < 200; i++) big.body("Line number " + i + " (with brackets) and a backslash \\ and emoji 🍅");
        big.write(file);
        String many = Files.readString(file, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(many.contains("/Count 4") || many.contains("/Count 5"), many.substring(many.indexOf("/Count"), many.indexOf("/Count") + 12));
    }

    // ---- journal and photos ----

    private static Path picture(int w, int h) throws Exception {
        Path file = Files.createTempFile("photo", ".png");
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB), "png", file.toFile());
        return file;
    }

    @Test void journalKeepsNotesAndShrinksPhotos() throws Exception {
        AppContext ctx = app();
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), crop(ctx, "okra").getId(), null);
        JournalEntry general = ctx.journal.add(null, "  Rain barrel is full  ", null);
        JournalEntry withPhoto = ctx.journal.add(bed.getId(), "Leaves are yellowing", picture(3000, 1500));
        assertEquals("Rain barrel is full", general.getNote());
        assertEquals("Okra", withPhoto.getCropName());
        assertEquals("Bed 1", withPhoto.getPlotName());
        assertEquals("Tester", withPhoto.getAuthorName());
        BufferedImage stored = ImageIO.read(ctx.journal.photoPath(withPhoto).toFile());
        assertEquals(900, stored.getWidth());
        assertEquals(450, stored.getHeight());
        assertEquals(List.of("Leaves are yellowing", "Rain barrel is full"),
                ctx.journal.list().stream().map(JournalEntry::getNote).toList());
        Path photo = ctx.journal.photoPath(withPhoto);
        ctx.journal.delete(withPhoto.getId());
        assertFalse(Files.exists(photo));
        assertEquals(1, ctx.journal.list().size());
    }

    @Test void journalRejectsBadInput() throws Exception {
        AppContext ctx = app();
        assertThrows(ValidationException.class, () -> ctx.journal.add(null, "   ", null));
        assertThrows(ValidationException.class, () -> ctx.journal.add(null, "x".repeat(2001), null));
        Path notAPicture = Files.createTempFile("note", ".txt");
        Files.writeString(notAPicture, "hello");
        assertThrows(ValidationException.class, () -> ctx.journal.add(null, "note", notAPicture));
        assertThrows(NotFoundException.class, () -> ctx.journal.add(9999L, "note", null));
        assertEquals(0, ctx.journal.list().size());
    }

    @Test void journalsAreSeparatePerAccount() {
        AppContext ctx = app();
        ctx.journal.add(null, "Mine", null);
        ctx.auth.logout();
        ctx.auth.register("other", "Other", "secret1", "secret1");
        assertTrue(ctx.journal.list().isEmpty());
    }

    // ---- account ----

    @Test void passwordChangeNeedsTheCurrentPasswordAndWorksAtNextLogin() {
        AppContext ctx = app();
        assertThrows(ValidationException.class, () -> ctx.account.changePassword("wrong", "newpass1", "newpass1"));
        assertThrows(ValidationException.class, () -> ctx.account.changePassword("secret1", "short", "short"));
        assertThrows(ValidationException.class, () -> ctx.account.changePassword("secret1", "newpass1", "different"));
        assertThrows(ValidationException.class, () -> ctx.account.changePassword("secret1", "secret1", "secret1"));
        ctx.account.changePassword("secret1", "newpass1", "newpass1");
        ctx.auth.logout();
        assertThrows(AuthenticationException.class, () -> ctx.auth.login("tester", "secret1"));
        assertEquals("Tester", ctx.auth.login("tester", "newpass1").getDisplayName());
        ctx.account.changePassword("newpass1", "third123", "third123");     // works with the changed hash too
    }

    @Test void deletingAnAccountRemovesItsWholeGarden() throws Exception {
        AppContext ctx = app();
        Plot bed = ctx.garden.allPlots().get(0);
        ctx.garden.plant(bed.getId(), crop(ctx, "okra").getId(), LocalDate.of(2026, 4, 1));
        ctx.garden.harvest(bed.getId());
        JournalEntry entry = ctx.journal.add(null, "note", picture(100, 100));
        Path photo = ctx.journal.photoPath(entry);
        assertThrows(ValidationException.class, () -> ctx.account.deleteAccount("wrong"));
        assertEquals(9, ctx.garden.allPlots().size());           // nothing happened
        ctx.account.deleteAccount("secret1");
        assertFalse(Files.exists(photo));
        ctx.auth.logout();
        assertThrows(AuthenticationException.class, () -> ctx.auth.login("tester", "secret1"));
        // the demo account and its garden are untouched, and the name can be used again
        assertEquals("Demo farmer", ctx.auth.login("demo", "demo1234").getDisplayName());
        assertEquals(9, ctx.garden.allPlots().size());
        ctx.auth.logout();
        assertEquals("tester", ctx.auth.register("tester", "Again", "secret1", "secret1").getUsername());
        assertTrue(ctx.garden.allHarvests().isEmpty());
    }

    // ---- shared gardens ----

    private static AppContext twoAccounts() {
        AppContext ctx = app();                       // "tester" is signed in
        ctx.auth.logout();
        ctx.auth.register("friend", "Friend", "secret1", "secret1");
        ctx.auth.logout();
        ctx.auth.login("tester", "secret1");
        return ctx;
    }

    @Test void invitedPersonWorksInTheOwnersGardenUntilTheyLeave() {
        AppContext ctx = twoAccounts();
        ctx.garden.plant(ctx.garden.allPlots().get(0).getId(), crop(ctx, "okra").getId(), null);
        ctx.journal.add(null, "Owner note", null);
        ctx.sharing.invite("FRIEND");
        assertEquals(1, ctx.sharing.sentInvites().size());
        assertThrows(ValidationException.class, () -> ctx.sharing.invite("friend"));
        ctx.auth.logout();

        ctx.auth.login("friend", "secret1");
        assertEquals("Tester", ctx.sharing.invitations().get(0).getDisplayName());
        assertTrue(ctx.garden.allPlots().get(0).isEmpty());          // still their own garden
        ctx.sharing.accept(ctx.sharing.invitations().get(0).getId());
        assertEquals("Okra", ctx.garden.allPlots().get(0).getCrop().getName());     // now the owner's garden
        assertEquals("Owner note", ctx.journal.list().get(0).getNote());
        assertEquals("Tester", ctx.sharing.joinedOwner().orElseThrow().getDisplayName());
        ctx.journal.add(null, "Friend note", null);
        assertEquals("Friend", ctx.journal.list().get(0).getAuthorName());

        ctx.auth.logout();                                           // membership survives signing in again
        ctx.auth.login("friend", "secret1");
        assertEquals("Okra", ctx.garden.allPlots().get(0).getCrop().getName());

        ctx.sharing.leave();
        assertTrue(ctx.garden.allPlots().get(0).isEmpty());
        assertTrue(ctx.journal.list().isEmpty());
        ctx.auth.logout();
        ctx.auth.login("tester", "secret1");
        assertEquals(2, ctx.journal.list().size());                  // the friend's note stays in the shared garden
        assertTrue(ctx.sharing.members().isEmpty());
    }

    @Test void ownerCanRemoveMembersAndCancelInvites() {
        AppContext ctx = twoAccounts();
        ctx.sharing.invite("friend");
        ctx.sharing.cancelInvite(ctx.sharing.sentInvites().get(0).getId());
        assertTrue(ctx.sharing.sentInvites().isEmpty());
        ctx.sharing.invite("friend");
        ctx.auth.logout();
        ctx.auth.login("friend", "secret1");
        ctx.sharing.accept(ctx.sharing.invitations().get(0).getId());
        ctx.auth.logout();
        ctx.auth.login("tester", "secret1");
        assertEquals(1, ctx.sharing.members().size());
        ctx.sharing.remove(ctx.sharing.members().get(0).getId());
        assertTrue(ctx.sharing.members().isEmpty());
        ctx.auth.logout();
        ctx.auth.login("friend", "secret1");
        assertTrue(ctx.sharing.joinedOwner().isEmpty());
    }

    @Test void sharingRulesAreEnforced() {
        AppContext ctx = twoAccounts();
        assertThrows(ValidationException.class, () -> ctx.sharing.invite("tester"));
        assertThrows(NotFoundException.class, () -> ctx.sharing.invite("nobody"));
        assertThrows(ValidationException.class, () -> ctx.sharing.invite(" "));
        assertThrows(NotFoundException.class, () -> ctx.sharing.accept(12345));
        ctx.sharing.invite("friend");
        ctx.auth.logout();
        ctx.auth.login("friend", "secret1");
        long owner = ctx.sharing.invitations().get(0).getId();
        ctx.sharing.accept(owner);
        assertThrows(ValidationException.class, () -> ctx.sharing.invite("demo"));      // a member cannot host
        assertThrows(NotFoundException.class, () -> ctx.sharing.remove(owner));          // and is not an owner
        ctx.auth.logout();
        ctx.auth.login("demo", "demo1234");
        ctx.sharing.invite("tester");
        ctx.auth.logout();
        ctx.auth.login("tester", "secret1");
        assertEquals(1, ctx.sharing.members().size());
        long demo = ctx.sharing.invitations().get(0).getId();
        assertThrows(ValidationException.class, () -> ctx.sharing.accept(demo));         // has a member of their own
        ctx.sharing.decline(demo);
        assertTrue(ctx.sharing.invitations().isEmpty());
    }

    @Test void deletingAnOwnerReturnsMembersToTheirOwnGarden() {
        AppContext ctx = twoAccounts();
        ctx.sharing.invite("friend");
        ctx.auth.logout();
        ctx.auth.login("friend", "secret1");
        ctx.sharing.accept(ctx.sharing.invitations().get(0).getId());
        ctx.auth.logout();
        ctx.auth.login("tester", "secret1");
        ctx.account.deleteAccount("secret1");
        ctx.auth.logout();
        ctx.auth.login("friend", "secret1");
        assertTrue(ctx.sharing.joinedOwner().isEmpty());
        assertEquals(9, ctx.garden.allPlots().size());
    }
}
