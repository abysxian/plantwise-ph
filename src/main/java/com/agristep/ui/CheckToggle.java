package com.agristep.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** A round tick box for a care task. */
final class CheckToggle extends JComponent {
    private final boolean checked;
    private final boolean alert;

    CheckToggle(boolean checked, boolean alert, Runnable onToggle) {
        this.checked = checked;
        this.alert = alert;
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onToggle.run(); }
        });
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggle");
        getActionMap().put("toggle", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onToggle.run(); }
        });
    }

    boolean isChecked() { return checked; }

    @Override public Dimension getPreferredSize() { return new Dimension(28, 28); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int d = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;
        Color edge = checked ? Theme.LEAF : alert ? Theme.SILI : Theme.MUTED;
        if (checked) {
            g2.setColor(Theme.LEAF);
            g2.fillOval(x, y, d, d);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[] {x + d / 4, x + d * 2 / 5, x + d * 3 / 4},
                    new int[] {y + d / 2, y + d * 2 / 3, y + d / 3}, 3);
        } else {
            g2.setColor(Theme.WHITE);
            g2.fillOval(x, y, d, d);
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x, y, d, d);
        }
        if (hasFocus()) {
            g2.setColor(Theme.LIME);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x - 2, y - 2, d + 4, d + 4);
        }
        g2.dispose();
    }
}
