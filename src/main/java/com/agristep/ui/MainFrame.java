package com.agristep.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

/** The application window. It first shows a loading note, then either the app or a friendly start-up error. */
public final class MainFrame extends JFrame implements Shell {

    public MainFrame() {
        super("PlantWise PH");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        if (Brand.hasLogo()) {
            setIconImages(java.util.List.of(Brand.icon(16), Brand.icon(32), Brand.icon(64), Brand.icon(128)));
        } else {
            BufferedImage icon = Emoji.image("🌱");
            if (icon != null) setIconImage(icon);
        }
        setMinimumSize(new Dimension(340, 560));
        setSize(1100, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.PAPER);
        showLoading("Opening your garden…");
    }

    public void showLoading(String message) {
        setBody(message(message, null, null));
    }

    public void showError(String message, Runnable retry) {
        setBody(message(message, "Try again", retry));
    }

    @Override public void showBody(Component body) { setBody(body); }

    @Override public void setWindowTitle(String title) { setTitle(title); }

    private void setBody(Component body) {
        getContentPane().removeAll();
        getContentPane().add(body, BorderLayout.CENTER);
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private static Component message(String text, String buttonText, Runnable action) {
        ScrollPage page = new ScrollPage();
        page.setBackground(Theme.PAPER);
        Card card = new Card().gap(12).padding(20);
        card.add(Brand.mark(72, false));
        card.add(new WrapLabel(buttonText == null ? "PlantWise PH" : "PlantWise couldn't start", Theme.H2, Theme.INK)
                .align(WrapLabel.CENTER));
        card.add(WrapLabel.body(text).align(WrapLabel.CENTER));
        if (action != null) {
            PillButton retry = PillButton.primary(buttonText);
            retry.addActionListener(e -> action.run());
            JPanel row = new JPanel(new RowLayout(0, false));
            row.setOpaque(false);
            row.add(retry, RowLayout.CENTER);
            card.add(row);
        }
        page.add(Box.spacer(60));
        page.add(card);
        return new javax.swing.JScrollPane(page, javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
            { setBorder(null); getViewport().setBackground(Color.WHITE); }
        };
    }

    /** A blank gap that works inside a StackLayout. */
    private static final class Box extends JPanel {
        private final int height;

        private Box(int height) {
            this.height = height;
            setOpaque(false);
        }

        static Box spacer(int height) { return new Box(height); }

        @Override public Dimension getPreferredSize() { return new Dimension(1, height); }
    }
}
