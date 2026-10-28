package com.agristep.ui;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * The base of every screen: a column of cards that always matches the window width (no sideways scrolling)
 * and is centred with a maximum width on big monitors.
 */
class ScrollPage extends JPanel implements Scrollable {
    static final int MAX_CONTENT_WIDTH = 1000;

    ScrollPage() {
        setBackground(Theme.PAPER);
        setLayout(new StackLayout(14, MAX_CONTENT_WIDTH));
        setBorder(new EmptyBorder(16, 16, 28, 16));
    }

    /** A few faint leaves in the bottom-right corner, so the page background is not a flat colour. */
    @Override protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (!isOpaque() || getWidth() < 200) return;
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        float w = getWidth();
        float h = getHeight();
        g2.setColor(new java.awt.Color(31, 157, 91, 20));
        g2.fill(Scenery.leaf(w - 20, h - 10, w - 150, h - 150, 38));
        g2.fill(Scenery.leaf(w - 20, h - 10, w - 40, h - 190, 30));
        g2.setColor(new java.awt.Color(242, 201, 76, 26));
        g2.fill(Scenery.leaf(w - 60, h - 10, w - 230, h - 70, 26));
        g2.dispose();
    }

    @Override public Dimension getPreferredSize() {
        Container parent = getParent();
        int w = parent instanceof JViewport ? parent.getWidth() : getWidth();
        if (w <= 0) w = 380;
        Insets in = getInsets();
        int inner = Math.max(1, w - in.left - in.right);
        return new Dimension(w, in.top + in.bottom + ((StackLayout) getLayout()).heightFor(this, inner));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(380, 500); }

    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }

    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? (int) (visible.height * 0.9) : visible.width;
    }

    @Override public boolean getScrollableTracksViewportWidth() { return true; }

    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
