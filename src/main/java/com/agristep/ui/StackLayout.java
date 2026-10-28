package com.agristep.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

/** Stacks children top to bottom at full width. An optional maximum width centres the column on wide windows. */
final class StackLayout implements LayoutManager2, WidthLayout {
    private final int gap;
    private final int maxWidth;

    StackLayout(int gap) { this(gap, Integer.MAX_VALUE); }

    StackLayout(int gap, int maxWidth) {
        this.gap = gap;
        this.maxWidth = maxWidth;
    }

    @Override public int heightFor(Container parent, int width) {
        int w = Math.min(width, maxWidth);
        int total = 0;
        int shown = 0;
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) continue;
            total += WidthLayout.heightOf(c, w);
            shown++;
        }
        return total + Math.max(0, shown - 1) * gap;
    }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int avail = Math.max(1, parent.getWidth() - in.left - in.right);
        int w = Math.min(avail, maxWidth);
        int x = in.left + (avail - w) / 2;
        int y = in.top;
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) continue;
            int h = WidthLayout.heightOf(c, w);
            c.setBounds(x, y, w, h);
            y += h + gap;
        }
    }

    @Override public Dimension preferredLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        int w = parent.getWidth() > 0 ? parent.getWidth() : 380;
        int inner = Math.max(1, w - in.left - in.right);
        return new Dimension(w, in.top + in.bottom + heightFor(parent, inner));
    }

    @Override public Dimension minimumLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        return new Dimension(in.left + in.right + 40, in.top + in.bottom);
    }

    @Override public Dimension maximumLayoutSize(Container target) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    @Override public void addLayoutComponent(Component comp, Object constraints) {}
    @Override public void addLayoutComponent(String name, Component comp) {}
    @Override public void removeLayoutComponent(Component comp) {}
    @Override public float getLayoutAlignmentX(Container target) { return 0; }
    @Override public float getLayoutAlignmentY(Container target) { return 0; }
    @Override public void invalidateLayout(Container target) {}
}
