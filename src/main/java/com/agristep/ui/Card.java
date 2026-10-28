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

/** A rounded white surface with a soft shadow: the building block of every screen. */
class Card extends JPanel {
    private final Color fill;
    private final Color outline;
    private boolean hover;
    private boolean hero;

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

    /** The big gradient banner at the top of a screen. */
    Card hero() {
        hero = true;
        return this;
    }

    Card padding(int px) {
        setBorder(new EmptyBorder(px, px, px, px));
        return this;
    }

    /** Makes the whole card clickable (shows a hand cursor and a soft highlight). */
    Card onClick(Runnable action) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
        return this;
    }

    @Override public Dimension getPreferredSize() {
        if (getLayout() instanceof WidthLayout) {
            int w = getWidth() > 0 ? getWidth() : 380;
            return new Dimension(w, WidthLayout.heightOf(this, w));
        }
        return super.getPreferredSize();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight() - 4;      // the last few pixels hold the shadow
        int arc = 22;
        for (int i = 4; i >= 1; i--) {   // soft layered shadow, drawn inside the card's own bounds
            g2.setColor(new Color(0x0E, 0x3B, 0x2C, hover ? 16 : 10));
            g2.fillRoundRect(i - 1, i, w - 2 * i + 2, h, arc, arc);
        }
        if (hero) {
            java.awt.Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, arc, arc);
            g2.setClip(clip);
            g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.LEAF, w, h, Theme.CANOPY_2));
            g2.fill(clip);
            g2.setColor(new Color(0xF2, 0xC9, 0x4C, 235));     // the sun, partly out of frame
            int sun = Math.min(84, Math.max(56, w / 8));
            g2.fillOval(w - sun / 2 - 4, -sun / 2, sun, sun);
            g2.setColor(new Color(0xF2, 0xC9, 0x4C, 45));
            g2.fillOval(w - sun / 2 - 4 - sun / 3, -sun / 2 - sun / 3, sun * 5 / 3, sun * 5 / 3);
            g2.setClip(null);
        } else {
            g2.setColor(hover ? Theme.LEAF_SOFT : fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.setColor(hover ? Theme.LEAF_2 : outline);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        }
        g2.dispose();
    }
}
