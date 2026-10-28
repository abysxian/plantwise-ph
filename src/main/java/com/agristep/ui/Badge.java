package com.agristep.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A small coloured pill with a short text, e.g. "Great fit". */
final class Badge extends JComponent {
    private String text;
    private Color background;

    Badge(String text, Color foreground, Color background) {
        this.text = I18n.t(text);
        this.background = background;
        setForeground(foreground);
        setFont(Theme.SMALL_BOLD);
        setOpaque(false);
    }

    void set(String text, Color foreground, Color background) {
        this.text = I18n.t(text);
        this.background = background;
        setForeground(foreground);
        revalidate();
        repaint();
    }

    String getText() { return text; }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text) + 20, fm.getHeight() + 8);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
