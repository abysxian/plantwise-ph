package com.agristep.ui;

import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;

/** A desktop notification where the system supports one (a speech bubble from the system tray). Never fails. */
final class Notifier {
    private Notifier() {}

    static void desktop(String title, String message) {
        try {
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) return;
            SystemTray tray = SystemTray.getSystemTray();
            TrayIcon icon = new TrayIcon(Brand.hasLogo() ? Brand.icon(32) : new java.awt.image.BufferedImage(16, 16,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB), "PlantWise PH");
            icon.setImageAutoSize(true);
            tray.add(icon);
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            javax.swing.Timer remove = new javax.swing.Timer(15_000, e -> tray.remove(icon));
            remove.setRepeats(false);
            remove.start();
        } catch (Exception | LinkageError ignored) {
            // notifications are optional
        }
    }
}
