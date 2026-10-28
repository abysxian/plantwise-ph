package com.agristep.ui;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emoji drawn from bundled Twemoji PNG files (CC-BY 4.0, https://github.com/jdecked/twemoji).
 * Swing cannot draw colour emoji reliably, so the app ships its own pictures instead of relying on fonts.
 */
final class Emoji {
    private Emoji() {}

    private static final Map<String, BufferedImage> RAW = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> SCALED = new ConcurrentHashMap<>();
    private static final BufferedImage MISSING = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /** File name for an emoji: its code points in hex joined by "-", without the variation selector. */
    static String fileName(String emoji) {
        StringBuilder sb = new StringBuilder();
        emoji.codePoints().filter(cp -> cp != 0xFE0F).forEach(cp -> {
            if (sb.length() > 0) sb.append('-');
            sb.append(Integer.toHexString(cp));
        });
        return sb + ".png";
    }

    /** The original 72 px picture, or null if the emoji is not bundled. */
    static BufferedImage image(String emoji) {
        BufferedImage img = RAW.computeIfAbsent(emoji, e -> {
            try (InputStream in = Emoji.class.getResourceAsStream("/emoji/" + fileName(e))) {
                if (in == null) return MISSING;
                BufferedImage read = ImageIO.read(in);
                return read == null ? MISSING : read;
            } catch (IOException ex) {
                return MISSING;
            }
        });
        return img == MISSING ? null : img;
    }

    /** A square icon that is px wide in layout units and stays sharp on high-resolution screens. */
    static Icon icon(String emoji, int px) {
        BufferedImage source = image(emoji);
        BufferedImage prepared = source == null ? null
                : SCALED.computeIfAbsent(emoji + "@" + px, k -> downscale(source, Math.min(source.getWidth(), px * 2)));
        return new EmojiIcon(prepared, px);
    }

    static JLabel label(String emoji, int px) {
        JLabel label = new JLabel(icon(emoji, px));
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }

    /** Halves the picture step by step so shrinking stays smooth. */
    private static BufferedImage downscale(BufferedImage src, int target) {
        BufferedImage current = src;
        while (current.getWidth() > target) {
            int next = Math.max(target, current.getWidth() / 2);
            BufferedImage out = new BufferedImage(next, next, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(current, 0, 0, next, next, null);
            g.dispose();
            current = out;
        }
        return current;
    }

    private static final class EmojiIcon implements Icon {
        private final BufferedImage image;
        private final int size;

        EmojiIcon(BufferedImage image, int size) {
            this.image = image;
            this.size = size;
        }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, x, y, size, size, null);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
