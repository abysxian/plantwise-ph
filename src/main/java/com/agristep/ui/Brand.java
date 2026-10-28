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
 * The app's logo. Put your own picture at {@code src/main/resources/brand/logo.png} (a square PNG with a
 * transparent background works best) and it appears in the menu, the sign-in screen and the window icon.
 * Until that file exists the app simply shows its name.
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

    /** The logo as a square image for the window icon, or null when there is no logo file yet. */
    static BufferedImage icon(int size) {
        BufferedImage src = logo();
        if (src == null) return null;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    /** The logo as a component of the given height; takes no space when there is no logo file yet. */
    static JComponent mark(int height) {
        return new JComponent() {
            { setOpaque(false); }

            @Override public Dimension getPreferredSize() {
                BufferedImage src = logo();
                if (src == null) return new Dimension(0, 0);
                return new Dimension(Math.round(height * src.getWidth() / (float) src.getHeight()), height);
            }

            @Override protected void paintComponent(Graphics g) {
                BufferedImage src = logo();
                if (src == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                Dimension d = getPreferredSize();
                g2.drawImage(src, (getWidth() - d.width) / 2, (getHeight() - d.height) / 2, d.width, d.height, null);
                g2.dispose();
            }
        };
    }
}
