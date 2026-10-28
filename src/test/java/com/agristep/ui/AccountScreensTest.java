package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.config.RememberedLogin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountScreensTest {
    private static final class StubShell implements Shell {
        Component body;
        String title = "";
        @Override public void showBody(Component body) { this.body = body; }
        @Override public void setWindowTitle(String title) { this.title = title; }
    }

    private static void size(Component c, int w) {
        c.setSize(w, 4000);
        UiFixture.layoutTree(c);
        UiFixture.layoutTree(c);
    }

    private static PillButton submit(Component body) {
        List<PillButton> all = UiFixture.all((java.awt.Container) body, PillButton.class);
        return all.stream().filter(b -> b.getText().equals("Sign in") || b.getText().equals("Create account"))
                .reduce((a, b) -> b).orElseThrow();
    }

    private static void fill(Component body, String user, String name, String pw, String again) {
        List<SearchField> fields = UiFixture.all((java.awt.Container) body, SearchField.class);
        fields.get(0).setText(user);
        if (name != null) fields.get(1).setText(name);
        List<PasswordInput> pws = UiFixture.all((java.awt.Container) body, PasswordInput.class);
        pws.get(0).setText(pw);
        if (again != null) pws.get(1).setText(again);
    }

    private static AppContext ctx() { return UiFixture.signedOutContext(); }

    @Test void signingInWithTheDemoAccountOpensTheAppAndRemembersIt(@TempDir Path dir) {
        UiFixture.edt(() -> {
            AppContext ctx = ctx();
            StubShell shell = new StubShell();
            RememberedLogin remembered = new RememberedLogin(dir.resolve("u.txt"));
            new AppController(shell, ctx, remembered, "note").start(false);
            assertTrue(UiFixture.hasText((java.awt.Container) shell.body, "demo1234"), "demo login is shown");
            size(shell.body, 400);
            UiFixture.assertFits((java.awt.Container) shell.body, "login phone");
            UiFixture.button((java.awt.Container) shell.body, "Fill in the demo account").doClick();
            UiFixture.all((java.awt.Container) shell.body, JCheckBox.class).stream()
                    .filter(b -> b.getText().startsWith("Keep me")).findFirst().orElseThrow().setSelected(true);
            submit(shell.body).doClick();
            assertTrue(shell.body instanceof MainPanel, "app is showing");
            assertTrue(shell.title.startsWith("PlantWise PH - "), shell.title);
            assertEquals("demo", remembered.read().orElseThrow());
        });
    }

    @Test void wrongPasswordShowsAnErrorAndStaysOnSignIn(@TempDir Path dir) {
        UiFixture.edt(() -> {
            AppContext ctx = ctx();
            StubShell shell = new StubShell();
            new AppController(shell, ctx, new RememberedLogin(dir.resolve("u.txt")), "").start(false);
            fill(shell.body, "demo", null, "wrongpass", null);
            submit(shell.body).doClick();
            assertFalse(shell.body instanceof MainPanel);
            assertTrue(UiFixture.hasText((java.awt.Container) shell.body, "Wrong username or password."));
            assertTrue(dir.resolve("u.txt").toFile().length() == 0 || !dir.resolve("u.txt").toFile().exists());
        });
    }

    @Test void creatingAnAccountLandsOnTheLocationPageWithAWelcome(@TempDir Path dir) {
        UiFixture.edt(() -> {
            AppContext ctx = ctx();
            StubShell shell = new StubShell();
            new AppController(shell, ctx, new RememberedLogin(dir.resolve("u.txt")), "").start(false);
            UiFixture.buttons((java.awt.Container) shell.body, "Create account").get(0).doClick();
            fill(shell.body, "maria", "Maria", "secret1", "secret2");
            submit(shell.body).doClick();
            assertTrue(UiFixture.hasText((java.awt.Container) shell.body, "The two passwords don't match."));
            fill(shell.body, "maria", "Maria", "secret1", "secret1");
            submit(shell.body).doClick();
            MainPanel main = (MainPanel) shell.body;
            assertEquals("location", main.currentPage());
            assertTrue(main.flashBar().message().startsWith("Welcome, Maria!"));
            assertEquals("Maria", ctx.session.require().getDisplayName());
        });
    }

    @Test void rememberedUserOpensStraightIntoTheirSavedGardenAndSignOutForgetsThem(@TempDir Path dir) {
        UiFixture.edt(() -> {
            AppContext ctx = ctx();
            ctx.auth.register("maria", "Maria", "secret1", "secret1");
            ctx.garden.addPlot();
            ctx.auth.logout();
            RememberedLogin remembered = new RememberedLogin(dir.resolve("u.txt"));
            remembered.write("maria");
            boolean signedIn = remembered.read().flatMap(ctx.auth::signInRemembered).isPresent();
            StubShell shell = new StubShell();
            AppController controller = new AppController(shell, ctx, remembered, "");
            controller.start(signedIn);
            assertTrue(shell.body instanceof MainPanel);
            assertEquals(10, ctx.garden.allPlots().size());   // the bed added earlier is still there
            controller.signOut();
            assertFalse(shell.body instanceof MainPanel);
            assertFalse(ctx.session.isSignedIn());
            assertTrue(remembered.read().isEmpty());
        });
    }

    @Test void locationFormIsPrefilledSavesTypedPlaceAndRejectsBadInput() {
        UiFixture.edt(() -> {
            AppContext ctx = UiFixture.context(new UiFixture.FakeWeather());
            MainPanel panel = new MainPanel(ctx);
            panel.show("location");
            size(panel, 1000);
            Page page = panel.page("location");
            List<SearchField> f = UiFixture.all(page, SearchField.class);
            assertEquals("Manila, Philippines", f.get(0).getText());
            assertEquals("14.5995", f.get(1).getText());

            f.get(1).setText("95");
            UiFixture.button(page, "Save location").doClick();
            assertTrue(UiFixture.hasText(page, "Latitude must be a number between -90 and 90."));
            assertEquals("Manila, Philippines", ctx.settings.current().getLocationName());

            f.get(0).setText("  ");
            f.get(1).setText("10,5");
            UiFixture.button(page, "Save location").doClick();
            assertTrue(UiFixture.hasText(page, "Type the name of your place."));

            f.get(0).setText("Brgy. San Isidro, Lipa");
            f.get(1).setText("13,9411");   // comma decimal accepted
            f.get(2).setText("121.1631");
            UiFixture.button(page, "Save location").doClick();
            assertEquals("Brgy. San Isidro, Lipa", ctx.settings.current().getLocationName());
            assertEquals(13.9411, ctx.settings.current().getLatitude(), 1e-9);
            assertEquals("today", panel.currentPage());
        });
    }

    @Test void commonPlacesAreFilterable() {
        UiFixture.edt(() -> {
            AppContext ctx = UiFixture.context(new UiFixture.FakeWeather());
            MainPanel panel = new MainPanel(ctx);
            panel.show("location");
            size(panel, 1000);
            Page page = panel.page("location");
            assertTrue(LocationPage.COMMON_PLACES.size() >= 25);
            UiFixture.all(page, SearchField.class).get(4).setText("cebu");
            size(panel, 1000);
            assertNotNull(UiFixture.button(page, "Cebu City"));
            assertTrue(UiFixture.buttons(page, "Davao City").isEmpty());
            UiFixture.button(page, "Cebu City").doClick();
            assertEquals("Cebu City", ctx.settings.current().getLocationName());
        });
    }

    @Test void coordinateParsingRejectsNonsense() {
        assertEquals(-12.5, LocationPage.parseCoordinate(" -12,5 ", "Latitude", 90), 1e-9);
        for (String bad : new String[] {"", "abc", "NaN", "Infinity", "181"}) {
            assertThrows(IllegalArgumentException.class, () -> LocationPage.parseCoordinate(bad, "Longitude", 180), bad);
        }
    }
}
