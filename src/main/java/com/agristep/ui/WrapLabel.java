package com.agristep.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * A text label that wraps to whatever width it is given and reports the height that needs.
 * (JLabel cannot wrap, and HTML labels do not report their height reliably, so this draws the text itself.)
 */
final class WrapLabel extends JComponent {
    static final int LEFT = 0;
    static final int CENTER = 1;
    static final int RIGHT = 2;

    private String text;
    private int alignment = LEFT;
    private int maxLines = Integer.MAX_VALUE;
    private boolean strike;

    WrapLabel(String text, Font font, Color color) {
        this.text = text == null ? "" : text;
        setFont(font);
        setForeground(color);
        setOpaque(false);
    }

    static WrapLabel body(String text) { return new WrapLabel(text, Theme.BODY, Theme.INK); }
    static WrapLabel muted(String text) { return new WrapLabel(text, Theme.BODY, Theme.MUTED); }
    static WrapLabel small(String text) { return new WrapLabel(text, Theme.SMALL, Theme.MUTED); }
    static WrapLabel bold(String text) { return new WrapLabel(text, Theme.BODY_BOLD, Theme.INK); }
    static WrapLabel h1(String text) { return new WrapLabel(text, Theme.H1, Theme.INK); }
    static WrapLabel h2(String text) { return new WrapLabel(text, Theme.H2, Theme.INK); }
    static WrapLabel h3(String text) { return new WrapLabel(text, Theme.H3, Theme.INK); }

    WrapLabel align(int alignment) {
        this.alignment = alignment;
        return this;
    }

    WrapLabel maxLines(int maxLines) {
        this.maxLines = maxLines;
        return this;
    }

    WrapLabel color(Color color) {
        setForeground(color);
        return this;
    }

    WrapLabel strike(boolean strike) {
        this.strike = strike;
        return this;
    }

    String getText() { return text; }

    void setText(String text) {
        this.text = text == null ? "" : text;
        revalidate();
        repaint();
    }

    private int lineHeight(FontMetrics fm) { return fm.getHeight() + 2; }

    /** Breaks the text into lines no wider than {@code width}. */
    List<String> lines(int width) {
        FontMetrics fm = getFontMetrics(getFont());
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            wrapParagraph(paragraph, fm, Math.max(1, width), out);
        }
        if (out.size() > maxLines) {
            List<String> cut = new ArrayList<>(out.subList(0, maxLines));
            cut.set(maxLines - 1, ellipsize(cut.get(maxLines - 1) + " …", fm, width));
            return cut;
        }
        return out;
    }

    private static void wrapParagraph(String paragraph, FontMetrics fm, int width, List<String> out) {
        if (paragraph.isEmpty()) {
            out.add("");
            return;
        }
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
            }
            String rest = word;
            while (fm.stringWidth(rest) > width && rest.length() > 1) {
                int cut = 1;
                while (cut < rest.length() && fm.stringWidth(rest.substring(0, cut + 1)) <= width) cut++;
                out.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        out.add(line.toString());
    }

    private static String ellipsize(String s, FontMetrics fm, int width) {
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t) > width) {
            t = t.substring(0, t.length() - 2) + "…";
        }
        return t;
    }

    int heightFor(int width) {
        if (!isVisible()) return 0;
        return lines(width).size() * lineHeight(getFontMetrics(getFont()));
    }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int widest = 0;
        String[] parts = text.split("\n", -1);
        for (String part : parts) widest = Math.max(widest, fm.stringWidth(part));
        return new Dimension(widest + 1, parts.length * lineHeight(fm));
    }

    @Override public Dimension getMinimumSize() { return new Dimension(10, lineHeight(getFontMetrics(getFont()))); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int lh = lineHeight(fm);
        int y = fm.getAscent() + 1;
        for (String line : lines(getWidth())) {
            int w = fm.stringWidth(line);
            int x = switch (alignment) {
                case CENTER -> (getWidth() - w) / 2;
                case RIGHT -> getWidth() - w;
                default -> 0;
            };
            g2.drawString(line, x, y);
            if (strike && !line.isEmpty()) g2.drawLine(x, y - fm.getAscent() / 3, x + w, y - fm.getAscent() / 3);
            y += lh;
        }
        g2.dispose();
    }
}
