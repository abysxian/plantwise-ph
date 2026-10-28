package com.agristep.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LookTest {
    @Test void everyBundledPhotoLoadsAndIsLargeEnoughForABanner() throws Exception {
        for (Scenery.Photo p : Scenery.Photo.values()) {
            try (java.io.InputStream in = LookTest.class.getResourceAsStream("/images/" + p.file + ".jpg")) {
                assertNotNull(in, p.file + " is bundled");
                BufferedImage img = javax.imageio.ImageIO.read(in);
                assertTrue(img.getWidth() >= 1000, p.file + " is wide enough");
            }
        }
    }

    @Test void theLogoIsBundledAndDrawnOnAWhiteCircleForDarkBackgrounds() {
        assertTrue(Brand.hasLogo(), "logo.png is bundled");
        java.awt.Dimension d = Brand.mark(42, true).getPreferredSize();
        assertEquals(42, d.width);
        assertEquals(42, d.height);
        BufferedImage icon = Brand.icon(64);
        assertNotNull(icon);
        assertEquals(0xFFFFFF, icon.getRGB(32, 3) & 0xFFFFFF, "white circle behind the logo");
        assertTrue((icon.getRGB(0, 0) >>> 24) < 128, "corner outside the circle is see-through");
        java.util.Set<Integer> colours = new java.util.HashSet<>();
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) colours.add(icon.getRGB(x, y));
        assertTrue(colours.size() > 30, "the logo artwork is drawn");
    }

    @Test void weatherCodesPickTheMatchingPicture() {
        assertEquals(Scenery.Sky.CLEAR, Scenery.skyFor(0));
        assertEquals(Scenery.Sky.CLEAR, Scenery.skyFor(1));
        assertEquals(Scenery.Sky.CLOUDY, Scenery.skyFor(3));
        assertEquals(Scenery.Sky.CLOUDY, Scenery.skyFor(45));
        assertEquals(Scenery.Sky.RAIN, Scenery.skyFor(63));
        assertEquals(Scenery.Sky.RAIN, Scenery.skyFor(95));
        assertEquals(Scenery.Photo.HILLS, Scenery.photoFor(0));
        assertEquals(Scenery.Photo.MOUNTAINS, Scenery.photoFor(3));
        assertEquals(Scenery.Photo.WET_LEAVES, Scenery.photoFor(63));
    }

    @Test void everyPhotoRendersAsABannerAndABackdropAtPhoneAndDesktopSizes() {
        for (Scenery.Photo photo : Scenery.Photo.values()) {
            for (Scenery.Sky sky : Scenery.Sky.values()) {
                for (int w : new int[] {320, 900}) {
                    BufferedImage img = new BufferedImage(w, 160, BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = img.createGraphics();
                    Scenery.paintBanner(g, photo, sky, w, 160, 22, 3.5f);
                    g.dispose();
                    assertNotEquals(0, img.getRGB(w / 2, 80) >>> 24, photo + "/" + sky + " paints the middle");
                    assertEquals(0, img.getRGB(0, 0) >>> 24, "rounded corner stays clear");
                }
            }
            BufferedImage back = new BufferedImage(390, 700, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = back.createGraphics();
            Scenery.paintBackdrop(g, photo, 390, 700, 0f);
            g.dispose();
            assertNotEquals(0, back.getRGB(195, 350) >>> 24);
        }
    }

    @Test void motionIsOffInTestsSoEverythingShowsItsFinalState() {
        assertFalse(Motion.enabled());
        assertEquals(0f, Motion.seconds());
        javax.swing.JPanel p = new javax.swing.JPanel();
        Motion.Tween t = new Motion.Tween(p, 200);
        t.to(1f);
        assertEquals(1f, t.value());
        t.to(0f);
        assertEquals(0f, t.value());
    }

    @Test void blendingColoursIsLinearAndClamped() {
        assertEquals(Theme.LEAF, Theme.mix(Theme.LEAF, Theme.LIME, 0f));
        assertEquals(Theme.LIME, Theme.mix(Theme.LEAF, Theme.LIME, 1f));
        assertEquals(Theme.LIME, Theme.mix(Theme.LEAF, Theme.LIME, 7f));
    }
}
