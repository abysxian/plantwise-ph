package com.agristep.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A thin horizontal progress bar, 0 to 100. */
final class Meter extends JComponent {
    private final int value;
    private final Color color;
    private final Motion.Tween grow = new Motion.Tween(this, 700);
    private boolean started;

    Meter(int value, Color color) {
        this.value = Math.max(0, Math.min(100, value));
        this.color = color;
        setOpaque(false);
    }

    int getValue() { return value; }

    @Override public Dimension getPreferredSize() { return new Dimension(60, 10); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        g2.setColor(Theme.LINE);
        g2.fillRoundRect(0, 0, getWidth(), h, h, h);
        if (!started) {
            started = true;
            if (Motion.enabled() && isShowing()) {
                grow.jump(0f);
                grow.to(1f);
            } else {
                grow.jump(1f);
            }
        }
        int w = Math.round(getWidth() * value / 100f * grow.value());
        if (w > 0) {
            g2.setPaint(new java.awt.GradientPaint(0, 0, color.darker(), Math.max(w, h), 0, color));
            g2.fillRoundRect(0, 0, Math.max(w, h), h, h, h);
        }
        g2.dispose();
    }
}
