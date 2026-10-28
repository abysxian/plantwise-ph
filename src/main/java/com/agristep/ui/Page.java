package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.Crop;
import com.agristep.domain.Plot;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.List;

/** One screen of the app. Subclasses fill themselves in {@link #onShow()} each time the person opens them. */
abstract class Page extends ScrollPage {
    protected final AppContext ctx;
    protected final Navigator nav;
    private final Motion.Tween fade = new Motion.Tween(this, 320);

    Page(AppContext ctx, Navigator nav) {
        this.ctx = ctx;
        this.nav = nav;
    }

    /** Called every time the screen becomes visible: reload the data and rebuild. */
    abstract void onShow();

    /** Replaces everything on the screen. */
    protected void setContent(Component... parts) {
        removeAll();
        for (Component c : parts) add(c);
        add(Footer.create());
        revalidate();
        repaint();
        fade.jump(0f);
        fade.to(1f);
    }

    /** New content eases in from the page background instead of popping in. */
    @Override protected void paintChildren(java.awt.Graphics g) {
        super.paintChildren(g);
        float v = fade.value();
        if (v < 1f) {
            g.setColor(new java.awt.Color(Theme.PAPER.getRed(), Theme.PAPER.getGreen(), Theme.PAPER.getBlue(),
                    Math.round(255 * (1f - v))));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** The picture banner at the top of a screen: a title, one line under it, and optionally more parts. */
    static Card banner(String title, String subtitle, Scenery.Photo photo, java.awt.Component... extra) {
        Card banner = new Card(Theme.LEAF, Theme.LEAF).hero(photo, Scenery.Sky.CLEAR).gap(8).padding(18);
        banner.add(new WrapLabel(I18n.t(title), Theme.H1, java.awt.Color.WHITE));
        if (subtitle != null && !subtitle.isEmpty()) banner.add(new WrapLabel(subtitle, Theme.BODY, new java.awt.Color(0xD9F0E3)));
        for (java.awt.Component c : extra) banner.add(c);
        return banner;
    }

    protected void fail(Throwable t) {
        nav.flash(Tasks.messageOf(t), true);
    }

    /** Asks which empty bed to use, then plants the crop there. Shared by Today and the crop pages. */
    protected void plantInBed(Crop crop) {
        Tasks.run(() -> ctx.garden.allPlots().stream().filter(Plot::isEmpty).toList(), empty -> {
            if (empty.isEmpty()) {
                nav.flash("All your beds are full. Add a bed or harvest one first.", true);
                nav.show(Navigator.GARDEN);
                return;
            }
            Plot chosen = empty.size() == 1 ? empty.get(0) : askBed(crop, empty);
            if (chosen == null) return;
            Tasks.run(() -> ctx.garden.plant(chosen.getId(), crop.getId(), null), planted -> {
                nav.flash("Planted " + crop.getName() + " in " + planted.getName() + ".", false);
                nav.show(Navigator.GARDEN);
            }, this::fail);
        }, this::fail);
    }

    private Plot askBed(Crop crop, List<Plot> empty) {
        JComboBox<Plot> box = new JComboBox<>(empty.toArray(new Plot[0]));
        box.setRenderer((list, value, index, selected, focus) -> {
            javax.swing.JLabel label = new javax.swing.JLabel(value == null ? "" : value.getName());
            label.setOpaque(true);
            label.setFont(Theme.BODY);
            label.setBackground(selected ? Theme.LEAF_SOFT : Theme.WHITE);
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        });
        int answer = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), box,
                I18n.t("Which bed for " + crop.getName() + "?"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return answer == JOptionPane.OK_OPTION ? (Plot) box.getSelectedItem() : null;
    }
}
