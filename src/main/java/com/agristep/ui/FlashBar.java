package com.agristep.ui;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/** A message strip across the top of the window. It hides itself after five seconds. */
final class FlashBar extends JPanel {
    private static final int SHOW_MILLIS = 5000;

    private final WrapLabel label = new WrapLabel("", Theme.BODY_BOLD, Theme.INK);
    private final Timer hider = new Timer(SHOW_MILLIS, e -> dismiss());
    private Color background = Theme.LEAF_SOFT;

    FlashBar() {
        setOpaque(false);
        setLayout(new StackLayout(0));
        setBorder(new EmptyBorder(10, 16, 10, 16));
        add(label);
        hider.setRepeats(false);
        setVisible(false);
    }

    void show(String message, boolean error) {
        label.setText(message);
        label.color(error ? Theme.SILI : Theme.LEAF);
        background = error ? Theme.SILI_SOFT : Theme.LEAF_SOFT;
        setVisible(true);
        hider.restart();
        revalidate();
        repaint();
    }

    void dismiss() {
        hider.stop();
        setVisible(false);
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    String message() { return isVisible() ? label.getText() : ""; }

    @Override public Dimension getPreferredSize() {
        int w = getWidth() > 0 ? getWidth() : 380;
        return new Dimension(w, WidthLayout.heightOf(this, w));
    }

    @Override protected void paintComponent(Graphics g) {
        g.setColor(background);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.LINE);
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }
}
