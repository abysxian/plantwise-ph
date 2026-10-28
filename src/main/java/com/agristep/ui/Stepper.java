package com.agristep.ui;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.function.IntConsumer;

/** A number with minus and plus buttons, for small whole numbers such as rows and columns. */
final class Stepper extends JPanel {
    private final int min;
    private final int max;
    private final WrapLabel valueLabel = new WrapLabel("", Theme.H2, Theme.INK);
    private final PillButton minus = PillButton.secondary("−");
    private final PillButton plus = PillButton.secondary("+");
    private int value;
    private IntConsumer onChange = v -> { };

    Stepper(String name, int value, int min, int max) {
        super(new RowLayout(8, false));
        this.min = min;
        this.max = max;
        setOpaque(false);
        minus.getAccessibleContext().setAccessibleName("Fewer " + name);
        plus.getAccessibleContext().setAccessibleName("More " + name);
        minus.addActionListener(e -> set(this.value - 1));
        plus.addActionListener(e -> set(this.value + 1));
        valueLabel.align(WrapLabel.CENTER);
        add(minus, RowLayout.WEST);
        add(valueLabel, RowLayout.CENTER);
        add(plus, RowLayout.EAST);
        this.value = clamp(value);
        refresh();
    }

    int value() { return value; }

    void onChange(IntConsumer listener) { this.onChange = listener; }

    void set(int next) {
        int v = clamp(next);
        if (v == value) return;
        value = v;
        refresh();
        onChange.accept(v);
    }

    /** Sets the number without telling the listener (used when a preset fills both steppers). */
    void setQuietly(int next) {
        value = clamp(next);
        refresh();
    }

    private int clamp(int v) { return Math.max(min, Math.min(max, v)); }

    private void refresh() {
        valueLabel.setText(String.valueOf(value));
        minus.setEnabled(value > min);
        plus.setEnabled(value < max);
        repaint();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(150, 40); }
}
