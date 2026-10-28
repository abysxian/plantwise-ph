package com.agristep.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

/**
 * A grid that picks its own column count from the width: as many columns as fit {@code minColumnWidth},
 * up to {@code maxColumns}. One column on a phone, several on a desktop. With {@code aspect > 0} every
 * cell has height = width x aspect (used for the garden beds); otherwise each row is as tall as its tallest cell.
 */
final class FlowGridLayout implements LayoutManager2, WidthLayout {
    private final int minColumnWidth;
    private final int maxColumns;
    private final int gap;
    private final double aspect;

    FlowGridLayout(int minColumnWidth, int maxColumns, int gap, double aspect) {
        this.minColumnWidth = minColumnWidth;
        this.maxColumns = maxColumns;
        this.gap = gap;
        this.aspect = aspect;
    }

    int columnsFor(int width) {
        int fit = (width + gap) / (minColumnWidth + gap);
        return Math.max(1, Math.min(maxColumns, fit));
    }

    @Override public int heightFor(Container parent, int width) {
        int cols = columnsFor(width);
        int cellW = (width - gap * (cols - 1)) / cols;
        int total = 0;
        int rows = 0;
        int rowMax = 0;
        int inRow = 0;
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) continue;
            int h = aspect > 0 ? (int) Math.round(cellW * aspect) : WidthLayout.heightOf(c, cellW);
            rowMax = Math.max(rowMax, h);
            if (++inRow == cols) {
                total += rowMax;
                rows++;
                rowMax = 0;
                inRow = 0;
            }
        }
        if (inRow > 0) {
            total += rowMax;
            rows++;
        }
        return total + Math.max(0, rows - 1) * gap;
    }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int width = Math.max(1, parent.getWidth() - in.left - in.right);
        int cols = columnsFor(width);
        int cellW = (width - gap * (cols - 1)) / cols;
        int y = in.top;
        int col = 0;
        int rowMax = 0;
        java.util.List<Component> row = new java.util.ArrayList<>();
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) continue;
            int h = aspect > 0 ? (int) Math.round(cellW * aspect) : WidthLayout.heightOf(c, cellW);
            rowMax = Math.max(rowMax, h);
            c.setBounds(in.left + col * (cellW + gap), y, cellW, h);
            row.add(c);
            if (++col == cols) {
                stretch(row, rowMax);
                y += rowMax + gap;
                col = 0;
                rowMax = 0;
                row.clear();
            }
        }
        stretch(row, rowMax);
    }

    /** Makes every cell in a row as tall as the tallest, so neighbouring cards line up. */
    private void stretch(java.util.List<Component> row, int height) {
        for (Component c : row) c.setSize(c.getWidth(), height);
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
