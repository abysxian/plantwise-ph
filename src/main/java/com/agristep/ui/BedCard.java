package com.agristep.ui;

import com.agristep.domain.Plot;
import com.agristep.domain.PlotStatus;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One garden bed as a square tile. The green fill rises as the crop gets closer to harvest. */
final class BedCard extends JComponent {
    private static final Color GROWING_FILL = new Color(0xBFDDB0);

    private final Plot plot;
    private final PlotStatus status;
    private final int progress;
    private final String statusText;
    private final boolean selected;

    BedCard(Plot plot, LocalDate today, boolean selected, Runnable onSelect) {
        this.plot = plot;
        this.status = plot.status(today);
        this.progress = plot.isEmpty() ? 0 : plot.progressPercent();
        this.selected = selected;
        this.statusText = describe(plot, today);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                onSelect.run();
            }
        });
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "select");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "select");
        getActionMap().put("select", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onSelect.run(); }
        });
        getAccessibleContext().setAccessibleName(plot.getName() + ", " + statusText);
    }

    private static String describe(Plot plot, LocalDate today) {
        if (plot.isEmpty()) return "Tap to plant";
        PlotStatus s = plot.status(today);
        if (s == PlotStatus.READY) return "Ready!";
        long day = ChronoUnit.DAYS.between(plot.getPlantedOn(), today);
        return "Day " + Math.max(0, day) + " of " + plot.getCrop().getCycleDays();
    }

    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }

    Plot getPlot() { return plot; }
    boolean isSelected() { return selected; }
    String getStatusText() { return statusText; }
    int getProgress() { return progress; }

    @Override public Dimension getPreferredSize() { return new Dimension(100, 100); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Shape tile = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 20, 20);

        g2.setColor(plot.isEmpty() ? Theme.PAPER : Theme.WHITE);
        g2.fill(tile);
        if (!plot.isEmpty()) {
            Shape oldClip = g2.getClip();
            g2.clip(tile);
            int fillH = Math.round(h * Math.max(progress, status == PlotStatus.READY ? 100 : 6) / 100f);
            g2.setColor(status == PlotStatus.READY ? Theme.LIME : GROWING_FILL);
            g2.fillRect(0, h - fillH, w, fillH);
            g2.setClip(oldClip);
        }
        if (selected) {
            g2.setColor(Theme.LEAF);
            g2.setStroke(new java.awt.BasicStroke(3f));
        } else {
            g2.setColor(status == PlotStatus.READY ? Theme.LEAF_2 : Theme.LINE);
            g2.setStroke(new java.awt.BasicStroke(status == PlotStatus.READY ? 2f : 1f));
        }
        g2.draw(tile);
        if (hasFocus()) {
            g2.setColor(Theme.LIME);
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawRoundRect(5, 5, w - 11, h - 11, 14, 14);
        }

        // bed name, top left
        g2.setFont(Theme.SMALL_BOLD);
        FontMetrics small = g2.getFontMetrics();
        g2.setColor(Theme.MUTED);
        g2.drawString(fit(plot.getName(), small, w - 20), 10, 10 + small.getAscent());

        // emoji in the middle
        int icon = Math.max(22, Math.min(44, Math.round(Math.min(w, h) * 0.30f)));
        String emoji = plot.isEmpty() ? "🌱" : plot.getCrop().getEmoji();
        javax.swing.Icon pic = Emoji.icon(emoji, icon);
        java.awt.Composite oldComposite = g2.getComposite();
        if (plot.isEmpty()) g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
        pic.paintIcon(this, g2, (w - icon) / 2, (int) (h * 0.22f));
        g2.setComposite(oldComposite);

        // crop name and status along the bottom
        g2.setFont(Theme.BODY_BOLD);
        FontMetrics body = g2.getFontMetrics();
        String title = plot.isEmpty() ? "Empty" : plot.getCrop().getName();
        g2.setColor(plot.isEmpty() ? Theme.MUTED : Theme.INK);
        String shownTitle = fit(title, body, w - 16);
        g2.drawString(shownTitle, (w - body.stringWidth(shownTitle)) / 2, h - 12 - small.getHeight() - 2);
        g2.setFont(Theme.SMALL);
        FontMetrics note = g2.getFontMetrics();
        g2.setColor(status == PlotStatus.READY ? Theme.LEAF : Theme.MUTED);
        String shownStatus = fit(statusText, note, w - 16);
        g2.drawString(shownStatus, (w - note.stringWidth(shownStatus)) / 2, h - 12);
        g2.dispose();
    }

    private static String fit(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > width) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
