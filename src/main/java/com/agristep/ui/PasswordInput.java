package com.agristep.ui;

import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A password box in the app's style. */
final class PasswordInput extends JPasswordField {
    private static final char HIDDEN = '•';
    private final String hint;

    String hint() { return hint; }

    PasswordInput(String hint) {
        this.hint = I18n.t(hint);
        setFont(Theme.BODY);
        setForeground(Theme.INK);
        setCaretColor(Theme.INK);
        setBackground(Theme.WHITE);
        setEchoChar(HIDDEN);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
    }

    void setShown(boolean shown) { setEchoChar(shown ? (char) 0 : HIDDEN); }

    /** Reads the typed password and wipes the field's copy of it. */
    String takeText() {
        char[] chars = getPassword();
        String text = new String(chars);
        java.util.Arrays.fill(chars, '\0');
        return text;
    }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 40));
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getPassword().length == 0 && !hint.isEmpty()) {
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
