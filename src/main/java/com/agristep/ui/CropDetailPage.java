package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.Crop;
import com.agristep.domain.SoilType;
import com.agristep.domain.TaskTemplate;

import javax.swing.JPanel;
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Everything about one vegetable: what it needs, where the numbers come from, and its care plan. */
final class CropDetailPage extends Page {
    private Crop crop;

    CropDetailPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    void setCrop(Crop crop) { this.crop = crop; }

    @Override void onShow() {
        if (crop == null) {
            nav.show(Navigator.CROPS);
            return;
        }
        PillButton back = PillButton.secondary("Back to crops");
        back.addActionListener(e -> nav.show(Navigator.CROPS));
        setContent(CropsPage.left(back), header(), needs(), source(), carePlan(),
                tips("Care tips", crop.careTips()), tips("Pests and problems", crop.pestTips()));
    }

    private Card header() {
        Card card = new Card(Theme.LEAF_SOFT, Theme.LEAF_SOFT).gap(10).padding(16);
        JPanel row = new JPanel(new RowLayout(14, false));
        row.setOpaque(false);
        row.add(Emoji.label(crop.getEmoji(), 64), RowLayout.WEST);
        JPanel titles = new JPanel(new StackLayout(3));
        titles.setOpaque(false);
        titles.add(WrapLabel.h1(crop.getName()));
        titles.add(WrapLabel.muted(crop.getEnglishName() + " · " + crop.getScientificName()));
        titles.add(CropsPage.left(CropsPage.sourceBadge(crop)));
        row.add(titles, RowLayout.CENTER);
        card.add(row);
        PillButton plant = PillButton.primary("Plant this");
        plant.addActionListener(e -> plantInBed(crop));
        card.add(CropsPage.left(plant));
        return card;
    }

    private Card needs() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h2("What it needs"));
        JPanel facts = new JPanel(new FlowGridLayout(150, 3, 8, 0));
        facts.setOpaque(false);
        facts.add(fact("Temperature", crop.getTemperature().describeOptimal("°C") + " is ideal"));
        facts.add(fact("Rainfall", crop.getRainfall().describeOptimal("mm") + " a year"));
        facts.add(fact("Soil pH", crop.getPh().describeOptimal("")));
        facts.add(fact("Sunlight", crop.getSunlight()));
        facts.add(fact("Spacing", crop.getSpacing()));
        facts.add(fact("Time to harvest", "About " + crop.getCycleDays() + " days"));
        facts.add(fact("Best soil", soilNames(crop.getOptimalSoils())));
        Set<SoilType> also = crop.getTolerableSoils().stream()
                .filter(s -> !crop.getOptimalSoils().contains(s)).collect(Collectors.toSet());
        if (!also.isEmpty()) facts.add(fact("Also grows in", soilNames(also)));
        card.add(facts);
        return card;
    }

    private static String soilNames(Set<SoilType> soils) {
        return soils.stream().sorted().map(s -> I18n.t(s.getLabel().toLowerCase(Locale.ROOT)))
                .collect(Collectors.joining(", "));
    }

    private static Card fact(String label, String value) {
        Card tile = new Card(Theme.PAPER, Theme.PAPER).gap(2).padding(10);
        tile.add(WrapLabel.small(I18n.t(label).toUpperCase(I18n.current() == I18n.Language.FILIPINO ? Locale.forLanguageTag("fil-PH") : Locale.ROOT)));
        tile.add(WrapLabel.bold(value));
        return tile;
    }

    private Card source() {
        Card card = new Card(crop.isVerified() ? Theme.WHITE : Theme.LIME_SOFT,
                crop.isVerified() ? Theme.LINE : Theme.LIME).gap(8);
        card.add(WrapLabel.h3(crop.isVerified() ? "Source: FAO EcoCrop" : "Where these numbers come from"));
        card.add(WrapLabel.body(crop.getSourceNote()));
        if (!crop.isVerified()) {
            card.add(WrapLabel.small("Treat the ranges as a guide and check your local agriculture office for your area."));
        }
        if (crop.getSourceUrl() != null && !crop.getSourceUrl().isBlank()) {
            PillButton open = PillButton.secondary("Open source page");
            open.addActionListener(e -> openLink(crop.getSourceUrl()));
            card.add(CropsPage.left(open));
        }
        return card;
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                nav.flash("Open this address in your browser: " + url, false);
            }
        } catch (Exception ex) {
            nav.flash("Couldn't open the browser. Address: " + url, true);
        }
    }

    private Card carePlan() {
        Card card = new Card().gap(8);
        card.add(WrapLabel.h2("Care plan"));
        card.add(WrapLabel.muted("What to do, counted from the day you plant."));
        LocalDate anyDay = LocalDate.now();
        for (TaskTemplate t : crop.getTaskTemplates()) {
            String instructions = t.getKind().create(t.getDayOffset(), anyDay, t.getAmount(), t.getDetail(), t.getExtra())
                    .getInstructions();
            Card step = new Card(Theme.PAPER, Theme.PAPER).padding(10);
            JPanel row = new JPanel(new RowLayout(10, true));
            row.setOpaque(false);
            row.add(Emoji.label(t.getKind().getIcon(), 26), RowLayout.WEST);
            JPanel body = new JPanel(new StackLayout(2));
            body.setOpaque(false);
            String when = t.getDayOffset() == 0 ? "Day 0 · planting day" : "Day " + t.getDayOffset();
            body.add(WrapLabel.bold(when + " · " + t.getKind().getTitle()));
            body.add(WrapLabel.body(instructions));
            row.add(body, RowLayout.CENTER);
            step.add(row);
            card.add(step);
        }
        return card;
    }

    private Card tips(String title, List<String> tips) {
        Card card = new Card().gap(6);
        card.add(WrapLabel.h2(title));
        if (tips.isEmpty()) card.add(WrapLabel.muted("No tips yet."));
        for (String tip : tips) {
            JPanel row = new JPanel(new RowLayout(8, true));
            row.setOpaque(false);
            row.add(WrapLabel.bold("•"), RowLayout.WEST);
            row.add(WrapLabel.body(tip), RowLayout.CENTER);
            card.add(row);
        }
        return card;
    }
}
