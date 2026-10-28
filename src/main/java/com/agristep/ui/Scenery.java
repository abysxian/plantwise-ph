package com.agristep.ui;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The photos behind banners and the sign-in screen. A photo is cropped to fill the space, slowly drifts when motion
 * is on, and gets a dark green wash on the text side so white text stays readable.
 */
final class Scenery {
    private Scenery() {}

    /** The bundled photos (in src/main/resources/images). {@code focus} is how far down the picture to look (0 to 1). */
    enum Photo {
        HILLS("hills", 0.62f),
        MOUNTAINS("mountains", 0.45f),
        WET_LEAVES("wet-leaves", 0.45f),
        MONSTERA("monstera", 0.50f),
        PHILODENDRON("philodendron", 0.36f),
        HOUSEPLANTS("houseplants", 0.36f);

        final String file;
        final float focus;

        Photo(String file, float focus) {
            this.file = file;
            this.focus = focus;
        }
    }

    enum Sky { CLEAR, CLOUDY, RAIN }

    private static final Map<Photo, BufferedImage> SOURCES = new HashMap<>();
    private static final Map<String, BufferedImage> SCALED = new HashMap<>();

    /** Picks the weather picture that matches a WMO weather code from the forecast. */
    static Photo photoFor(int code) {
        return switch (skyFor(code)) {
            case CLEAR -> Photo.HILLS;
            case CLOUDY -> Photo.MOUNTAINS;
            case RAIN -> Photo.WET_LEAVES;
        };
    }

    static Sky skyFor(int code) {
        if (code == 0 || code == 1) return Sky.CLEAR;
        if (code == 2 || code == 3 || code == 45 || code == 48) return Sky.CLOUDY;
        return Sky.RAIN;
    }

    private static synchronized BufferedImage source(Photo p) {
        return SOURCES.computeIfAbsent(p, k -> {
            try (InputStream in = Scenery.class.getResourceAsStream("/images/" + k.file + ".jpg")) {
                return in == null ? null : ImageIO.read(in);
            } catch (IOException e) {
                return null;
            }
        });
    }

    /** The photo scaled once (in steps, so it stays smooth) to cover w x h; kept for the next paint. */
    private static synchronized BufferedImage cover(Photo p, int w, int h) {
        String key = p.file + ":" + w + "x" + h;
        BufferedImage hit = SCALED.get(key);
        if (hit != null) return hit;
        BufferedImage src = source(p);
        if (src == null) return null;
        double scale = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
        int sw = Math.max(w, (int) Math.ceil(src.getWidth() * scale));
        int sh = Math.max(h, (int) Math.ceil(src.getHeight() * scale));
        BufferedImage cur = src;
        while (cur.getWidth() / 2 >= sw && cur.getHeight() / 2 >= sh) cur = half(cur);
        BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(cur, 0, 0, sw, sh, null);
        g.dispose();
        if (SCALED.size() > 24) SCALED.clear();
        SCALED.put(key, out);
        return out;
    }

    private static BufferedImage half(BufferedImage in) {
        BufferedImage out = new BufferedImage(in.getWidth() / 2, in.getHeight() / 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(in, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    /**
     * Draws the photo to fill (0, 0, w, h), looking at its focus point. With motion on it glides slowly side to side;
     * the position is a fraction of a pixel (not rounded) so the movement is smooth instead of jumping.
     */
    private static void drawPhoto(Graphics2D g, Photo p, int w, int h, float t) {
        int pw = Math.round(w * 1.08f);     // extra size so the glide never shows an edge
        int ph = Math.round(h * 1.08f);
        BufferedImage img = cover(p, pw, ph);
        if (img == null) {
            g.setPaint(new GradientPaint(0, 0, Theme.LEAF, w, h, Theme.CANOPY_2));
            g.fillRect(0, 0, w, h);
            return;
        }
        double drift = 0.5 + 0.5 * Math.sin(t * 0.09);             // 0..1, eases at both ends
        double sway = Math.sin(t * 0.06 + 1.0) * 3.0;               // a few pixels up and down
        double x = -(img.getWidth() - w) * drift;
        double y = -Math.max(0, img.getHeight() - h) * p.focus + sway;
        y = Math.max(-(img.getHeight() - h), Math.min(0, y));
        Object oldInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, java.awt.geom.AffineTransform.getTranslateInstance(x, y), null);
        if (oldInterp != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
    }

    /** Fills the rounded banner (0, 0, w, h) with a photo, a green wash for text, and rain when it is raining. */
    static void paintBanner(Graphics2D g, Photo photo, Sky sky, int w, int h, int arc, float t) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        java.awt.Shape old = g.getClip();
        g.clip(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
        drawPhoto(g, photo, w, h, t);
        int strong = w < 520 ? 185 : 175;
        g.setPaint(new GradientPaint(0, 0, new Color(8, 44, 32, strong), w * 0.85f, 0, new Color(8, 44, 32, 70)));
        g.fillRect(0, 0, w, h);
        g.setPaint(new GradientPaint(0, h * 0.55f, new Color(8, 44, 32, 0), 0, h, new Color(8, 44, 32, 110)));
        g.fillRect(0, 0, w, h);
        if (sky == Sky.RAIN) rain(g, w, h, t);
        g.setClip(old);
    }

    private static void rain(Graphics2D g, int w, int h, float t) {
        g.setStroke(new java.awt.BasicStroke(1.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(new Color(215, 238, 250, 110));
        int drops = Math.max(14, w / 24);
        for (int i = 0; i < drops; i++) {
            float x = (i * 97 % Math.max(1, w)) + ((i * 31) % 17);
            float y = ((i * 53 % Math.max(1, h)) + t * 150f) % Math.max(1, h);
            g.draw(new java.awt.geom.Line2D.Float(x, y, x - 4f, y + 11f));
        }
    }

    /** The photo behind the sign-in card, lightened so the card and its text stay clear. */
    static void paintBackdrop(Graphics2D g, Photo photo, int w, int h, float t) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPhoto(g, photo, w, h, t);
        g.setPaint(new GradientPaint(0, 0, new Color(8, 44, 32, 175), 0, h * 0.35f, new Color(8, 44, 32, 60)));
        g.fillRect(0, 0, w, h);
    }

    /** A leaf shape from its base to its tip, bulging by {@code bulge} on both sides (used for page decoration). */
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
}
