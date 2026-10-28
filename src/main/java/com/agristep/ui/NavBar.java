package com.agristep.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The main navigation. On wide windows it is a dark side rail with labels beside the icons;
 * on narrow windows (phone-sized) it becomes a bottom bar with the labels under the icons.
 */
final class NavBar extends JPanel {
    private static final String[][] ITEMS = {
            {Navigator.TODAY, "Today", "☀️"},
            {Navigator.GARDEN, "Garden", "🏡"},
            {Navigator.CROPS, "Crops", "📖"},
            {Navigator.CALENDAR, "Calendar", "📅"},
            {Navigator.JOURNAL, "Journal", "📝"},
            {Navigator.INSIGHTS, "Insights", "📊"},
            {Navigator.LOCATION, "Location", "📍"},
            {Navigator.ACCOUNT, "Account", "👤"},
    };

    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private boolean wide = true;
    private JPanel account;

    NavBar(Consumer<String> onChoose) {
        for (String[] item : ITEMS) buttons.put(item[0], new Button(item[0], item[1], item[2], onChoose));
        layoutFor(true);
    }

    boolean isWide() { return wide; }

    void setWide(boolean wide) {
        if (wide == this.wide) return;
        layoutFor(wide);
    }

    void select(String id) {
        buttons.forEach((key, b) -> b.setSelected(key.equals(id)));
        repaint();
    }

    String selected() {
        return buttons.entrySet().stream().filter(e -> e.getValue().selected).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    /** Shows who is signed in (wide layout only; the phone layout shows it in the top strip). */
    void setAccount(String displayName, Runnable onSignOut) {
        account = new JPanel(new StackLayout(6));
        account.setOpaque(false);
        account.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 6, 0, 6));
        account.add(new WrapLabel(I18n.t("Signed in as"), Theme.SMALL_BOLD, Theme.LIME));
        account.add(new WrapLabel(displayName, Theme.BODY_BOLD, Color.WHITE).maxLines(1));
        PillButton out = PillButton.secondary("Sign out");
        out.addActionListener(e -> onSignOut.run());
        account.add(out);
        layoutFor(wide);
    }

    private void layoutFor(boolean wide) {
        this.wide = wide;
        removeAll();
        if (wide) {
            setLayout(new java.awt.BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 12, 16, 12));
            JPanel top = new JPanel(new StackLayout(6));
            top.setOpaque(false);
            top.add(brand());
            top.add(javax.swing.Box.createVerticalStrut(14));
            buttons.values().forEach(b -> {
                b.setWide(true);
                top.add(b);
            });
            add(top, java.awt.BorderLayout.NORTH);
            if (account != null) add(account, java.awt.BorderLayout.SOUTH);
        } else {
            setLayout(new GridLayout(1, ITEMS.length));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
            buttons.values().forEach(b -> {
                b.setWide(false);
                add(b);
            });
        }
        revalidate();
        repaint();
    }

    private JComponent brand() {
        JPanel row = new JPanel(new RowLayout(10, false));
        row.setOpaque(false);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        row.add(Brand.mark(42, true), RowLayout.WEST);
        JPanel text = new JPanel(new StackLayout(0));
        text.setOpaque(false);
        text.add(new WrapLabel("PlantWise PH", Theme.H3, Color.WHITE));
        text.add(new WrapLabel(I18n.t("Backyard farming"), Theme.SMALL, Theme.LIME));
        row.add(text, RowLayout.CENTER);
        return row;
    }

    @Override public Dimension getPreferredSize() {
        return wide ? new Dimension(210, 100) : new Dimension(100, 66);
    }

    @Override protected void paintComponent(Graphics g) {
        if (wide) {
            ((Graphics2D) g).setPaint(new java.awt.GradientPaint(0, 0, Theme.LEAF, 0, getHeight(), Theme.CANOPY_2));
        } else {
            g.setColor(Theme.WHITE);
        }
        g.fillRect(0, 0, getWidth(), getHeight());
        if (!wide) {
            g.setColor(Theme.LINE);
            g.drawLine(0, 0, getWidth(), 0);
        }
    }

    /** One destination. */
    private static final class Button extends JComponent {
        private final String label;
        private final String emoji;
        private boolean wide = true;
        private boolean selected;
        private final Motion.Tween hover = new Motion.Tween(this, 150);

        Button(String id, String label, String emoji, Consumer<String> onChoose) {
            this.label = I18n.t(label);
            this.emoji = emoji;
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(this.label);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                    onChoose.accept(id);
                }
                @Override public void mouseEntered(MouseEvent e) { hover.to(1); }
                @Override public void mouseExited(MouseEvent e) { hover.to(0); }
            });
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "go");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "go");
            getActionMap().put("go", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { onChoose.accept(id); }
            });
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

        void setWide(boolean wide) { this.wide = wide; }
        void setSelected(boolean selected) { this.selected = selected; }

        @Override public Dimension getPreferredSize() { return wide ? new Dimension(180, 44) : new Dimension(80, 64); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            javax.swing.Icon icon = Emoji.icon(emoji, wide ? 24 : 26);
            if (wide) {
                if (selected) {
                    g2.setColor(Theme.LIME);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
                } else if (hover.value() > 0f) {
                    g2.setColor(new Color(255, 255, 255, Math.round(34 * hover.value())));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
                }
                icon.paintIcon(this, g2, 12 + Math.round(3 * hover.value()), (h - icon.getIconHeight()) / 2);
                g2.setFont(Theme.BODY_BOLD);
                g2.setColor(selected ? Theme.INK : Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, 12 + icon.getIconWidth() + 12, (h - fm.getHeight()) / 2 + fm.getAscent());
            } else {
                if (selected) {
                    g2.setColor(Theme.LEAF_SOFT);
                    g2.fillRoundRect(w / 2 - 28, 6, 56, 32, 32, 32);
                }
                icon.paintIcon(this, g2, (w - icon.getIconWidth()) / 2, 9);
                java.awt.Font labelFont = selected ? Theme.SMALL_BOLD : Theme.SMALL;
                g2.setFont(labelFont);
                FontMetrics fm = g2.getFontMetrics();
                for (float size = labelFont.getSize2D(); fm.stringWidth(label) > w - 2 && size > 8f; ) {
                    size -= 0.5f;                       // many destinations on a narrow screen: shrink the label to fit
                    g2.setFont(labelFont.deriveFont(size));
                    fm = g2.getFontMetrics();
                }
                g2.setColor(selected ? Theme.LEAF : Theme.MUTED);
                g2.drawString(label, (w - fm.stringWidth(label)) / 2, h - 10);
            }
            if (hasFocus()) {
                g2.setColor(wide ? Theme.LIME : Theme.LEAF_2);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 14, 14);
            }
            g2.dispose();
        }
    }
}
