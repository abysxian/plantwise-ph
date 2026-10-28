package com.agristep.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.Map;

/**
 * One row with a fixed-size west part, a flexible centre that takes the leftover width (and wraps its text),
 * and a fixed-size east part. Add children with the constraints {@link #WEST}, {@link #CENTER}, {@link #EAST}.
 */
final class RowLayout implements LayoutManager2, WidthLayout {
    static final String WEST = "west";
    static final String CENTER = "center";
    static final String EAST = "east";

    private final int gap;
    private final boolean alignTop;
    private final Map<Component, String> parts = new HashMap<>();

    RowLayout(int gap, boolean alignTop) {
        this.gap = gap;
        this.alignTop = alignTop;
    }

    private Component part(Container parent, String name) {
        for (Component c : parent.getComponents()) {
            if (c.isVisible() && name.equals(parts.get(c))) return c;
        }
        return null;
    }

    private int fixedWidth(Component c) { return c == null ? 0 : c.getPreferredSize().width; }

    private int centerWidth(Container parent, int width) {
        Component west = part(parent, WEST);
        Component east = part(parent, EAST);
        int used = fixedWidth(west) + fixedWidth(east);
        int gaps = (west != null ? gap : 0) + (east != null ? gap : 0);
        return Math.max(1, width - used - gaps);
    }

    @Override public int heightFor(Container parent, int width) {
        int cw = centerWidth(parent, width);
        int h = 0;
        Component west = part(parent, WEST);
        Component center = part(parent, CENTER);
        Component east = part(parent, EAST);
        if (west != null) h = Math.max(h, west.getPreferredSize().height);
        if (east != null) h = Math.max(h, east.getPreferredSize().height);
        if (center != null) h = Math.max(h, WidthLayout.heightOf(center, cw));
        return h;
    }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int width = Math.max(1, parent.getWidth() - in.left - in.right);
        int height = heightFor(parent, width);
        Component west = part(parent, WEST);
        Component center = part(parent, CENTER);
        Component east = part(parent, EAST);
        int x = in.left;
        if (west != null) {
            Dimension d = west.getPreferredSize();
            west.setBounds(x, in.top + offset(height, d.height), d.width, d.height);
            x += d.width + gap;
        }
        int cw = centerWidth(parent, width);
        if (center != null) {
            int ch = WidthLayout.heightOf(center, cw);
            center.setBounds(x, in.top + offset(height, ch), cw, ch);
        }
        if (east != null) {
            Dimension d = east.getPreferredSize();
            east.setBounds(in.left + width - d.width, in.top + offset(height, d.height), d.width, d.height);
        }
    }

    private int offset(int rowHeight, int childHeight) {
        return alignTop ? 0 : Math.max(0, (rowHeight - childHeight) / 2);
    }

    @Override public void addLayoutComponent(Component comp, Object constraints) {
        parts.put(comp, constraints == null ? CENTER : constraints.toString());
    }

    @Override public void removeLayoutComponent(Component comp) { parts.remove(comp); }

    @Override public Dimension preferredLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        int w = parent.getWidth() > 0 ? parent.getWidth() : 380;
        return new Dimension(w, in.top + in.bottom + heightFor(parent, Math.max(1, w - in.left - in.right)));
    }

    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(40, 20); }
    @Override public Dimension maximumLayoutSize(Container target) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    @Override public void addLayoutComponent(String name, Component comp) { parts.put(comp, name); }
    @Override public float getLayoutAlignmentX(Container target) { return 0; }
    @Override public float getLayoutAlignmentY(Container target) { return 0; }
    @Override public void invalidateLayout(Container target) {}
}
