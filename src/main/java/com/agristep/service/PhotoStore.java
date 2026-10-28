package com.agristep.service;

import com.agristep.domain.ValidationException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Keeps journal photos in a folder: each picture is shrunk to a sensible size and saved as a JPEG. */
public class PhotoStore {
    static final int MAX_SIDE = 900;
    static final long MAX_SOURCE_BYTES = 25L * 1024 * 1024;

    private final Path folder;

    public PhotoStore(Path folder) {
        this.folder = folder;
    }

    /** Copies the picture in and returns the name it is stored under. */
    public String save(Path source) {
        try {
            if (!Files.isRegularFile(source)) throw new ValidationException("That photo could not be found.");
            if (Files.size(source) > MAX_SOURCE_BYTES) throw new ValidationException("That photo is too big (25 MB at most).");
            BufferedImage image = ImageIO.read(source.toFile());
            if (image == null) throw new ValidationException("That file is not a picture the app can read (use JPG, PNG or GIF).");
            double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(image.getWidth(), image.getHeight()));
            int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.drawImage(image.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
            Files.createDirectories(folder);
            String name = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            if (!ImageIO.write(out, "jpg", folder.resolve(name).toFile())) throw new IOException("No JPEG writer is available.");
            return name;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the photo: " + e.getMessage(), e);
        }
    }

    public Path pathOf(String name) { return folder.resolve(safe(name)); }

    public void delete(String name) {
        try {
            Files.deleteIfExists(folder.resolve(safe(name)));
        } catch (IOException ignored) {
            // a leftover picture is harmless
        }
    }

    private static String safe(String name) {
        return Path.of(name).getFileName().toString();      // never walk out of the photo folder
    }
}
