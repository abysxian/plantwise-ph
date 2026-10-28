package com.agristep.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

/**
 * Two children. On wide windows they sit side by side (main on the left, a fixed-width side panel on the right);
 * on narrow windows they stack, which is the phone layout.
 */
final class SplitLayout implements LayoutManager2, WidthLayout {
    private final int breakpoint;
    private final int sideWidth;
    private final int gap;

    SplitLayout(int breakpoint, int sideWidth, int gap) {
        this.breakpoint = breakpoint;
        this.sideWidth = sideWidth;
        this.gap = gap;
    }

    private boolean sideBySide(int width) { return width >= breakpoint; }

    private Component[] two(Container parent) {
        Component[] all = parent.getComponents();
        return all.length >= 2 ? new Component[] {all[0], all[1]} : all;
    }

    @Override public int heightFor(Container parent, int width) {
        Component[] c = two(parent);
        if (c.length < 2) return c.length == 1 ? WidthLayout.heightOf(c[0], width) : 0;
        if (sideBySide(width)) {
            return Math.max(WidthLayout.heightOf(c[0], width - sideWidth - gap), WidthLayout.heightOf(c[1], sideWidth));
        }
        int a = WidthLayout.heightOf(c[0], width);
        int b = WidthLayout.heightOf(c[1], width);
        return a + (a > 0 && b > 0 ? gap : 0) + b;
    }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int width = Math.max(1, parent.getWidth() - in.left - in.right);
        Component[] c = two(parent);
        if (c.length < 2) {
            if (c.length == 1) c[0].setBounds(in.left, in.top, width, WidthLayout.heightOf(c[0], width));
            return;
        }
        if (sideBySide(width)) {
            int mainW = width - sideWidth - gap;
            c[0].setBounds(in.left, in.top, mainW, WidthLayout.heightOf(c[0], mainW));
            c[1].setBounds(in.left + mainW + gap, in.top, sideWidth, WidthLayout.heightOf(c[1], sideWidth));
        } else {
            int a = WidthLayout.heightOf(c[0], width);
            int b = WidthLayout.heightOf(c[1], width);
            c[0].setBounds(in.left, in.top, width, a);
            c[1].setBounds(in.left, in.top + a + (a > 0 && b > 0 ? gap : 0), width, b);
        }
    }

    @Override public Dimension preferredLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        int w = parent.getWidth() > 0 ? parent.getWidth() : 380;
        return new Dimension(w, in.top + in.bottom + heightFor(parent, Math.max(1, w - in.left - in.right)));
    }

    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(40, 40); }
    @Override public Dimension maximumLayoutSize(Container target) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    @Override public void addLayoutComponent(Component comp, Object constraints) {}
    @Override public void addLayoutComponent(String name, Component comp) {}
    @Override public void removeLayoutComponent(Component comp) {}
    @Override public float getLayoutAlignmentX(Container target) { return 0; }
    @Override public float getLayoutAlignmentY(Container target) { return 0; }
    @Override public void invalidateLayout(Container target) {}
}
