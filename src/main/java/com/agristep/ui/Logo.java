package com.agristep.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The PlantWise PH logo, drawn in code so it stays sharp at every size: a sprout rising from the soil
 * under a calamansi sun, on a deep green badge. (A matching SVG is in the project's assets folder.)
 */
final class Logo {
    private Logo() {}

    /** Paints the badge inside the square (x, y, size, size). */
    static void paintMark(Graphics2D g, float x, float y, float s) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        java.awt.Shape oldClip = g.getClip();
        RoundRectangle2D badge = new RoundRectangle2D.Float(x, y, s, s, s * 0.30f, s * 0.30f);
        g.setPaint(new GradientPaint(x, y, new Color(0x1B8A5A), x + s, y + s, new Color(0x0A3326)));
        g.fill(badge);
        g.clip(badge);

        // sun, partly hidden behind the hill
        g.setColor(new Color(0xF2, 0xC9, 0x4C, 70));
        g.fill(new Ellipse2D.Float(x + s * 0.40f, y + s * 0.08f, s * 0.56f, s * 0.56f));
        g.setColor(Theme.LIME);
        g.fill(new Ellipse2D.Float(x + s * 0.52f, y + s * 0.20f, s * 0.32f, s * 0.32f));

        // soil mound
        Path2D hill = new Path2D.Float();
        hill.moveTo(x - s * 0.1f, y + s * 1.02f);
        hill.lineTo(x - s * 0.1f, y + s * 0.80f);
        hill.curveTo(x + s * 0.25f, y + s * 0.66f, x + s * 0.75f, y + s * 0.66f, x + s * 1.1f, y + s * 0.82f);
        hill.lineTo(x + s * 1.1f, y + s * 1.02f);
        hill.closePath();
        g.setPaint(new GradientPaint(x, y + s * 0.66f, new Color(0x7A5238), x, y + s, new Color(0x4A3426)));
        g.fill(hill);

        // sprout: stem and two leaves
        float cx = x + s * 0.44f;
        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(s * 0.065f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        Path2D stem = new Path2D.Float();
        stem.moveTo(cx, y + s * 0.74f);
        stem.curveTo(cx, y + s * 0.62f, cx + s * 0.02f, y + s * 0.54f, cx + s * 0.03f, y + s * 0.46f);
        g.draw(stem);
        g.setColor(new Color(0xC8F3D6));
        g.fill(leaf(cx + s * 0.02f, y + s * 0.55f, cx - s * 0.26f, y + s * 0.30f, s * 0.12f));
        g.setColor(Color.WHITE);
        g.fill(leaf(cx + s * 0.03f, y + s * 0.47f, cx + s * 0.27f, y + s * 0.24f, s * 0.13f));
        g.setClip(oldClip);
    }

    /** A leaf shape from its base to its tip, bulging by {@code bulge} on both sides. */
    static Path2D leaf(float bx, float by, float tx, float ty, float bulge) {
        float mx = (bx + tx) / 2f;
        float my = (by + ty) / 2f;
        float dx = tx - bx;
        float dy = ty - by;
        float len = (float) Math.hypot(dx, dy);
        float nx = -dy / len * bulge;
        float ny = dx / len * bulge;
        Path2D p = new Path2D.Float();
        p.moveTo(bx, by);
        p.quadTo(mx + nx, my + ny, tx, ty);
        p.quadTo(mx - nx, my - ny, bx, by);
        p.closePath();
        return p;
    }

    /** The badge as a picture, e.g. for the window icon. */
    static BufferedImage image(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paintMark(g, 0, 0, size);
        g.dispose();
        return img;
    }

    /** Just the badge, as a component. */
    static JComponent mark(int size) {
        return new JComponent() {
            { setOpaque(false); }
            @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                paintMark(g2, (getWidth() - size) / 2f, (getHeight() - size) / 2f, size);
                g2.dispose();
            }
        };
    }

    /** The badge with the name beside it: "PlantWise" and a calamansi "PH". */
    static JComponent wordmark(int badge, Color nameColor) {
        return new JComponent() {
            { setOpaque(false); }
            @Override public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(Theme.font(java.awt.Font.BOLD, Math.round(badge * 0.50f)));
                return new Dimension(badge + 10 + fm.stringWidth("PlantWise PH"), badge);
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintMark(g2, 0, 0, badge);
                g2.setFont(Theme.font(java.awt.Font.BOLD, Math.round(badge * 0.50f)));
                FontMetrics fm = g2.getFontMetrics();
                int y = (badge - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(nameColor);
                g2.drawString("PlantWise", badge + 10, y);
                g2.setColor(Theme.LIME);
                g2.drawString(" PH", badge + 10 + fm.stringWidth("PlantWise"), y);
                g2.dispose();
            }
        };
    }

}
