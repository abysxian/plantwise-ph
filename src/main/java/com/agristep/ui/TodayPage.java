package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.GardenSettings;
import com.agristep.domain.SoilType;
import com.agristep.service.CropRecommendation;
import com.agristep.service.RecommendationResult;
import com.agristep.weather.DailyWeather;
import com.agristep.weather.WeatherCodes;
import com.agristep.weather.WeatherForecast;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** "What should I do today?": the weather, the soil choice, and crops ranked for this place. */
final class TodayPage extends Page {
    private static final int FIRST_SHOWN = 10;

    private record Snapshot(GardenSettings settings, Optional<WeatherForecast> forecast, String advice) {}

    private int generation;
    private boolean showAll;
    private JPanel recsSection;
    private GardenSettings settings;

    TodayPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    @Override void onShow() {
        showAll = false;
        load();
    }

    private void load() {
        int mine = ++generation;
        setContent(loadingCard("Checking the weather and your garden…"));
        Tasks.run(() -> {
            GardenSettings s = ctx.settings.current();
            Optional<WeatherForecast> forecast = ctx.weather.forecast(s);
            String advice = ctx.advisor.todayAdvice(forecast, ctx.garden.dueTaskCount(), ctx.garden.readyPlotCount());
            return new Snapshot(s, forecast, advice);
        }, snap -> {
            if (mine == generation) render(snap);
        }, this::fail);
    }

    static Card loadingCard(String text) {
        Card card = new Card();
        card.add(WrapLabel.muted(text).align(WrapLabel.CENTER));
        return card;
    }

    private void render(Snapshot snap) {
        settings = snap.settings();
        recsSection = new JPanel(new StackLayout(10));
        recsSection.setOpaque(false);
        recsSection.add(WrapLabel.h2("What to plant"));
        recsSection.add(WrapLabel.muted("Checking what fits " + settings.getLocationName() + "…"));
        setContent(hero(snap), weatherCard(snap.forecast()), soilCard(), recsSection);
        loadRecommendations();
    }

    private void loadRecommendations() {
        int mine = generation;
        GardenSettings s = settings;
        Tasks.run(() -> ctx.recommendations.recommend(s), result -> {
            if (mine == generation) fillRecommendations(result);
        }, t -> {
            if (mine == generation) fail(t);
        });
    }

    // ---- hero ----

    private Card hero(Snapshot snap) {
        Card hero = new Card(Theme.LEAF, Theme.LEAF).hero().gap(10).padding(20);
        hero.add(new WrapLabel("TODAY IN " + snap.settings().getLocationName().toUpperCase(Locale.ROOT),
                Theme.SMALL_BOLD, Theme.LIME));
        hero.add(new WrapLabel(snap.advice(), Theme.H2, Color.WHITE));
        JPanel row = new JPanel(new RowLayout(8, false));
        row.setOpaque(false);
        PillButton change = PillButton.secondary("Change place");
        change.addActionListener(e -> nav.show(Navigator.LOCATION));
        row.add(change, RowLayout.WEST);
        hero.add(row);
        return hero;
    }

    // ---- weather ----

    private Card weatherCard(Optional<WeatherForecast> forecast) {
        Card card = new Card().gap(12);
        if (forecast.isEmpty()) {
            card.add(WrapLabel.h3("Weather"));
            card.add(WrapLabel.muted("The weather service can't be reached right now. Check your internet connection. "
                    + "Crop suggestions still work from the climate data saved earlier."));
            return card;
        }
        WeatherForecast f = forecast.get();
        JPanel now = new JPanel(new RowLayout(12, false));
        now.setOpaque(false);
        now.add(Emoji.label(WeatherCodes.emoji(f.currentWeatherCode()), 52), RowLayout.WEST);
        JPanel text = new JPanel(new StackLayout(0));
        text.setOpaque(false);
        text.add(new WrapLabel(Math.round(f.currentTempC()) + " °C", Theme.BIG_NUMBER, Theme.INK));
        text.add(WrapLabel.muted(WeatherCodes.describe(f.currentWeatherCode()) + " · humidity "
                + f.humidityPercent() + "%"));
        now.add(text, RowLayout.CENTER);
        card.add(now);

        List<DailyWeather> days = f.days();
        JPanel strip = new JPanel(new GridLayout(1, Math.max(1, days.size()), 4, 0));
        strip.setOpaque(false);
        for (int i = 0; i < days.size(); i++) strip.add(dayCell(days.get(i), i == 0));
        card.add(strip);
        return card;
    }

    private JComponent dayCell(DailyWeather d, boolean today) {
        JPanel cell = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(today ? Theme.LIME_SOFT : Theme.PAPER);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        String name = today ? "Today" : d.date().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        cell.add(centered(name, Theme.font(java.awt.Font.BOLD, 11), Theme.MUTED));
        JLabel icon = Emoji.label(WeatherCodes.emoji(d.weatherCode()), 28);
        icon.setAlignmentX(0.5f);
        icon.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        cell.add(icon);
        cell.add(centered(Math.round(d.tempMaxC()) + "°", Theme.BODY_BOLD, Theme.INK));
        cell.add(centered(Math.round(d.tempMinC()) + "°", Theme.font(java.awt.Font.PLAIN, 11), Theme.MUTED));
        cell.add(centered(d.rainMm() >= 0.5 ? Math.round(d.rainMm()) + "mm" : "dry", Theme.font(java.awt.Font.PLAIN, 11),
                d.rainMm() >= 0.5 ? Theme.RAIN : Theme.MUTED));
        return cell;
    }

    private static JLabel centered(String text, java.awt.Font font, Color color) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(0.5f);
        return label;
    }

    // ---- soil ----

    private Card soilCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h3("What soil is in your garden?"));
        card.add(WrapLabel.muted("Squeeze a handful when it is damp. The feel tells you which one it is."));
        JPanel options = new JPanel(new FlowGridLayout(150, 3, 8, 0));
        options.setOpaque(false);
        for (SoilType soil : SoilType.values()) {
            boolean selected = soil == settings.getSoil();
            Card option = new Card(selected ? Theme.LEAF_SOFT : Theme.WHITE, selected ? Theme.LEAF : Theme.LINE)
                    .gap(4).padding(12);
            option.add(iconLeft(soil.getEmoji()));
            option.add(WrapLabel.bold(soil.getLabel()));
            option.add(WrapLabel.small(soil.getDescription()));
            option.onClick(() -> chooseSoil(soil));
            options.add(option);
        }
        card.add(options);
        return card;
    }

    private static JComponent iconLeft(String emoji) {
        JPanel row = new JPanel(new RowLayout(0, false));
        row.setOpaque(false);
        row.add(Emoji.label(emoji, 28), RowLayout.WEST);
        return row;
    }

    private void chooseSoil(SoilType soil) {
        if (soil == settings.getSoil()) return;
        Tasks.run(() -> ctx.settings.changeSoil(soil), changed -> {
            nav.flash("Soil set to " + soil.getLabel().toLowerCase(Locale.ROOT) + ".", false);
            load();
        }, this::fail);
    }

    // ---- recommendations ----

    private void fillRecommendations(RecommendationResult result) {
        recsSection.removeAll();
        recsSection.add(WrapLabel.h2("What to plant"));
        recsSection.add(WrapLabel.muted("Ranked for " + settings.getLocationName() + " with "
                + settings.getSoil().getLabel().toLowerCase(Locale.ROOT) + "."));
        if (result.degraded()) {
            recsSection.add(notice("No weather data is available, so crops are ranked by soil only."));
        } else if (result.stale()) {
            recsSection.add(notice("You are offline. Ranking uses climate data saved earlier."));
        }
        List<CropRecommendation> items = result.items();
        int limit = showAll ? items.size() : Math.min(FIRST_SHOWN, items.size());
        for (int i = 0; i < limit; i++) recsSection.add(recommendationRow(items.get(i), i + 1));
        if (items.size() > FIRST_SHOWN) {
            PillButton more = PillButton.secondary(showAll ? "Show fewer" : "Show all " + items.size() + " crops");
            more.addActionListener(e -> {
                showAll = !showAll;
                fillRecommendations(result);
            });
            JPanel row = new JPanel(new RowLayout(0, false));
            row.setOpaque(false);
            row.add(more, RowLayout.WEST);
            recsSection.add(row);
        }
        recsSection.revalidate();
        recsSection.repaint();
        revalidate();
    }

    private static Card notice(String text) {
        Card card = new Card(Theme.LIME_SOFT, Theme.LIME).padding(12);
        card.add(WrapLabel.body(text));
        return card;
    }

    private Card recommendationRow(CropRecommendation rec, int rank) {
        Card card = new Card().padding(12);
        JPanel row = new JPanel(new RowLayout(12, true));
        row.setOpaque(false);
        row.add(Emoji.label(rec.crop().getEmoji(), 40), RowLayout.WEST);

        JPanel body = new JPanel(new StackLayout(5));
        body.setOpaque(false);
        body.add(WrapLabel.h3(rank + ". " + rec.crop().getName()));
        body.add(WrapLabel.small(rec.crop().getEnglishName()));

        JPanel fitRow = new JPanel(new RowLayout(10, false));
        fitRow.setOpaque(false);
        fitRow.add(fitBadge(rec), RowLayout.WEST);
        fitRow.add(new Meter(rec.score(), meterColor(rec)), RowLayout.CENTER);
        body.add(fitRow);

        List<String> reasons = rec.reasons();
        if (!reasons.isEmpty()) {
            body.add(WrapLabel.small(String.join("\n", reasons.subList(0, Math.min(2, reasons.size())))));
        }
        row.add(body, RowLayout.CENTER);

        boolean good = rec.score() >= 55;
        PillButton plant = good ? PillButton.primary("Plant") : PillButton.secondary("Plant");
        plant.addActionListener(e -> plantInBed(rec.crop()));
        row.add(plant, RowLayout.EAST);

        card.add(row);
        card.onClick(() -> nav.showCrop(rec.crop()));
        return card;
    }

    static Badge fitBadge(CropRecommendation rec) {
        String text = rec.fit().getLabel() + " · " + rec.score();
        return switch (rec.fit()) {
            case GREAT -> new Badge(text, Color.WHITE, Theme.LEAF);
            case GOOD -> new Badge(text, Theme.LEAF, Theme.LEAF_SOFT);
            case POSSIBLE -> new Badge(text, Theme.SOIL, Theme.LIME_SOFT);
            case POOR -> new Badge(text, Theme.SILI, Theme.SILI_SOFT);
        };
    }

    private static Color meterColor(CropRecommendation rec) {
        return switch (rec.fit()) {
            case GREAT, GOOD -> Theme.LEAF_2;
            case POSSIBLE -> Theme.SOIL_2;
            case POOR -> Theme.SILI;
        };
    }
}
