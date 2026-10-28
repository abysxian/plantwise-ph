package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.Crop;
import com.agristep.domain.Plot;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Goes through every screen in Filipino and fails when readable English is left over. */
class FilipinoCoverageTest {
    /** Common English words that Filipino text does not use. A text containing one is probably untranslated. */
    private static final Pattern ENGLISH = Pattern.compile(
            "\\b(the|your|you|is|are|to|and|for|with|of|this|that|will|from|when|yet|has|have|keep|check|pick|plant|leaves|days|soil|garden|harvest|overdue|water|bed|beds|not|then|sandy|clay|week|weeks|tomorrow|mon|tue|wed|thu|fri|sat|sun|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Words that are the same in both languages or are names, so they alone do not mean "English". */
    private static final Set<String> OK = Set.of("PlantWise PH", "OK", "CSV", "PDF", "Account", "Email");

    private final Set<String> offenders = new LinkedHashSet<>();
    private final List<String> englishNames = new ArrayList<>();

    private void scan(Container root, String screen) {
        List<String> texts = new ArrayList<>(UiFixture.texts(root));
        UiFixture.all(root, PillButton.class).forEach(b -> texts.add(b.getText()));
        UiFixture.all(root, Badge.class).forEach(b -> texts.add(b.getText()));
        UiFixture.all(root, BedCard.class).forEach(b -> { texts.add(b.getStatusText()); texts.add(b.getAccessibleContext().getAccessibleName()); });
        UiFixture.all(root, SearchField.class).forEach(f -> texts.add(f.hint()));
        UiFixture.all(root, PasswordInput.class).forEach(f -> texts.add(f.hint()));
        UiFixture.all(root, javax.swing.JCheckBox.class).forEach(c -> texts.add(c.getText()));
        for (String text : texts) {
            if (text == null || text.isBlank() || OK.contains(text.trim())) continue;
            for (String line : text.split("\n")) {
                String shown = line;
                for (String name : englishNames) shown = shown.replace(name, "");     // English crop names stay as printed
                if (ENGLISH.matcher(shown).find()) offenders.add(screen + ": " + line);
            }
        }
    }

    @Test void everyScreenIsFullyInFilipino() {
        try {
            UiFixture.edt(() -> {
                AppContext ctx = UiFixture.context(new UiFixture.FakeWeather());
                UiFixture.populate(ctx);
                Plot bed = ctx.garden.allPlots().get(0);
                ctx.journal.add(bed.getId(), "Mukhang malusog ang mga dahon.", null);
                ctx.garden.harvest(ctx.garden.allPlots().get(4).getId());
                ctx.sharing.invite("demo");
                ctx.crops.findAll().forEach(c -> englishNames.add(c.getEnglishName()));
                I18n.use(I18n.Language.FILIPINO);
                MainPanel panel = new MainPanel(ctx);
                for (String id : List.of("today", "garden", "crops", "calendar", "journal", "insights", "location", "account")) {
                    panel.show(id);
                    panel.setSize(1100, 2400);
                    UiFixture.layoutTree(panel);
                    scan((Container) panel.page(id), id);
                }
                // every bed's side panel, so every kind of care task is read
                for (Plot plot : ctx.garden.allPlots()) {
                    panel.show("garden");
                    GardenPage garden = (GardenPage) panel.page("garden");
                    garden.select(plot.getId());
                    panel.setSize(1100, 2400);
                    UiFixture.layoutTree(panel);
                    scan(garden, "garden/" + plot.getName());
                }
                // the care guide and pest guide of every crop
                for (Crop crop : ctx.crops.findAll()) {
                    panel.showCrop(crop);
                    panel.setSize(1100, 4000);
                    UiFixture.layoutTree(panel);
                    scan((Container) panel.page("crop"), "crop/" + crop.getCode());
                }
                // editing the grid, and what a message bar says
                panel.show("garden");
                GardenPage garden = (GardenPage) panel.page("garden");
                UiFixture.button(garden, "Baguhin ang grid").doClick();
                panel.setSize(1100, 2400);
                UiFixture.layoutTree(panel);
                scan(garden, "grid editor");
                panel.flash("Planted Talong in Bed 2.", false);
                panel.flash("Wrong username or password.", true);
                scan(panel.flashBar(), "message bar");
                // the account screen of the person who was invited
                ctx.auth.logout();
                ctx.auth.login("demo", "demo1234");
                MainPanel demo = new MainPanel(ctx);
                demo.show("account");
                demo.setSize(1100, 2400);
                UiFixture.layoutTree(demo);
                scan((Container) demo.page("account"), "account invited");
                UiFixture.button((Container) demo.page("account"), "Burahin ang account").doClick();
                UiFixture.layoutTree(demo);
                scan((Container) demo.page("account"), "account delete step");
                LoginPage login = new LoginPage(ctx, "", (user, remember, created) -> { });
                login.setSize(900, 1400);
                UiFixture.layoutTree(login);
                scan(login, "login");
                UiFixture.button(login, "Mag-sign in").doClick();                   // empty form shows an error
                UiFixture.layoutTree(login);
                scan(login, "login error");
                UiFixture.button(login, "Gumawa ng account").doClick();
                login.setSize(900, 1400);
                UiFixture.layoutTree(login);
                scan(login, "register");
                panel.show("crops");
                UiFixture.button((Container) panel.page("crops"), "Gabay sa peste").doClick();
                scan((Container) panel.page("crops"), "pest guide");
            });
        } finally {
            I18n.use(I18n.Language.ENGLISH);
        }
        assertTrue(offenders.isEmpty(), "left in English:\n" + String.join("\n", offenders));
    }
}
