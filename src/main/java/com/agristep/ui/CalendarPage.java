package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.Crop;
import com.agristep.domain.GardenSettings;
import com.agristep.service.CropRecommendation;
import com.agristep.service.PlantingCalendar;
import com.agristep.weather.ClimateStats;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** What to plant in each month at the garden's location, from the last year of weather there. */
final class CalendarPage extends Page {
    private static final int FIRST_SHOWN = 12;

    private record Snapshot(GardenSettings settings, ClimateStats climate, boolean stale, List<Crop> crops) {}

    private Snapshot snap;
    private int month = LocalDate.now().getMonthValue();
    private Crop yearCrop;
    private boolean showAll;
    private int generation;

    CalendarPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    @Override void onShow() {
        month = LocalDate.now(java.time.Clock.systemDefaultZone()).getMonthValue();
        showAll = false;
        int mine = ++generation;
        setContent(TodayPage.loadingCard("Reading the climate for your garden…"));
        Tasks.run(() -> {
            GardenSettings s = ctx.settings.current();
            ClimateStats climate = null;
            boolean stale = false;
            try {
                var result = ctx.weather.climate(s);
                climate = result.stats();
                stale = result.stale();
            } catch (RuntimeException e) {
                // no saved climate and no internet: the screen explains it
            }
            return new Snapshot(s, climate, stale, ctx.crops.findAllByOrderByNameAsc());
        }, loaded -> {
            if (mine != generation) return;
            snap = loaded;
            if (yearCrop == null || loaded.crops().stream().noneMatch(c -> c.getId().equals(yearCrop.getId()))) {
                yearCrop = loaded.crops().isEmpty() ? null : loaded.crops().get(0);
            }
            render();
        }, this::fail);
    }

    private void render() {
        Card header = banner("Planting calendar", "Which vegetables suit each month at " + snap.settings().getLocationName()
                + ", worked out from the last year of weather there.", Scenery.Photo.HILLS);
        if (!ctx.calendar.available(snap.climate())) {
            setContent(header, Widgets.notice("Month-by-month weather is not saved for this place yet. Connect to the internet, "
                    + "then open this screen again. The Today screen still ranks crops for right now."));
            return;
        }
        JPanel parts = new JPanel(new StackLayout(12));
        parts.setOpaque(false);
        if (snap.stale()) parts.add(Widgets.notice("You are offline. This calendar uses climate data saved earlier."));
        parts.add(monthChips());
        parts.add(monthCard());
        parts.add(yearCard());
        setContent(header, parts);
    }

    private JPanel monthChips() {
        JPanel chips = new JPanel(new FlowGridLayout(64, 6, 6, 0));
        chips.setOpaque(false);
        for (int m = 1; m <= 12; m++) {
            int chosen = m;
            PillButton b = m == month ? PillButton.primary(PlantingCalendar.shortMonthName(m))
                    : PillButton.secondary(PlantingCalendar.shortMonthName(m));
            b.addActionListener(e -> {
                month = chosen;
                showAll = false;
                render();
            });
            chips.add(b);
        }
        return chips;
    }

    private Card monthCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h2(PlantingCalendar.monthName(month)));
        card.add(WrapLabel.muted(ctx.calendar.describeMonth(snap.climate(), month) + " Soil: "
                + snap.settings().getSoil().getLabel().toLowerCase(Locale.ROOT) + "."));
        List<CropRecommendation> ranked = ctx.calendar.bestFor(month, snap.crops(), snap.climate(), snap.settings().getSoil());
        int limit = showAll ? ranked.size() : Math.min(FIRST_SHOWN, ranked.size());
        for (int i = 0; i < limit; i++) card.add(cropRow(ranked.get(i), i + 1));
        if (ranked.size() > FIRST_SHOWN) {
            PillButton more = PillButton.secondary(showAll ? "Show fewer" : "Show all " + ranked.size() + " crops");
            more.addActionListener(e -> {
                showAll = !showAll;
                render();
            });
            card.add(Widgets.left(more));
        }
        return card;
    }

    private Card cropRow(CropRecommendation rec, int rank) {
        Card card = new Card(Theme.PAPER, Theme.LINE).padding(10);
        JPanel row = new JPanel(new RowLayout(10, true));
        row.setOpaque(false);
        row.add(Emoji.label(rec.crop().getEmoji(), 34), RowLayout.WEST);
        JPanel body = new JPanel(new StackLayout(4));
        body.setOpaque(false);
        body.add(WrapLabel.bold(rank + ". " + rec.crop().getName() + " · " + rec.crop().getEnglishName()));
        JPanel fit = new JPanel(new RowLayout(10, false));
        fit.setOpaque(false);
        fit.add(TodayPage.fitBadge(rec), RowLayout.WEST);
        fit.add(new Meter(rec.score(), rec.score() >= 55 ? Theme.LEAF_2 : rec.score() >= 30 ? Theme.SOIL_2 : Theme.SILI), RowLayout.CENTER);
        body.add(fit);
        row.add(body, RowLayout.CENTER);
        card.add(row);
        card.onClick(() -> nav.showCrop(rec.crop()));
        return card;
    }

    private Card yearCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h2("One crop, all year"));
        card.add(WrapLabel.muted("Pick a vegetable to see how every month suits it."));
        JComboBox<Crop> box = Widgets.cropBox(snap.crops());
        box.setSelectedItem(yearCrop);
        box.addActionListener(e -> {
            Crop chosen = (Crop) box.getSelectedItem();
            if (chosen != null && chosen != yearCrop) {
                yearCrop = chosen;
                render();
            }
        });
        card.add(box);
        if (yearCrop != null) {
            JPanel strip = new JPanel(new FlowGridLayout(54, 6, 6, 0));
            strip.setOpaque(false);
            for (PlantingCalendar.MonthFit fit : ctx.calendar.yearFor(yearCrop, snap.climate(), snap.settings().getSoil())) {
                strip.add(new MonthCell(fit));
            }
            card.add(strip);
            card.add(WrapLabel.small("Dark green is a great fit, light green good, yellow possible and red not recommended."));
        }
        return card;
    }

    /** One month of the year-at-a-glance strip. */
    static final class MonthCell extends JComponent {
        private final PlantingCalendar.MonthFit fit;

        MonthCell(PlantingCalendar.MonthFit fit) { this.fit = fit; }

        PlantingCalendar.MonthFit fit() { return fit; }

        @Override public java.awt.Dimension getPreferredSize() { return new java.awt.Dimension(54, 52); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color back = switch (fit.fit()) {
                case GREAT -> Theme.LEAF;
                case GOOD -> Theme.LEAF_SOFT;
                case POSSIBLE -> Theme.LIME_SOFT;
                case POOR -> Theme.SILI_SOFT;
            };
            Color text = fit.fit() == com.agristep.service.Fit.GREAT ? Color.WHITE : Theme.INK;
            g2.setColor(back);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(text);
            g2.setFont(Theme.SMALL_BOLD);
            String name = PlantingCalendar.shortMonthName(fit.month());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(name, (getWidth() - fm.stringWidth(name)) / 2, 20);
            g2.setFont(Theme.BODY_BOLD);
            String score = String.valueOf(fit.score());
            fm = g2.getFontMetrics();
            g2.drawString(score, (getWidth() - fm.stringWidth(score)) / 2, 40);
            g2.dispose();
        }
    }
}
