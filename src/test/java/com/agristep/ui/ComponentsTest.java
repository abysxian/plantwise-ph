package com.agristep.ui;

import com.agristep.domain.SoilType;
import com.agristep.domain.TaskKind;
import com.agristep.weather.WeatherCodes;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The building blocks: emoji pictures, wrapped text, and the layouts that make the app responsive. */
class ComponentsTest {

    // ---- emoji ----

    @Test void everyEmojiTheAppUsesHasABundledPicture() throws Exception {
        java.util.Set<String> wanted = new java.util.TreeSet<>();
        try (var in = ComponentsTest.class.getResourceAsStream("/data/crops.json")) {
            for (var crop : com.agristep.config.CropCatalog.load(in)) wanted.add(crop.getEmoji());
        }
        for (TaskKind k : TaskKind.values()) wanted.add(k.getIcon());
        for (SoilType s : SoilType.values()) wanted.add(s.getEmoji());
        for (int code : new int[] {0, 1, 2, 3, 45, 51, 61, 71, 80, 95, 999}) wanted.add(WeatherCodes.emoji(code));
        wanted.addAll(List.of("☀️", "🏡", "📖", "📍", "🌱"));
        for (String e : wanted) assertNotNull(Emoji.image(e), "missing picture for " + e + " (" + Emoji.fileName(e) + ")");
    }

    @Test void emojiFileNamesDropTheVariationSelector() {
        assertEquals("1f346.png", Emoji.fileName("🍆"));
        assertEquals("2600.png", Emoji.fileName("☀️"));
        assertEquals("1f3d6.png", Emoji.fileName("🏖️"));
    }

    @Test void unknownEmojiGivesAnEmptyIconInsteadOfCrashing() {
        assertNull(Emoji.image("not an emoji"));
        assertEquals(24, Emoji.icon("not an emoji", 24).getIconWidth());
    }

    // ---- WrapLabel ----

    private static WrapLabel label(String text) { return WrapLabel.body(text); }

    @Test void textWrapsToTheWidthAndEveryLineFits() {
        WrapLabel l = label("Water the plants early in the morning so the soil stays damp through the day");
        int width = 160;
        List<String> lines = l.lines(width);
        assertTrue(lines.size() > 2);
        var fm = l.getFontMetrics(l.getFont());
        lines.forEach(line -> assertTrue(fm.stringWidth(line) <= width, line));
        assertEquals(l.getText(), String.join(" ", lines));
    }

    @Test void narrowerMeansTallerAndHeightMatchesLineCount() {
        WrapLabel l = label("one two three four five six seven eight nine ten eleven twelve");
        assertTrue(l.heightFor(120) > l.heightFor(400));
        assertEquals(l.lines(200).size() * l.heightFor(100000), l.heightFor(200));
    }

    @Test void aWordLongerThanTheWidthIsBrokenInsteadOfOverflowing() {
        WrapLabel l = label("Supercalifragilisticexpialidocious");
        var fm = l.getFontMetrics(l.getFont());
        List<String> lines = l.lines(80);
        assertTrue(lines.size() > 1);
        lines.forEach(line -> assertTrue(fm.stringWidth(line) <= 80, line));
    }

    @Test void newlinesAreKeptAndMaxLinesAddsAnEllipsis() {
        assertEquals(List.of("a", "b"), label("a\nb").lines(300));
        WrapLabel l = label("one two three four five six seven eight nine ten eleven twelve thirteen fourteen").maxLines(2);
        List<String> lines = l.lines(120);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith("…"));
    }

    @Test void emptyTextStillTakesOneLine() {
        assertEquals(1, label("").lines(100).size());
        assertTrue(label("").heightFor(100) > 0);
    }

    // ---- layouts ----

    private static JPanel stack(int gap, int maxWidth, String... texts) {
        JPanel p = new JPanel(new StackLayout(gap, maxWidth));
        for (String t : texts) p.add(label(t));
        return p;
    }

    @Test void stackPlacesChildrenTopToBottomWithGaps() {
        JPanel p = stack(10, Integer.MAX_VALUE, "first", "second", "third");
        p.setSize(300, 500);
        p.doLayout();
        Component[] c = p.getComponents();
        assertEquals(0, c[0].getY());
        assertEquals(c[0].getHeight() + 10, c[1].getY());
        assertEquals(c[1].getY() + c[1].getHeight() + 10, c[2].getY());
        assertEquals(300, c[0].getWidth());
        assertEquals(c[2].getY() + c[2].getHeight(), ((WidthLayout) p.getLayout()).heightFor(p, 300));
    }

    @Test void stackWithMaxWidthCentresItsColumn() {
        JPanel p = stack(8, 400, "hello");
        p.setSize(1000, 100);
        p.doLayout();
        assertEquals(400, p.getComponent(0).getWidth());
        assertEquals(300, p.getComponent(0).getX());
    }

    @Test void flowGridPicksColumnsFromTheWidth() {
        FlowGridLayout g = new FlowGridLayout(150, 3, 8, 0);
        assertEquals(1, g.columnsFor(100));
        assertEquals(1, g.columnsFor(300));
        assertEquals(2, g.columnsFor(330));
        assertEquals(3, g.columnsFor(480));
        assertEquals(3, g.columnsFor(2000));
    }

    @Test void flowGridWithAspectMakesSquareCells() {
        JPanel p = new JPanel(new FlowGridLayout(90, 3, 10, 1.0));
        for (int i = 0; i < 9; i++) p.add(new JPanel());
        p.setSize(350, 1000);
        p.doLayout();
        for (Component c : p.getComponents()) assertEquals(c.getWidth(), c.getHeight());
        long columns = java.util.Arrays.stream(p.getComponents()).map(Component::getX).distinct().count();
        assertEquals(3, columns);
    }

    @Test void flowGridMakesCardsInARowTheSameHeight() {
        JPanel p = new JPanel(new FlowGridLayout(150, 2, 8, 0));
        p.add(label("short"));
        p.add(label("a much longer piece of text that needs several lines when the column is narrow like this one"));
        p.setSize(330, 500);
        p.doLayout();
        assertEquals(p.getComponent(0).getHeight(), p.getComponent(1).getHeight());
    }

    @Test void rowGivesTheCentreWhatTheEdgesLeave() {
        JPanel p = new JPanel(new RowLayout(10, false));
        JPanel west = new JPanel();
        west.setPreferredSize(new java.awt.Dimension(40, 40));
        JPanel east = new JPanel();
        east.setPreferredSize(new java.awt.Dimension(60, 30));
        WrapLabel centre = label("middle text");
        p.add(west, RowLayout.WEST);
        p.add(centre, RowLayout.CENTER);
        p.add(east, RowLayout.EAST);
        p.setSize(400, 100);
        p.doLayout();
        assertEquals(0, west.getX());
        assertEquals(50, centre.getX());
        assertEquals(400 - 60, east.getX());
        assertEquals(400 - 40 - 60 - 20, centre.getWidth());
    }

    @Test void splitGoesSideBySideWhenWideAndStacksWhenNarrow() {
        JPanel p = new JPanel(new SplitLayout(800, 300, 14));
        p.add(label("main main main"));
        p.add(label("side"));
        p.setSize(1000, 400);
        p.doLayout();
        assertEquals(1000 - 300 - 14, p.getComponent(0).getWidth());
        assertEquals(p.getComponent(0).getY(), p.getComponent(1).getY());
        assertEquals(1000 - 300, p.getComponent(1).getX());
        assertEquals(300, p.getComponent(1).getWidth());

        p.setSize(400, 400);
        p.doLayout();
        assertEquals(400, p.getComponent(0).getWidth());
        assertTrue(p.getComponent(1).getY() > p.getComponent(0).getY());
    }

    @Test void cardHeightGrowsWhenItsTextHasToWrap() {
        Card card = new Card();
        card.add(label("This card holds some text that is long enough to wrap onto several lines when narrow."));
        assertTrue(WidthLayout.heightOf(card, 200) > WidthLayout.heightOf(card, 600));
    }

    // ---- small widgets ----

    @Test void meterClampsItsValue() {
        assertEquals(100, new Meter(250, Color.GREEN).getValue());
        assertEquals(0, new Meter(-5, Color.GREEN).getValue());
    }

    @Test void flashBarShowsAndHides() {
        UiFixture.edt(() -> {
            FlashBar bar = new FlashBar();
            assertEquals("", bar.message());
            bar.show("Saved.", false);
            assertEquals("Saved.", bar.message());
            bar.dismiss();
            assertEquals("", bar.message());
        });
    }

    @Test void checkToggleCallsBackOnClick() {
        int[] hits = {0};
        CheckToggle t = new CheckToggle(false, false, () -> hits[0]++);
        UiFixture.click(t);
        assertEquals(1, hits[0]);
    }
}
