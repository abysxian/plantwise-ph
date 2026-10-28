package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.*;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Drives the real screens (real services, in-memory database, fake weather) without opening a window. */
class ScreensTest {
    private static final int[] WIDTHS = {1100, 820, 390, 340};

    private record App(AppContext ctx, UiFixture.FakeWeather weather, MainPanel panel) {}

    private static App app(boolean populate, boolean offline) {
        UiFixture.FakeWeather weather = new UiFixture.FakeWeather();
        weather.offline = offline;
        AppContext ctx = UiFixture.context(weather);
        if (populate) UiFixture.populate(ctx);
        return new App(ctx, weather, new MainPanel(ctx));
    }

    private static void size(MainPanel p, int width) {
        p.setSize(width, 5000);
        UiFixture.layoutTree(p);
        UiFixture.layoutTree(p);
    }

    private static Page page(App a, String id) { return a.panel().page(id); }

    // ---- fit at every width ----

    @Test void everyScreenFitsAtDesktopTabletAndPhoneWidths() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            for (int width : WIDTHS) {
                size(a.panel(), width);
                for (String id : List.of("today", "garden", "crops", "location")) {
                    a.panel().show(id);
                    size(a.panel(), width);
                    UiFixture.assertFits(page(a, id), id + " @" + width);
                    assertTrue(page(a, id).getPreferredSize().height > 200, id + " @" + width);
                }
                for (String code : List.of("talong", "sibuyas", "kamote")) {
                    a.panel().showCrop(a.ctx().crops.findByCode(code).orElseThrow());
                    size(a.panel(), width);
                    UiFixture.assertFits(page(a, "crop"), code + " @" + width);
                }
            }
        });
    }

    @Test void navigationSwitchesBetweenSideRailAndBottomBar() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            size(a.panel(), 1100);
            assertTrue(a.panel().isWideMode());
            assertTrue(a.panel().navBar().isWide());
            assertEquals(BorderLayout.WEST, ((BorderLayout) a.panel().getLayout()).getConstraints(a.panel().navBar()));

            size(a.panel(), 390);
            assertFalse(a.panel().isWideMode());
            assertFalse(a.panel().navBar().isWide());
            assertEquals(BorderLayout.SOUTH, ((BorderLayout) a.panel().getLayout()).getConstraints(a.panel().navBar()));

            size(a.panel(), 1100);
            assertTrue(a.panel().isWideMode());
        });
    }

    @Test void clickingNavigationButtonsChangesTheScreen() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            size(a.panel(), 1100);
            assertEquals("today", a.panel().currentPage());
            for (String[] item : new String[][] {{"Garden", "garden"}, {"Crops", "crops"}, {"Location", "location"}, {"Today", "today"}}) {
                JComponent button = UiFixture.all(a.panel().navBar(), JComponent.class).stream()
                        .filter(c -> item[0].equals(c.getAccessibleContext() == null ? null : c.getAccessibleContext().getAccessibleName()))
                        .findFirst().orElseThrow(() -> new AssertionError("no nav button " + item[0]));
                UiFixture.click(button);
                assertEquals(item[1], a.panel().currentPage());
                assertEquals(item[1], a.panel().navBar().selected());
            }
        });
    }

    // ---- Today ----

    @Test void todayShowsAdviceWeatherSoilAndTenRankedCrops() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            size(a.panel(), 1100);
            Page today = page(a, "today");
            assertTrue(UiFixture.hasText(today, "Fair weather for garden chores"));
            assertTrue(UiFixture.hasText(today, "tasks are due"));
            assertTrue(UiFixture.hasText(today, "1 bed is ready to harvest"));
            assertTrue(UiFixture.hasText(today, "29 °C"));
            assertTrue(UiFixture.hasText(today, "Partly cloudy"));
            assertTrue(UiFixture.hasText(today, "Soft garden soil"));
            assertTrue(UiFixture.hasText(today, "Sandy soil"));
            assertTrue(UiFixture.hasText(today, "Sticky clay"));
            assertEquals(10, UiFixture.buttons(today, "Plant").size());
            assertTrue(UiFixture.hasText(today, "1. "));

            UiFixture.button(today, "Show all 36 crops").doClick();
            assertEquals(36, UiFixture.buttons(today, "Plant").size());
            UiFixture.button(today, "Show fewer").doClick();
            assertEquals(10, UiFixture.buttons(today, "Plant").size());
        });
    }

    @Test void todayWorksOfflineAndSaysSo() {
        UiFixture.edt(() -> {
            App a = app(false, true);
            size(a.panel(), 390);
            Page today = page(a, "today");
            assertTrue(UiFixture.hasText(today, "Weather is not available right now."));
            assertTrue(UiFixture.hasText(today, "can't be reached"));
            assertTrue(UiFixture.hasText(today, "ranked by soil only"));
            assertEquals(10, UiFixture.buttons(today, "Plant").size());
            UiFixture.assertFits(today, "offline today");
        });
    }

    @Test void pickingADifferentSoilSavesItAndReranks() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            size(a.panel(), 1100);
            Page today = page(a, "today");
            List<String> before = UiFixture.texts(today).stream().filter(t -> t.matches("\\d+\\. .*")).toList();
            UiFixture.click(UiFixture.cardWith(today, "Sandy soil"));
            assertEquals(SoilType.SANDY, a.ctx().settings.current().getSoil());
            assertTrue(a.panel().flashBar().message().toLowerCase().contains("sandy"));
            size(a.panel(), 1100);
            List<String> after = UiFixture.texts(today).stream().filter(t -> t.matches("\\d+\\. .*")).toList();
            assertNotEquals(before, after, "ranking should change with the soil");
        });
    }

    @Test void plantingFromTodayUsesTheOnlyEmptyBedAndOpensTheGarden() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            List<Plot> plots = a.ctx().garden.allPlots();
            Long okra = a.ctx().crops.findByCode("okra").orElseThrow().getId();
            for (int i = 0; i < plots.size() - 1; i++) a.ctx().garden.plant(plots.get(i).getId(), okra, null);
            size(a.panel(), 1100);
            a.panel().show("today");
            size(a.panel(), 1100);
            UiFixture.buttons(page(a, "today"), "Plant").get(0).doClick();
            Plot last = a.ctx().garden.getPlot(plots.get(plots.size() - 1).getId());
            assertFalse(last.isEmpty());
            assertTrue(a.panel().flashBar().message().startsWith("Planted "));
            assertEquals("garden", a.panel().currentPage());
        });
    }

    @Test void plantingWithNoEmptyBedExplainsAndGoesToTheGarden() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            Long okra = a.ctx().crops.findByCode("okra").orElseThrow().getId();
            for (Plot p : a.ctx().garden.allPlots()) a.ctx().garden.plant(p.getId(), okra, null);
            size(a.panel(), 1100);
            UiFixture.buttons(page(a, "today"), "Plant").get(0).doClick();
            assertTrue(a.panel().flashBar().message().contains("All your beds are full"));
            assertEquals("garden", a.panel().currentPage());
        });
    }

    // ---- Garden ----

    @Test void gardenShowsNineBedsInThreeColumnsAtEveryWidthAndTheRightStates() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            for (int width : WIDTHS) {
                size(a.panel(), width);
                a.panel().show("garden");
                size(a.panel(), width);
                List<BedCard> beds = UiFixture.all(page(a, "garden"), BedCard.class);
                assertEquals(9, beds.size());
                Set<Integer> columns = new HashSet<>();
                beds.forEach(b -> columns.add(b.getX()));
                assertEquals(3, columns.size(), "3 columns at " + width);
                beds.forEach(b -> assertEquals(b.getWidth(), b.getHeight(), 2, "square tiles at " + width));
            }
            size(a.panel(), 1100);
            List<BedCard> beds = UiFixture.all(page(a, "garden"), BedCard.class);
            assertEquals("Day 20 of 75", beds.get(0).getStatusText());
            assertEquals("Ready!", beds.get(1).getStatusText());
            assertEquals("Day 0 of 28", beds.get(2).getStatusText());
            assertEquals("Tap to plant", beds.get(3).getStatusText());
            assertTrue(beds.get(0).isSelected(), "first planted bed is selected by default");
        });
    }

    @Test void sidePanelSitsBesideOnDesktopAndBelowOnPhone() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            a.panel().show("garden");
            size(a.panel(), 1100);
            Component bed = UiFixture.all(page(a, "garden"), BedCard.class).get(0);
            Component side = UiFixture.button(page(a, "garden"), "Care guide").getParent().getParent();
            int bedY = SwingUtilitiesAbs.y(bed);
            int bedRight = SwingUtilitiesAbs.x(bed) + bed.getWidth();
            assertTrue(SwingUtilitiesAbs.x(side) >= bedRight, "side panel is to the right on desktop");

            size(a.panel(), 390);
            Component lastBed = UiFixture.all(page(a, "garden"), BedCard.class).get(8);
            Component side2 = UiFixture.button(page(a, "garden"), "Care guide").getParent().getParent();
            assertTrue(SwingUtilitiesAbs.y(side2) > SwingUtilitiesAbs.y(lastBed), "side panel is below the grid on phone");
            assertTrue(bedY >= 0);
        });
    }

    @Test void plantingInAnEmptyBedFromTheSidePanel() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            a.panel().show("garden");
            size(a.panel(), 1100);
            Page garden = page(a, "garden");
            UiFixture.click(UiFixture.all(garden, BedCard.class).get(3));
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(garden, "Bed 4 is empty"));
            UiFixture.button(garden, "Plant here").doClick();
            Plot bed4 = a.ctx().garden.allPlots().get(3);
            assertFalse(bed4.isEmpty());
            assertEquals("Alugbati", bed4.getCrop().getName());
            assertTrue(a.panel().flashBar().message().startsWith("Planted Alugbati in Bed 4"));
            size(a.panel(), 1100);
            assertEquals("Day 0 of 40", UiFixture.all(garden, BedCard.class).get(3).getStatusText());
        });
    }

    @Test void tickingATaskSavesItAndUpdatesProgress() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            a.panel().show("garden");
            size(a.panel(), 1100);
            Page garden = page(a, "garden");
            Plot before = a.ctx().garden.allPlots().get(0);
            long doneBefore = before.getTasks().stream().filter(CareTask::isCompleted).count();
            CheckToggle open = UiFixture.all(garden, CheckToggle.class).stream().filter(t -> !t.isChecked()).findFirst().orElseThrow();
            UiFixture.click(open);
            Plot after = a.ctx().garden.allPlots().get(0);
            assertEquals(doneBefore + 1, after.getTasks().stream().filter(CareTask::isCompleted).count());
            assertTrue(after.progressPercent() > before.progressPercent());
        });
    }

    @Test void harvestingAReadyBedLogsHistoryAndEmptiesIt() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            a.panel().show("garden");
            size(a.panel(), 1100);
            Page garden = page(a, "garden");
            assertTrue(UiFixture.hasText(garden, "Nothing harvested yet"));
            UiFixture.click(UiFixture.all(garden, BedCard.class).get(1));
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(garden, "Kangkong · Bed 2"));
            UiFixture.button(garden, "Harvest").doClick();   // ready, so no confirmation question
            assertTrue(a.ctx().garden.getPlot(a.ctx().garden.allPlots().get(1).getId()).isEmpty());
            assertEquals(1, a.ctx().garden.recentHarvests().size());
            assertTrue(a.panel().flashBar().message().startsWith("Harvested Kangkong"));
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(garden, "Kangkong · Bed 2"), "shows in harvest history");
            assertFalse(UiFixture.hasText(garden, "Nothing harvested yet"));
        });
    }

    @Test void addingABedAndOpeningTheCareGuide() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            a.panel().show("garden");
            size(a.panel(), 1100);
            Page garden = page(a, "garden");
            UiFixture.button(garden, "Add a bed").doClick();
            size(a.panel(), 1100);
            assertEquals(10, UiFixture.all(garden, BedCard.class).size());
            assertTrue(UiFixture.hasText(garden, "Bed 10 is empty"));

            UiFixture.click(UiFixture.all(garden, BedCard.class).get(0));
            size(a.panel(), 1100);
            UiFixture.button(garden, "Care guide").doClick();
            assertEquals("crop", a.panel().currentPage());
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(page(a, "crop"), "Talong"));
        });
    }

    @Test void overdueTasksAreMarkedAndRainHintShownForWatering() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            a.panel().show("garden");
            size(a.panel(), 1100);
            Page garden = page(a, "garden");
            assertTrue(UiFixture.hasText(garden, "Overdue by"));
            assertTrue(UiFixture.hasText(garden, "skip watering"));
            assertTrue(UiFixture.hasText(garden, "Due in"));
        });
    }

    // ---- Crops ----

    @Test void cropListShowsAllAndSearchFilters() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            a.panel().show("crops");
            size(a.panel(), 1100);
            Page crops = page(a, "crops");
            assertEquals(36, UiFixture.all(crops, Badge.class).size());
            assertEquals(1, UiFixture.all(crops, Badge.class).stream().filter(b -> b.getText().equals("FAO verified")).count());
            assertTrue(UiFixture.hasText(crops, "36 vegetables"));

            SearchField search = UiFixture.all(crops, SearchField.class).get(0);
            search.setText("eggplant");
            size(a.panel(), 1100);
            assertEquals(1, UiFixture.all(crops, Badge.class).size());
            assertTrue(UiFixture.hasText(crops, "1 of 36 match"));

            search.setText("zzzz");
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(crops, "No crop matches"));

            search.setText("");
            size(a.panel(), 1100);
            assertEquals(36, UiFixture.all(crops, Badge.class).size());
        });
    }

    @Test void searchMatchesEnglishAndScientificNamesToo() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            a.panel().show("crops");
            size(a.panel(), 1100);
            Page crops = page(a, "crops");
            SearchField search = UiFixture.all(crops, SearchField.class).get(0);
            search.setText("allium");
            size(a.panel(), 1100);
            assertEquals(2, UiFixture.all(crops, Badge.class).size(), "onion and garlic");
            search.setText("TALONG");
            size(a.panel(), 1100);
            assertEquals(1, UiFixture.all(crops, Badge.class).size());
        });
    }

    @Test void openingACropShowsItsDetailAndHonestSourceLabel() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            a.panel().show("crops");
            size(a.panel(), 1100);
            UiFixture.click(UiFixture.cardWith(page(a, "crops"), "Talong"));
            assertEquals("crop", a.panel().currentPage());
            assertEquals("crops", a.panel().navBar().selected(), "Crops stays highlighted on the detail screen");
            size(a.panel(), 1100);
            Page detail = page(a, "crop");
            assertTrue(UiFixture.hasText(detail, "Source: FAO EcoCrop"));
            assertTrue(UiFixture.hasText(detail, "Day 75 · Harvest"));
            assertTrue(UiFixture.hasText(detail, "Care tips"));
            assertTrue(UiFixture.hasText(detail, "Pests and problems"));

            a.panel().showCrop(a.ctx().crops.findByCode("sibuyas").orElseThrow());
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(detail, "Not yet checked against the FAO EcoCrop datasheet"));
            assertTrue(UiFixture.hasText(detail, "Where these numbers come from"));
            assertTrue(UiFixture.all(detail, Badge.class).stream().anyMatch(b -> b.getText().equals("Typical range")));

            UiFixture.button(detail, "Back to crops").doClick();
            assertEquals("crops", a.panel().currentPage());
        });
    }

    @Test void plantThisButtonPlantsTheCropShown() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            List<Plot> plots = a.ctx().garden.allPlots();
            Long okra = a.ctx().crops.findByCode("okra").orElseThrow().getId();
            for (int i = 0; i < plots.size() - 1; i++) a.ctx().garden.plant(plots.get(i).getId(), okra, null);
            a.panel().showCrop(a.ctx().crops.findByCode("luya").orElseThrow());
            size(a.panel(), 1100);
            UiFixture.button(page(a, "crop"), "Plant this").doClick();
            assertEquals("Luya", a.ctx().garden.getPlot(plots.get(plots.size() - 1).getId()).getCrop().getName());
        });
    }

    // ---- Location ----

    @Test void commonPlaceButtonChangesLocationAndGoesToToday() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            a.panel().show("location");
            size(a.panel(), 390);
            UiFixture.button(page(a, "location"), "Baguio").doClick();
            assertEquals("Baguio", a.ctx().settings.current().getLocationName());
            assertEquals("today", a.panel().currentPage());
            assertEquals("Location set to Baguio.", a.panel().flashBar().message());
            size(a.panel(), 390);
            assertTrue(UiFixture.hasText(page(a, "today"), "BAGUIO"));
        });
    }

    @Test void searchingShowsResultsAndPickingOneMovesTheGarden() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            a.panel().show("location");
            size(a.panel(), 1100);
            Page location = page(a, "location");
            UiFixture.all(location, SearchField.class).get(3).setText("Taga");
            UiFixture.button(location, "Search").doClick();
            size(a.panel(), 1100);
            assertTrue(UiFixture.hasText(location, "Tagaytay, Calabarzon, Philippines"));
            UiFixture.click(UiFixture.cardWith(location, "Tagaytay, Calabarzon, Philippines"));
            assertEquals("Tagaytay, Calabarzon, Philippines", a.ctx().settings.current().getLocationName());
            assertEquals(14.10, a.ctx().settings.current().getLatitude(), 1e-9);
        });
    }

    @Test void searchRejectsTooShortTextAndExplainsWhenOffline() {
        UiFixture.edt(() -> {
            App a = app(false, true);
            a.panel().show("location");
            size(a.panel(), 1100);
            Page location = page(a, "location");
            SearchField field = UiFixture.all(location, SearchField.class).get(3);
            field.setText("T");
            UiFixture.button(location, "Search").doClick();
            assertTrue(UiFixture.hasText(location, "at least 2 letters"));
            field.setText("Tagaytay");
            UiFixture.button(location, "Search").doClick();
            assertTrue(UiFixture.hasText(location, "isn't available right now"));
            assertTrue(UiFixture.hasText(location, "Pick a common place"));
        });
    }

    // ---- looks ----

    @Test void screensActuallyPaintTheAppColours() {
        UiFixture.edt(() -> {
            App a = app(true, false);
            for (int width : new int[] {1100, 390}) {
                a.panel().setSize(width, 844);
                UiFixture.layoutTree(a.panel());
                for (String id : List.of("today", "garden", "crops", "location")) {
                    a.panel().show(id);
                    BufferedImage img = UiFixture.render(a.panel(), width, 844);
                    Set<Integer> colours = new HashSet<>();
                    for (int y = 0; y < img.getHeight(); y += 3) {
                        for (int x = 0; x < img.getWidth(); x += 3) colours.add(img.getRGB(x, y) & 0xFFFFFF);
                    }
                    assertTrue(colours.size() > 40, id + " @" + width + " looks blank: " + colours.size() + " colours");
                    assertTrue(colours.contains(Theme.PAPER.getRGB() & 0xFFFFFF), id + " has the paper background");
                    assertTrue(colours.contains(Theme.LEAF.getRGB() & 0xFFFFFF), id + " has the green navigation/hero");
                }
            }
        });
    }

    @Test void desktopRailIsDarkGreenAndPhoneBottomBarIsLight() {
        UiFixture.edt(() -> {
            App a = app(false, false);
            BufferedImage wide = UiFixture.render(a.panel(), 1100, 800);
            java.awt.Color rail = new java.awt.Color(wide.getRGB(8, 400));
            assertTrue(rail.getGreen() > rail.getRed() && rail.getGreen() < 140 && rail.getRed() < 40, "dark green rail: " + rail);
            BufferedImage narrow = UiFixture.render(a.panel(), 390, 800);
            assertEquals(Theme.WHITE.getRGB() & 0xFFFFFF, narrow.getRGB(5, 790) & 0xFFFFFF);
        });
    }

    /** Position of a component relative to the top-left of the page it is in. */
    private static final class SwingUtilitiesAbs {
        static int x(Component c) {
            int x = 0;
            for (Component p = c; p != null && !(p instanceof Page); p = p.getParent()) x += p.getX();
            return x;
        }

        static int y(Component c) {
            int y = 0;
            for (Component p = c; p != null && !(p instanceof Page); p = p.getParent()) y += p.getY();
            return y;
        }
    }
}
