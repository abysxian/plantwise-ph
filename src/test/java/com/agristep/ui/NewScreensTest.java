package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.config.LanguagePreference;
import com.agristep.domain.Crop;
import com.agristep.domain.Plot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** The calendar, journal, insights and account screens, and the new parts of Today, Garden and Crops. */
class NewScreensTest {
    private static final int[] WIDTHS = {1100, 820, 390, 340};

    private record App(AppContext ctx, MainPanel panel) {}

    private static App app(boolean populate) {
        AppContext ctx = UiFixture.context(new UiFixture.FakeWeather());
        if (populate) UiFixture.populate(ctx);
        return new App(ctx, new MainPanel(ctx));
    }

    private static void size(MainPanel p, int width) {
        p.setSize(width, 6000);
        UiFixture.layoutTree(p);
        UiFixture.layoutTree(p);
    }

    private static Page page(App a, String id) { return a.panel().page(id); }

    // ---- layout ----

    @Test void newScreensFitAtDesktopTabletAndPhoneWidths() {
        UiFixture.edt(() -> {
            App a = app(true);
            a.ctx().sharing.invite("demo");
            for (int width : WIDTHS) {
                size(a.panel(), width);
                for (String id : List.of("today", "calendar", "journal", "insights", "account", "crops")) {
                    a.panel().show(id);
                    size(a.panel(), width);
                    UiFixture.assertFits(page(a, id), id + " @" + width);
                }
            }
        });
    }

    @Test void filledNewScreensFitToo(@TempDir Path dir) throws Exception {
        Path photo = dir.resolve("p.png");
        ImageIO.write(new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB), "png", photo.toFile());
        UiFixture.edt(() -> {
            App a = app(true);
            Plot bed = a.ctx().garden.allPlots().get(0);
            a.ctx().journal.add(bed.getId(), "Leaves look healthy. ".repeat(12), photo);
            a.ctx().journal.add(null, "Short note", null);
            a.ctx().garden.harvest(a.ctx().garden.allPlots().get(4).getId());
            for (int width : WIDTHS) {
                for (String id : List.of("journal", "insights")) {
                    a.panel().show(id);
                    size(a.panel(), width);
                    UiFixture.assertFits(page(a, id), id + " @" + width);
                }
            }
            a.panel().show("crops");
            UiFixture.button(page(a, "crops"), "Pest guide").doClick();
            for (int width : WIDTHS) {
                size(a.panel(), width);
                UiFixture.assertFits(page(a, "crops"), "pest guide @" + width);
            }
        });
    }

    @Test void phoneBottomBarHasEveryDestination() {
        UiFixture.edt(() -> {
            App a = app(false);
            size(a.panel(), 340);
            assertEquals(8, a.panel().navBar().getComponentCount());
            for (java.awt.Component c : a.panel().navBar().getComponents()) assertTrue(c.getWidth() > 30, "button " + c);
            size(a.panel(), 1100);
            a.panel().navBar().select("calendar");
            assertEquals("calendar", a.panel().navBar().selected());
        });
    }

    // ---- Today ----

    @Test void todayShowsWeatherAlertsAndComingUpTasks() {
        UiFixture.edt(() -> {
            App a = app(true);
            a.panel().show("today");
            Container today = page(a, "today");
            assertTrue(UiFixture.hasText(today, "Weather alerts"));
            assertTrue(UiFixture.hasText(today, "Thunderstorm in the forecast"));
            assertTrue(UiFixture.hasText(today, "PAGASA"));
            assertTrue(UiFixture.hasText(today, "Coming up"));
            assertTrue(UiFixture.hasText(today, "Overdue by") || UiFixture.hasText(today, "Due today"));
            UiFixture.button(today, "Open garden").doClick();
            assertEquals("garden", a.panel().currentPage());
        });
    }

    @Test void todayHasNoComingUpCardForAnEmptyGarden() {
        UiFixture.edt(() -> {
            App a = app(false);
            a.panel().show("today");
            assertFalse(UiFixture.hasText(page(a, "today"), "Coming up"));
        });
    }

    // ---- Calendar ----

    @Test void calendarShowsMonthsAndRanksCropsForTheChosenMonth() {
        UiFixture.edt(() -> {
            App a = app(false);
            a.panel().show("calendar");
            Container cal = page(a, "calendar");
            assertTrue(UiFixture.hasText(cal, "Planting calendar"));
            UiFixture.button(cal, "Aug").doClick();
            assertTrue(UiFixture.hasText(cal, "August"));
            assertTrue(UiFixture.hasText(cal, "wet"), "August is wet in the fake climate");
            UiFixture.button(cal, "Show all 36 crops").doClick();
            assertEquals(36, UiFixture.all(page(a, "calendar"), Card.class).stream()
                    .filter(Card::isClickable).count());
            assertEquals(12, UiFixture.all(cal, CalendarPage.MonthCell.class).size());
            JComboBox<?> box = UiFixture.all(cal, JComboBox.class).get(0);
            box.setSelectedIndex(3);
            assertEquals(12, UiFixture.all(page(a, "calendar"), CalendarPage.MonthCell.class).size());
        });
    }

    @Test void calendarExplainsWhenThereIsNoMonthlyData() {
        UiFixture.FakeWeather weather = new UiFixture.FakeWeather();
        weather.offline = true;
        UiFixture.edt(() -> {
            AppContext ctx = UiFixture.context(weather);
            MainPanel panel = new MainPanel(ctx);
            panel.show("calendar");
            assertTrue(UiFixture.hasText(panel.page("calendar"), "not saved for this place yet"));
        });
    }

    // ---- Journal ----

    @Test void journalSavesNotesWithBedAndPhotoAndDeletesInTwoSteps(@TempDir Path dir) throws Exception {
        Path photo = dir.resolve("p.png");
        ImageIO.write(new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB), "png", photo.toFile());
        UiFixture.edt(() -> {
            App a = app(true);
            a.panel().show("journal");
            JournalPage journal = (JournalPage) page(a, "journal");
            assertTrue(UiFixture.hasText(journal, "No notes yet"));
            journal.saveEntry();                                         // empty note: refused, nothing saved
            assertEquals(0, a.ctx().journal.list().size());
            UiFixture.all(journal, JTextArea.class).get(0).setText("Aphids on the leaves");
            journal.attachPhoto(photo);
            assertTrue(UiFixture.hasText(journal, "Photo: p.png"));
            journal.saveEntry();
            assertEquals(1, a.ctx().journal.list().size());
            assertNotNull(a.ctx().journal.list().get(0).getPhotoFile());
            assertTrue(UiFixture.hasText(page(a, "journal"), "Aphids on the leaves"));
            assertTrue(UiFixture.hasText(page(a, "journal"), "Written by Tester"));
            UiFixture.button(page(a, "journal"), "Delete").doClick();
            assertEquals(1, a.ctx().journal.list().size(), "the first press only asks");
            UiFixture.button(page(a, "journal"), "Really delete?").doClick();
            assertEquals(0, a.ctx().journal.list().size());
        });
    }

    @Test void writeANoteOnTheGardenScreenOpensTheJournalForThatBed() {
        UiFixture.edt(() -> {
            App a = app(true);
            a.panel().show("garden");
            UiFixture.click(UiFixture.all(page(a, "garden"), BedCard.class).get(1));   // Bed 2: kangkong
            UiFixture.button(page(a, "garden"), "Write a note").doClick();
            assertEquals("journal", a.panel().currentPage());
            JComboBox<?> bed = UiFixture.all(page(a, "journal"), JComboBox.class).get(0);
            assertTrue(String.valueOf(((Plot) bed.getSelectedItem()).getName()).equals("Bed 2"));
            UiFixture.all(page(a, "journal"), JTextArea.class).get(0).setText("Pinched the tips");
            ((JournalPage) page(a, "journal")).saveEntry();
            assertEquals("Kangkong", a.ctx().journal.list().get(0).getCropName());
        });
    }

    // ---- Insights ----

    @Test void insightsChartsHarvestsAndExportsFiles(@TempDir Path dir) {
        UiFixture.edt(() -> {
            App a = app(true);
            a.panel().show("insights");
            Container insights = page(a, "insights");
            assertTrue(UiFixture.hasText(insights, "Nothing to chart yet."));
            a.ctx().garden.harvest(a.ctx().garden.allPlots().get(4).getId());     // sili
            a.panel().show("insights");
            insights = page(a, "insights");
            assertFalse(UiFixture.hasText(insights, "Nothing to chart yet."));
            assertEquals(1, UiFixture.all(insights, InsightsPage.BarChart.class).size());
            assertTrue(UiFixture.hasText(insights, "Sili"));
            InsightsPage p = (InsightsPage) page(a, "insights");
            Path csv = dir.resolve("h.csv");
            p.export(InsightsPage.Export.HARVESTS_CSV, csv);
            assertTrue(Files.readString(csv).contains("Sili"));
            Path pdf = dir.resolve("r.pdf");
            p.export(InsightsPage.Export.REPORT_PDF, pdf);
            assertTrue(Files.size(pdf) > 500);
            Path tasks = dir.resolve("t.csv");
            p.export(InsightsPage.Export.TASKS_CSV, tasks);
            assertTrue(Files.readString(tasks).contains("Talong"));
            // an unwritable place shows an error instead of crashing
            p.export(InsightsPage.Export.HARVESTS_CSV, dir.resolve("missing-folder").resolve("x.csv"));
            assertTrue(((FlashBar) a.panel().flashBar()).isShowingError());
        });
    }

    // ---- Garden hints ----

    @Test void gardenSuggestsGoodNeighboursBeforePlanting() {
        UiFixture.edt(() -> {
            App a = app(true);
            UiFixture.plant(a.ctx(), a.ctx().garden.allPlots().get(6), "sibuyas", a.ctx().garden.today());   // bed 7
            a.panel().show("garden");
            UiFixture.click(UiFixture.all(page(a, "garden"), BedCard.class).get(3));                         // bed 4, empty
            Container garden = page(a, "garden");
            @SuppressWarnings("unchecked") JComboBox<Crop> box = UiFixture.all(garden, JComboBox.class).get(0);
            Crop tomato = a.ctx().crops.findByCode("kamatis").orElseThrow();
            for (int i = 0; i < box.getItemCount(); i++) if (box.getItemAt(i).getId().equals(tomato.getId())) box.setSelectedIndex(i);
            assertTrue(UiFixture.hasText(garden, "Good neighbours:"), "flash=" + a.panel().flashBar().message() + " selected=" + box.getSelectedItem() + " " + UiFixture.texts(garden));
            assertTrue(UiFixture.hasText(garden, "Bed 7 (Sibuyas)"));
            size(a.panel(), 390);
            UiFixture.assertFits(page(a, "garden"), "garden hints phone");
        });
    }

    // ---- Account ----

    @Test void changingThePasswordFromTheAccountScreen() {
        UiFixture.edt(() -> {
            App a = app(false);
            a.panel().show("account");
            AccountPage account = (AccountPage) page(a, "account");
            assertTrue(UiFixture.hasText(account, "@tester"));
            List<PasswordInput> pw = UiFixture.all(account, PasswordInput.class);
            pw.get(0).setText("wrong");
            pw.get(1).setText("brandnew1");
            pw.get(2).setText("brandnew1");
            account.changePassword();
            assertTrue(a.panel().flashBar().isShowingError());
            pw = UiFixture.all(account, PasswordInput.class);
            pw.get(0).setText("secret1");
            pw.get(1).setText("brandnew1");
            pw.get(2).setText("brandnew1");
            account.changePassword();
            assertFalse(a.panel().flashBar().isShowingError());
            a.ctx().auth.logout();
            assertEquals("Tester", a.ctx().auth.login("tester", "brandnew1").getDisplayName());
        });
    }

    @Test void invitingAndJoiningFromTheAccountScreen() {
        UiFixture.edt(() -> {
            App a = app(true);
            a.panel().show("account");
            AccountPage account = (AccountPage) page(a, "account");
            UiFixture.all(account, SearchField.class).get(0).setText("demo");
            account.invite();
            assertTrue(UiFixture.hasText(page(a, "account"), "Invited, waiting for an answer"));
            assertTrue(UiFixture.hasText(page(a, "account"), "@demo"));

            a.ctx().auth.logout();
            a.ctx().auth.login("demo", "demo1234");
            MainPanel demoPanel = new MainPanel(a.ctx());
            demoPanel.show("account");
            assertTrue(UiFixture.hasText(demoPanel.page("account"), "Tester invited you to their garden"));
            UiFixture.button(demoPanel.page("account"), "Accept").doClick();
            assertTrue(UiFixture.hasText(demoPanel.page("account"), "You are working in Tester's garden"));
            demoPanel.show("garden");
            assertTrue(UiFixture.hasText(demoPanel.page("garden"), "Talong"), "sees the owner's beds");
            demoPanel.show("account");
            UiFixture.button(demoPanel.page("account"), "Leave garden").doClick();
            assertTrue(UiFixture.hasText(demoPanel.page("account"), "Invite a family member"));
        });
    }

    @Test void invitingAnUnknownPersonShowsAnError() {
        UiFixture.edt(() -> {
            App a = app(false);
            a.panel().show("account");
            UiFixture.all(page(a, "account"), SearchField.class).get(0).setText("nobody-here");
            ((AccountPage) page(a, "account")).invite();
            assertTrue(a.panel().flashBar().isShowingError());
        });
    }

    @Test void deletingTheAccountNeedsTheRightPasswordAndASecondPress() {
        UiFixture.edt(() -> {
            AppContext ctx = UiFixture.context(new UiFixture.FakeWeather());
            boolean[] signedOut = {false};
            MainPanel panel = new MainPanel(ctx, () -> signedOut[0] = true);
            panel.show("account");
            AccountPage account = (AccountPage) panel.page("account");
            UiFixture.button(account, "Delete account").doClick();                      // arms the button
            assertFalse(signedOut[0]);
            UiFixture.all(account, PasswordInput.class).get(3).setText("not-it");
            UiFixture.button(account, "Yes, delete everything").doClick();
            assertFalse(signedOut[0]);
            assertTrue(panel.flashBar().isShowingError());
            UiFixture.button(account, "Delete account").doClick();                      // disarmed again after the error
            UiFixture.all(account, PasswordInput.class).get(3).setText("secret1");
            UiFixture.button(account, "Yes, delete everything").doClick();
            assertTrue(signedOut[0]);
            assertThrows(com.agristep.service.AuthenticationException.class, () -> ctx.auth.login("tester", "secret1"));
        });
    }

    // ---- Language ----

    @Test void switchingToFilipinoTranslatesMenusTitlesAndButtons(@TempDir Path dir) {
        AccountPage.languagePreference = new LanguagePreference(dir.resolve("lang.txt"));
        AtomicReference<String> restarted = new AtomicReference<>();
        try {
            UiFixture.edt(() -> {
                AppContext ctx = UiFixture.context(new UiFixture.FakeWeather());
                MainPanel panel = new MainPanel(ctx, () -> { }, restarted::set);
                panel.show("account");
                UiFixture.button(page(panel), "Filipino");
                ((AccountPage) panel.page("account")).chooseLanguage(I18n.Language.FILIPINO);
                assertEquals("account", restarted.get());
                assertEquals("fil", AccountPage.languagePreference.read());
                MainPanel again = new MainPanel(ctx);            // what the app does when it restarts the window
                again.show("garden");
                size(again, 1100);
                assertTrue(UiFixture.hasText(again.page("garden"), "Ang iyong hardin"));
                assertTrue(UiFixture.hasText(again.page("garden"), "Baguhin ang grid") || UiFixture.buttons(again.page("garden"), "Baguhin ang grid").size() == 1);
                assertEquals("Hardin", I18n.t("Garden"));
                assertEquals("Hindi isinalin na teksto", I18n.t("Hindi isinalin na teksto"), "unknown text stays as it is");
                UiFixture.assertFits(again.page("garden"), "garden in Filipino");
                for (int width : WIDTHS) {
                    for (String id : List.of("today", "crops", "calendar", "account")) {
                        again.show(id);
                        size(again, width);
                        UiFixture.assertFits(again.page(id), id + " fil @" + width);
                    }
                }
            });
        } finally {
            I18n.use(I18n.Language.ENGLISH);
        }
        assertEquals("Garden", I18n.t("Garden"));
    }

    private static Container page(MainPanel p) { return p.page("account"); }
}
