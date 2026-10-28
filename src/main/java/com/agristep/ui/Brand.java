package com.agristep.ui;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * The PlantWise PH logo ({@code src/main/resources/brand/logo.png}). It is shown on a white circle where the
 * background is dark (the side menu, the phone top bar, the sign-in photo) and as the window icon.
 * If the file is ever missing the app simply shows its name.
 */
final class Brand {
    private static final String LOGO = "/brand/logo.png";
    private static BufferedImage logo;
    private static boolean loaded;

    private Brand() {}

    private static synchronized BufferedImage logo() {
        if (!loaded) {
            loaded = true;
            try (InputStream in = Brand.class.getResourceAsStream(LOGO)) {
                if (in != null) logo = ImageIO.read(in);
            } catch (IOException e) {
                logo = null;
            }
        }
        return logo;
    }

    static boolean hasLogo() { return logo() != null; }

    /** The logo on a white circle as a square image for the window icon, or null when there is no logo file. */
    static BufferedImage icon(int size) {
        BufferedImage src = logo();
        if (src == null) return null;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        paintBacked(g, src, 0, 0, size);
        g.dispose();
        return out;
    }

    private static void paintBacked(Graphics2D g, BufferedImage src, float x, float y, int size) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE);
        g.fill(new java.awt.geom.Ellipse2D.Float(x, y, size, size));
        int inner = Math.round(size * 0.80f);
        g.drawImage(src, Math.round(x + (size - inner) / 2f), Math.round(y + (size - inner) / 2f), inner, inner, null);
    }

    /** The logo as a square component; takes no space if there is no logo file. */
    static JComponent mark(int size, boolean onWhiteCircle) {
        return new JComponent() {
            { setOpaque(false); }

            @Override public Dimension getPreferredSize() {
                return logo() == null ? new Dimension(0, 0) : new Dimension(size, size);
            }

            @Override protected void paintComponent(Graphics g) {
                BufferedImage src = logo();
                if (src == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                float x = (getWidth() - size) / 2f;
                float y = (getHeight() - size) / 2f;
                if (onWhiteCircle) {
                    paintBacked(g2, src, x, y, size);
                } else {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.drawImage(src, Math.round(x), Math.round(y), size, size, null);
                }
                g2.dispose();
            }
        };
    }
}
