package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.CareTask;
import com.agristep.domain.Plot;
import com.agristep.persistence.Database;
import com.agristep.weather.ClimateStats;
import com.agristep.weather.DailyWeather;
import com.agristep.weather.GeoPlace;
import com.agristep.weather.WeatherForecast;
import com.agristep.weather.WeatherProvider;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Builds a real app (in-memory database, fake weather) and draws screens to images without opening a window. */
final class UiFixture {
    private UiFixture() {}

    private static final AtomicInteger COUNTER = new AtomicInteger();

    /** Weather that never touches the internet. Set {@code offline} to make it fail like a lost connection. */
    static final class FakeWeather implements WeatherProvider {
        boolean offline;

        @Override public WeatherForecast fetchForecast(double latitude, double longitude) {
            if (offline) throw new com.agristep.weather.WeatherUnavailableException("offline", null);
            int[] codes = {2, 61, 80, 0, 3, 1, 95};
            double[] max = {31, 28, 29, 32, 30, 33, 27};
            double[] min = {25, 24, 24, 25, 25, 26, 23};
            double[] rain = {0, 12, 6, 0, 0, 0, 22};
            List<DailyWeather> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                days.add(new DailyWeather(LocalDate.now().plusDays(i), max[i], min[i], rain[i], codes[i]));
            }
            return new WeatherForecast(29.3, 2, 78, days);
        }

        @Override public ClimateStats fetchClimate(double latitude, double longitude) {
            if (offline) throw new com.agristep.weather.WeatherUnavailableException("offline", null);
            return new ClimateStats(27.8, 2300, new double[] {26,26.5,27.5,28.5,29,28.5,28,28,27.8,27.5,27,26.2}, new double[] {60,40,45,60,150,260,380,420,380,250,150,100});
        }

        @Override public List<GeoPlace> searchPlaces(String query) {
            if (offline) throw new com.agristep.weather.WeatherUnavailableException("offline", null);
            return List.of(new GeoPlace("Tagaytay", "Calabarzon", "Philippines", 14.10, 120.96),
                    new GeoPlace("Tagaytay Heights", null, "Philippines", 14.11, 120.95));
        }
    }

    /** A seeded database with the demo account, but nobody signed in (the state at start-up). */
    static AppContext signedOutContext() {
        try {
            Database db = new Database("jdbc:h2:mem:ui" + COUNTER.incrementAndGet() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            AppContext ctx = AppContext.create(db, new FakeWeather(), Clock.systemDefaultZone());
            ctx.seed();
            return ctx;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static AppContext context(FakeWeather weather) {
        try {
            Database db = new Database("jdbc:h2:mem:ui" + COUNTER.incrementAndGet() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            AppContext ctx = AppContext.create(db, weather, Clock.systemDefaultZone());
            ctx.seed();
            ctx.auth.register("tester", "Tester", "secret1", "secret1");
            return ctx;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Plants a few beds at different stages so every state is visible. */
    static void populate(AppContext ctx) {
        List<Plot> plots = ctx.garden.allPlots();
        LocalDate today = ctx.garden.today();
        plant(ctx, plots.get(0), "talong", today.minusDays(20));
        plant(ctx, plots.get(1), "kangkong", today.minusDays(31));
        plant(ctx, plots.get(2), "pechay", today);
        plant(ctx, plots.get(4), "sili", today.minusDays(60));
        // tick the first task of bed 1
        Plot first = ctx.garden.getPlot(plots.get(0).getId());
        CareTask task = first.getTasks().get(0);
        ctx.garden.toggleTask(first.getId(), task.getId());
    }

    static void plant(AppContext ctx, Plot plot, String code, LocalDate on) {
        Long cropId = ctx.crops.findByCode(code).orElseThrow().getId();
        ctx.garden.plant(plot.getId(), cropId, on);
    }

    /** Lays out a component tree even though it was never shown on screen. */
    static void layoutTree(Component c) {
        if (c instanceof Container box) {
            box.doLayout();
            for (Component child : box.getComponents()) layoutTree(child);
        }
    }

    static BufferedImage render(Component c, int width, int height) {
        c.setSize(width, height);
        layoutTree(c);
        layoutTree(c); // second pass: scroll panes size their view after the first
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.printAll(g);
        g.dispose();
        return img;
    }

    // ---- helpers for finding and driving components ----

    interface Body { void run() throws Exception; }

    /** Runs the test body on the screen thread, the way Swing expects, and rethrows any failure. */
    static void edt(Body body) {
        Tasks.synchronous = true;
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    body.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            while (cause instanceof RuntimeException && cause.getCause() != null && !(cause instanceof AssertionError)) {
                cause = cause.getCause();
            }
            if (cause instanceof Error err) throw err;
            throw new IllegalStateException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            Tasks.synchronous = false;
        }
    }

    static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> out = new ArrayList<>();
        collect(root, type, out);
        return out;
    }

    private static <T extends Component> void collect(Container c, Class<T> type, List<T> out) {
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) out.add(type.cast(child));
            if (child instanceof Container box) collect(box, type, out);
        }
    }

    static List<String> texts(Container root) {
        List<String> out = new ArrayList<>();
        all(root, WrapLabel.class).forEach(l -> out.add(l.getText()));
        all(root, javax.swing.JLabel.class).forEach(l -> out.add(l.getText() == null ? "" : l.getText()));
        return out;
    }

    static boolean hasText(Container root, String fragment) {
        return texts(root).stream().anyMatch(t -> t.contains(fragment));
    }

    static PillButton button(Container root, String text) {
        return all(root, PillButton.class).stream().filter(b -> b.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no button \"" + text + "\"; have "
                        + all(root, PillButton.class).stream().map(PillButton::getText).toList()));
    }

    static List<PillButton> buttons(Container root, String text) {
        return all(root, PillButton.class).stream().filter(b -> b.getText().equals(text)).toList();
    }

    /** Presses the mouse on a component by calling its click listeners, as a real click would. */
    static void click(Component c) {
        for (java.awt.event.MouseListener l : c.getMouseListeners()) {
            l.mouseClicked(new java.awt.event.MouseEvent(c, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 1, 1, 1, false));
        }
    }

    /** The clickable card that holds a piece of text. */
    static Component cardWith(Container root, String text) {
        for (WrapLabel label : all(root, WrapLabel.class)) {
            if (!label.getText().equals(text)) continue;
            for (Component c = label; c != null; c = c.getParent()) {
                if (c instanceof Card && c.getMouseListeners().length > 0) return c;
            }
        }
        throw new AssertionError("no clickable card with text \"" + text + "\"");
    }

    /** Every child must sit inside its parent, and no text may be cut off. */
    static void assertFits(Container root, String where) {
        for (Component child : root.getComponents()) {
            if (!child.isVisible()) continue;
            if (child.getX() < 0 || child.getX() + child.getWidth() > root.getWidth() + 1) {
                throw new AssertionError(where + ": " + describe(child) + " sticks out sideways (x=" + child.getX()
                        + " w=" + child.getWidth() + " parent w=" + root.getWidth() + ")");
            }
            if (child instanceof WrapLabel label && label.getWidth() > 0) {
                int need = label.heightFor(label.getWidth());
                if (label.getHeight() < need) {
                    throw new AssertionError(where + ": text is cut off: \"" + label.getText() + "\" needs " + need
                            + " but has " + label.getHeight());
                }
            }
            if (child instanceof PillButton b) {
                int text = b.getFontMetrics(b.getFont()).stringWidth(b.getText());
                if (text > b.getWidth() - 10) throw new AssertionError(where + ": button text clipped: " + b.getText());
            }
            if (child instanceof Badge b) {
                int text = b.getFontMetrics(b.getFont()).stringWidth(b.getText());
                if (text > b.getWidth()) throw new AssertionError(where + ": badge text clipped: " + b.getText());
            }
            if (child instanceof Container box) assertFits(box, where);
        }
    }

    private static String describe(Component c) {
        if (c instanceof WrapLabel l) return "label \"" + l.getText() + "\"";
        if (c instanceof PillButton b) return "button \"" + b.getText() + "\"";
        return c.getClass().getSimpleName();
    }
}
