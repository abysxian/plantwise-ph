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
