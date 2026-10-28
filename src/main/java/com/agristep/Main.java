package com.agristep;

import com.agristep.config.DatabaseConfig;
import com.agristep.config.RememberedLogin;
import com.agristep.ui.AppController;
import com.agristep.ui.MainFrame;
import com.agristep.weather.OpenMeteoClient;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.time.Clock;
import java.util.concurrent.ExecutionException;

/**
 * Starts PlantWise PH. Run this class from NetBeans, VS Code, or the command line.
 * The window opens at once; the database and starter data load in the background.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // the app paints its own controls, so the default look is fine
            }
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            start(frame);
        });
    }

    private record Started(AppContext ctx, RememberedLogin remembered, boolean signedIn, String dataNote) {}

    private static void start(MainFrame frame) {
        frame.showLoading("Opening your garden…");
        new SwingWorker<Started, Void>() {
            private DatabaseConfig config;

            @Override protected Started doInBackground() throws Exception {
                config = DatabaseConfig.load();
                Clock clock = Clock.systemDefaultZone();
                AppContext ctx = AppContext.create(config.openDatabase(), new OpenMeteoClient(clock), clock);
                ctx.seed();
                RememberedLogin remembered = RememberedLogin.atDefaultLocation();
                boolean signedIn = remembered.read().flatMap(ctx.auth::signInRemembered).isPresent();
                return new Started(ctx, remembered, signedIn, config.describe());
            }

            @Override protected void done() {
                try {
                    Started s = get();
                    new AppController(frame, s.ctx(), s.remembered(), s.dataNote()).start(s.signedIn());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    frame.showError(explain(e.getCause() == null ? e : e.getCause(), config), () -> start(frame));
                }
            }
        }.execute();
    }

    /** Turns a start-up failure into one plain sentence that says what to do. */
    static String explain(Throwable error, DatabaseConfig config) {
        StringBuilder all = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            all.append(t.getMessage()).append(' ');
        }
        String text = all.toString();
        if (error instanceof IllegalArgumentException) {
            return error.getMessage();
        }
        if (config != null && config.isMySql()) {
            return "PlantWise couldn't connect to MySQL. Start MySQL in the XAMPP Control Panel and press Try again. "
                    + "If you don't use MySQL, open agristep.properties and change db.type to h2.\n\nDetails: "
                    + firstLine(text);
        }
        if (text.contains("already in use") || text.contains("Locked by another process")) {
            return "PlantWise is already open in another window, so its database is in use. Close the other "
                    + "window and press Try again.";
        }
        return "Something went wrong while opening the database.\n\nDetails: " + firstLine(text);
    }

    private static String firstLine(String text) {
        String trimmed = text.replace("null", "").trim();
        int end = trimmed.indexOf('\n');
        String line = end < 0 ? trimmed : trimmed.substring(0, end);
        return line.length() > 300 ? line.substring(0, 300) + "…" : line;
    }
}
