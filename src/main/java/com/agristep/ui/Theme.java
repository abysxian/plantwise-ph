package com.agristep.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** The look of the app: deep canopy green, fresh leaf green, one calamansi-yellow accent, on a cool mist background. */
final class Theme {
    private Theme() {}

    static final Color PAPER = new Color(0xF2F6F3);
    static final Color WHITE = new Color(0xFFFFFF);
    static final Color INK = new Color(0x10241B);
    static final Color MUTED = new Color(0x58695F);
    static final Color LINE = new Color(0xDCE5DF);
    static final Color LEAF = new Color(0x0E3B2C);
    static final Color LEAF_2 = new Color(0x1F9D5B);
    static final Color SOIL = new Color(0x4A3426);
    static final Color SOIL_2 = new Color(0x7A5238);
    static final Color LIME = new Color(0xF2C94C);
    static final Color SILI = new Color(0xC23A2B);
    static final Color RAIN = new Color(0x2B7FB0);
    /** Soft tints used for fills. */
    static final Color LEAF_SOFT = new Color(0xE0F1E7);
    static final Color LIME_SOFT = new Color(0xFFF3CC);
    static final Color SILI_SOFT = new Color(0xF6DDD8);
    static final Color RAIN_SOFT = new Color(0xDDEEF8);

    static final Color CANOPY_2 = new Color(0x16704C);

    /** Blends two colours: 0 gives {@code a}, 1 gives {@code b}. */
    static Color mix(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    private static final String FAMILY = pickFamily();

    private static String pickFamily() {
        Set<String> have = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String name : new String[] {"Segoe UI", "Helvetica Neue", "Noto Sans", "DejaVu Sans"}) {
            if (have.contains(name)) return name;
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    static final Font H1 = font(Font.BOLD, 26);
    static final Font H2 = font(Font.BOLD, 19);
    static final Font H3 = font(Font.BOLD, 15);
    static final Font BODY = font(Font.PLAIN, 14);
    static final Font BODY_BOLD = font(Font.BOLD, 14);
    static final Font SMALL = font(Font.PLAIN, 12);
    static final Font SMALL_BOLD = font(Font.BOLD, 12);
    static final Font BIG_NUMBER = font(Font.BOLD, 38);
}
