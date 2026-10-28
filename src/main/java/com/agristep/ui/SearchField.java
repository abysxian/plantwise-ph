package com.agristep.ui;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A text box with grey hint text while empty. */
final class SearchField extends JTextField {
    private final String hint;

    String hint() { return hint; }

    SearchField(String hint) {
        this.hint = I18n.t(hint);
        setFont(Theme.BODY);
        setForeground(Theme.INK);
        setCaretColor(Theme.INK);
        setBackground(new java.awt.Color(0xF7FAF8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0xC9D6CD)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
    }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 40));
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && !hint.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.MUTED);
            g2.setFont(getFont());
            int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(hint, getInsets().left, y);
            g2.dispose();
        }
    }
}
