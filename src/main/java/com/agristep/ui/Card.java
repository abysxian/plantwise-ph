package com.agristep.ui;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** A rounded white surface with a soft shadow: the building block of every screen. A "hero" card shows a picture. */
class Card extends JPanel {
    private static final int ARC = 22;

    private final Color fill;
    private final Color outline;
    private final Motion.Tween hover = new Motion.Tween(this, 160);
    private boolean clickable;
    private Scenery.Photo photo;
    private Scenery.Sky sky = Scenery.Sky.CLEAR;

    Card() { this(Theme.WHITE, Theme.LINE); }

    Card(Color fill) { this(fill, Theme.LINE); }

    Card(Color fill, Color outline) {
        this.fill = fill;
        this.outline = outline;
        setOpaque(false);
        setLayout(new StackLayout(8));
        setBorder(new EmptyBorder(14, 14, 14, 14));
    }

    Card gap(int gap) {
        setLayout(new StackLayout(gap));
        return this;
    }

    /** The big picture banner at the top of a screen. */
    Card hero(Scenery.Photo photo, Scenery.Sky sky) {
        this.photo = photo;
        this.sky = sky;
        Motion.ambient(this);
        return this;
    }

    Card hero() { return hero(Scenery.Photo.PHILODENDRON, Scenery.Sky.CLEAR); }

    boolean isHero() { return photo != null; }

    Card padding(int px) {
        setBorder(new EmptyBorder(px, px, px, px));
        return this;
    }

    /** Makes the whole card clickable: a hand cursor, a glow and a small lift when the pointer is over it. */
    Card onClick(Runnable action) {
        clickable = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
            @Override public void mouseEntered(MouseEvent e) { hover.to(1); }
            @Override public void mouseExited(MouseEvent e) { hover.to(0); }
        });
        return this;
    }

    @Override public Dimension getPreferredSize() {
        if (getLayout() instanceof WidthLayout) {
            int w = getWidth() > 0 ? getWidth() : 380;
            int h = WidthLayout.heightOf(this, w);
            if (photo != null) h = Math.max(h, 140);   // room for the photo to show
            return new Dimension(w, h);
        }
        return super.getPreferredSize();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight() - 4;      // the last few pixels hold the shadow
        float lift = hover.value();
        int top = -Math.round(lift * 2);
        for (int i = 4; i >= 1; i--) {   // soft layered shadow, drawn inside the card's own bounds
            g2.setColor(new Color(0x0E, 0x3B, 0x2C, 10 + Math.round(lift * 9)));
            g2.fillRoundRect(i - 1, i + top + 1, w - 2 * i + 2, h, ARC, ARC);
        }
        if (photo != null) {
            Scenery.paintBanner(g2, photo, sky, w, h, ARC, Motion.seconds());
        } else {
            g2.translate(0, top);
            g2.setColor(Theme.mix(fill, Theme.LEAF_SOFT, lift));
            g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
            g2.setColor(Theme.mix(outline, Theme.LEAF_2, lift));
            g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }
        g2.dispose();
    }

    boolean isClickable() { return clickable; }
}
