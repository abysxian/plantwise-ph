package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.Crop;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The whole app window's content: navigation plus the current screen. It switches between the desktop layout
 * (side rail) and the phone layout (bottom bar) by looking at its own width.
 */
public final class MainPanel extends JPanel implements Navigator {
    static final int WIDE_BREAKPOINT = 860;

    private final CardLayout cards = new CardLayout();
    private final JPanel stage = new JPanel(cards);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, JScrollPane> scrolls = new LinkedHashMap<>();
    private final NavBar navBar;
    private final FlashBar flash = new FlashBar();
    private final JPanel topBar;
    private final JPanel north = new JPanel(new BorderLayout());
    private boolean wideMode = true;
    private String current;

    /** For tests and simple uses: a window with no sign-out action. */
    public MainPanel(AppContext ctx) {
        this(ctx, () -> { });
    }

    public MainPanel(AppContext ctx, Runnable onSignOut) {
        super(new BorderLayout());
        setBackground(Theme.PAPER);
        String who = ctx.session.require().getDisplayName();
        topBar = brandStrip(who, onSignOut);

        addPage(Navigator.TODAY, new TodayPage(ctx, this));
        addPage(Navigator.GARDEN, new GardenPage(ctx, this));
        addPage(Navigator.CROPS, new CropsPage(ctx, this));
        addPage(Navigator.LOCATION, new LocationPage(ctx, this));
        addPage(Navigator.CROP_DETAIL, new CropDetailPage(ctx, this));

        navBar = new NavBar(this::show);
        navBar.setAccount(who, onSignOut);
        north.setOpaque(false);
        north.add(topBar, BorderLayout.NORTH);
        north.add(flash, BorderLayout.SOUTH);
        topBar.setVisible(false);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(north, BorderLayout.NORTH);
        center.add(stage, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(navBar, BorderLayout.WEST);
        show(Navigator.TODAY);
    }

    private void addPage(String id, Page page) {
        JScrollPane scroll = new JScrollPane(page, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        pages.put(id, page);
        scrolls.put(id, scroll);
        stage.add(scroll, id);
    }

    private static JPanel brandStrip(String who, Runnable onSignOut) {
        JPanel strip = new JPanel(new RowLayout(10, false)) {
            @Override public java.awt.Dimension getPreferredSize() { return new java.awt.Dimension(100, 48); }

            @Override protected void paintComponent(java.awt.Graphics g) {
                g.setColor(Theme.LEAF);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        strip.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 14, 0, 14));
        strip.add(Brand.mark(34), RowLayout.WEST);
        strip.add(new WrapLabel("PlantWise PH · " + who, Theme.H3, Color.WHITE).maxLines(1), RowLayout.CENTER);
        PillButton out = PillButton.secondary("Sign out");
        out.addActionListener(e -> onSignOut.run());
        strip.add(out, RowLayout.EAST);
        return strip;
    }

    /** Chooses the desktop or phone layout from the current width, before the children are laid out. */
    @Override public void doLayout() {
        boolean wide = getWidth() >= WIDE_BREAKPOINT;
        if (wide != wideMode) applyMode(wide);
        super.doLayout();
    }

    private void applyMode(boolean wide) {
        wideMode = wide;
        navBar.setWide(wide);
        remove(navBar);
        add(navBar, wide ? BorderLayout.WEST : BorderLayout.SOUTH);
        topBar.setVisible(!wide);
    }

    boolean isWideMode() { return wideMode; }

    String currentPage() { return current; }

    NavBar navBar() { return navBar; }

    FlashBar flashBar() { return flash; }

    Page page(String id) { return pages.get(id); }

    @Override public void show(String page) {
        if (!pages.containsKey(page)) return;
        current = page;
        cards.show(stage, page);
        navBar.select(Navigator.CROP_DETAIL.equals(page) ? Navigator.CROPS : page);
        pages.get(page).onShow();
        SwingUtilities.invokeLater(() -> scrolls.get(page).getVerticalScrollBar().setValue(0));
    }

    @Override public void showCrop(Crop crop) {
        ((CropDetailPage) pages.get(Navigator.CROP_DETAIL)).setCrop(crop);
        show(Navigator.CROP_DETAIL);
    }

    @Override public void flash(String message, boolean error) {
        Tasks.later(() -> flash.show(message, error));
    }

    /** Lets tests and the frame find a screen's scroll pane. */
    Component scrollOf(String id) { return scrolls.get(id); }
}
