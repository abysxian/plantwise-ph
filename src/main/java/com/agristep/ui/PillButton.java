package com.agristep.ui;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A rounded button in the app's style. */
final class PillButton extends JButton {

    enum Style { PRIMARY, SECONDARY, DANGER }

    private final Style style;
    private final Motion.Tween glow = new Motion.Tween(this, 140);

    PillButton(String text, Style style) {
        super(I18n.t(text));
        this.style = style;
        setFont(Theme.BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getModel().addChangeListener(e -> glow.to(isEnabled() && (getModel().isRollover() || getModel().isArmed()) ? 1f : 0f));
    }

    static PillButton primary(String text) { return new PillButton(text, Style.PRIMARY); }
    static PillButton secondary(String text) { return new PillButton(text, Style.SECONDARY); }
    static PillButton danger(String text) { return new PillButton(text, Style.DANGER); }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Icon icon = getIcon();
        int w = fm.stringWidth(getText()) + 28 + (icon == null ? 0 : icon.getIconWidth() + 6);
        return new Dimension(Math.max(w, 64), 38);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean on = isEnabled();
        float heat = on ? glow.value() : 0f;
        boolean pressed = on && getModel().isArmed();
        if (pressed) g2.translate(0, 1);
        Color bg;
        Color fg;
        Color line;
        switch (style) {
            case PRIMARY -> {
                bg = Theme.mix(Theme.LEAF, Theme.CANOPY_2, heat);
                fg = Color.WHITE;
                line = bg;
            }
            case DANGER -> {
                bg = Theme.mix(Theme.SILI_SOFT, new Color(0xF2C9C2), heat);
                fg = Theme.SILI;
                line = bg;
            }
            default -> {
                bg = Theme.mix(Theme.LEAF_SOFT, new Color(0xC3E5D2), heat);
                fg = Theme.LEAF;
                line = bg;
            }
        }
        if (!on) {
            bg = Theme.LINE;
            fg = Theme.MUTED;
            line = Theme.LINE;
        }
        int arc = getHeight();
        if (on && style == Style.PRIMARY) {
            g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.mix(Theme.LEAF, Theme.CANOPY_2, heat), getWidth(), 0,
                    Theme.mix(Theme.CANOPY_2, Theme.LEAF_2, heat)));
        } else {
            g2.setColor(bg);
        }
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(line);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (hasFocus()) {
            g2.setColor(Theme.LEAF_2);
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, arc, arc);
        }
        g2.setFont(getFont());
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        Icon icon = getIcon();
        int content = fm.stringWidth(getText()) + (icon == null ? 0 : icon.getIconWidth() + 6);
        int x = (getWidth() - content) / 2;
        if (icon != null) {
            icon.paintIcon(this, g2, x, (getHeight() - icon.getIconHeight()) / 2);
            x += icon.getIconWidth() + 6;
        }
        g2.drawString(getText(), x, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
