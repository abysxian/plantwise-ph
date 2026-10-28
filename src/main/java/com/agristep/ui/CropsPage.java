package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.Crop;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import java.util.Locale;

/** The vegetable catalogue, with search. */
final class CropsPage extends Page {
    private List<Crop> all = List.of();
    private boolean pestGuide;
    private final SearchField search = new SearchField("Search by name, e.g. talong or eggplant");
    private final JPanel list = new JPanel(new FlowGridLayout(300, 3, 10, 0));
    private final WrapLabel count = new WrapLabel("", Theme.BODY, new java.awt.Color(0xD9F0E3));

    CropsPage(AppContext ctx, Navigator nav) {
        super(ctx, nav);
        list.setOpaque(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refill(); }
            @Override public void removeUpdate(DocumentEvent e) { refill(); }
            @Override public void changedUpdate(DocumentEvent e) { refill(); }
        });
    }

    @Override void onShow() {
        Tasks.run(ctx.crops::findAllByOrderByNameAsc, crops -> {
            all = crops;
            PillButton mode = PillButton.secondary(pestGuide ? "Crop list" : "Pest guide");
            mode.addActionListener(e -> {
                pestGuide = !pestGuide;
                onShow();
            });
            Card header = banner("Crops", null, Scenery.Photo.PHILODENDRON, count, search, Widgets.left(mode));
            setContent(header, list);
            refill();
        }, this::fail);
    }

    private void refill() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        list.removeAll();
        int shown = 0;
        for (Crop crop : all) {
            if (!q.isEmpty() && !matches(crop, q)) continue;
            list.add(pestGuide ? pestCard(crop) : cropCard(crop));
            shown++;
        }
        count.setText(pestGuide
                ? (q.isEmpty() ? "Common pests and problems for each vegetable, and what to do about them. Written advice only; "
                + "for a serious outbreak ask your local agriculture office." : shown + " of " + all.size() + " match \"" + search.getText().trim() + "\".")
                : q.isEmpty()
                ? all.size() + " vegetables for Philippine gardens. Tap one for its care guide."
                : shown + " of " + all.size() + " match \"" + search.getText().trim() + "\".");
        if (shown == 0) list.add(new Card().add(WrapLabel.muted("No crop matches that search.")));
        list.revalidate();
        list.repaint();
        revalidate();
    }

    private static boolean matches(Crop c, String q) {
        return c.getName().toLowerCase(Locale.ROOT).contains(q)
                || c.getEnglishName().toLowerCase(Locale.ROOT).contains(q)
                || c.getScientificName().toLowerCase(Locale.ROOT).contains(q);
    }

    private Card cropCard(Crop crop) {
        Card card = new Card().padding(12);
        JPanel row = new JPanel(new RowLayout(12, true));
        row.setOpaque(false);
        row.add(Emoji.label(crop.getEmoji(), 44), RowLayout.WEST);
        JPanel body = new JPanel(new StackLayout(3));
        body.setOpaque(false);
        body.add(WrapLabel.h3(crop.getName()));
        body.add(WrapLabel.small(crop.getEnglishName() + " · " + crop.getScientificName()));
        body.add(WrapLabel.small("Ready in about " + crop.getCycleDays() + " days"));
        body.add(left(sourceBadge(crop)));
        row.add(body, RowLayout.CENTER);
        card.add(row);
        card.onClick(() -> nav.showCrop(crop));
        return card;
    }

    private Card pestCard(Crop crop) {
        Card card = new Card().padding(12).gap(6);
        JPanel head = new JPanel(new RowLayout(10, false));
        head.setOpaque(false);
        head.add(Emoji.label(crop.getEmoji(), 32), RowLayout.WEST);
        head.add(WrapLabel.h3(crop.getName()), RowLayout.CENTER);
        card.add(head);
        List<String> tips = crop.pestTips();
        if (tips.isEmpty()) card.add(WrapLabel.small("No pest notes for this crop yet."));
        for (String tip : tips) card.add(WrapLabel.small("• " + tip));
        card.onClick(() -> nav.showCrop(crop));
        return card;
    }

    static Badge sourceBadge(Crop crop) {
        return crop.isVerified()
                ? new Badge("FAO verified", java.awt.Color.WHITE, Theme.LEAF)
                : new Badge("Typical range", Theme.SOIL, Theme.LIME_SOFT);
    }

    /** Keeps a badge at its natural size instead of stretching across a column. */
    static JPanel left(java.awt.Component c) {
        JPanel row = new JPanel(new RowLayout(0, true));
        row.setOpaque(false);
        row.add(c, RowLayout.WEST);
        return row;
    }
}
