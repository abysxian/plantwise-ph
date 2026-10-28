package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.JournalEntry;
import com.agristep.domain.Plot;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dated notes and photos about the garden or about one bed. */
final class JournalPage extends Page {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final String GENERAL = "The whole garden";
    private static final int THUMB_W = 240, THUMB_H = 180;

    private record Loaded(List<Plot> plots, List<JournalEntry> entries, Map<Long, ImageIcon> thumbs) {}

    private final JTextArea note = Widgets.textArea(4);
    private final WrapLabel photoName = WrapLabel.small("No photo chosen.");
    private Path photo;
    private Long preselected;
    private Long armedDelete;
    private Loaded loaded;
    private JComboBox<Object> bedBox;
    private int generation;

    JournalPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    /** Chooses the bed that the next note is about (used by "Write a note" on the Garden screen). */
    void preselect(Long plotId) { this.preselected = plotId; }

    @Override void onShow() {
        armedDelete = null;
        int mine = ++generation;
        setContent(TodayPage.loadingCard("Opening your journal…"));
        Tasks.run(() -> {
            List<JournalEntry> entries = ctx.journal.list();
            Map<Long, ImageIcon> thumbs = new HashMap<>();
            for (JournalEntry e : entries) {
                Path p = ctx.journal.photoPath(e);
                if (p == null) continue;
                try {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) thumbs.put(e.getId(), new ImageIcon(fit(img)));
                } catch (java.io.IOException ignored) {
                    // a missing picture file just means no thumbnail
                }
            }
            return new Loaded(ctx.garden.allPlots(), entries, thumbs);
        }, result -> {
            if (mine != generation) return;
            loaded = result;
            render();
        }, this::fail);
    }

    private static Image fit(BufferedImage img) {
        double scale = Math.min((double) THUMB_W / img.getWidth(), (double) THUMB_H / img.getHeight());
        scale = Math.min(scale, 1.0);
        return img.getScaledInstance(Math.max(1, (int) (img.getWidth() * scale)), Math.max(1, (int) (img.getHeight() * scale)),
                Image.SCALE_SMOOTH);
    }

    private void render() {
        Card header = banner("Garden journal", loaded.entries().isEmpty()
                ? "Write down what you notice, with a photo if you like. Notes belong to the garden, so everyone sharing it sees them."
                : loaded.entries().size() + (loaded.entries().size() == 1 ? " note" : " notes")
                + " so far. Newest first.", Scenery.Photo.WET_LEAVES);
        JPanel parts = new JPanel(new StackLayout(10));
        parts.setOpaque(false);
        parts.add(formCard());
        for (JournalEntry e : loaded.entries()) parts.add(entryCard(e));
        if (loaded.entries().isEmpty()) parts.add(new Card().add(WrapLabel.muted("No notes yet. Your first one will show up here.")));
        setContent(header, parts);
    }

    private Card formCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h3("New note"));
        List<Object> choices = new java.util.ArrayList<>();
        choices.add(GENERAL);
        choices.addAll(loaded.plots());
        bedBox = Widgets.textBox(choices, o -> o instanceof Plot p
                ? p.getName() + (p.isEmpty() ? " (empty)" : " (" + p.getCrop().getName() + ")") : String.valueOf(o));
        if (preselected != null) {
            for (Object o : choices) if (o instanceof Plot p && p.getId().equals(preselected)) bedBox.setSelectedItem(o);
            preselected = null;
        }
        card.add(Widgets.labelled("About", bedBox));
        card.add(Widgets.labelled("What did you notice?", note));

        PillButton add = PillButton.secondary("Add photo");
        add.addActionListener(e -> choosePhoto());
        PillButton save = PillButton.primary("Save entry");
        save.addActionListener(e -> saveEntry());
        JPanel buttons = new JPanel(new FlowGridLayout(130, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(save);
        card.add(buttons);
        card.add(photoName);
        return card;
    }

    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Pictures (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
            attachPhoto(chooser.getSelectedFile().toPath());
        }
    }

    void attachPhoto(Path file) {
        photo = file;
        photoName.setText("Photo: " + file.getFileName());
    }

    void saveEntry() {
        Object chosen = bedBox == null ? GENERAL : bedBox.getSelectedItem();
        Long plotId = chosen instanceof Plot p ? p.getId() : null;
        String text = note.getText();
        Path picked = photo;
        Tasks.run(() -> ctx.journal.add(plotId, text, picked), saved -> {
            note.setText("");
            photo = null;
            photoName.setText("No photo chosen.");
            nav.flash("Note saved.", false);
            onShow();
        }, this::fail);
    }

    private Card entryCard(JournalEntry e) {
        Card card = new Card().gap(8);
        ImageIcon thumb = loaded.thumbs().get(e.getId());
        if (thumb != null) card.add(Widgets.left(new JLabel(thumb)));
        JPanel body = new JPanel(new StackLayout(4));
        body.setOpaque(false);
        String about = e.getPlotName() == null ? "Whole garden"
                : e.getPlotName() + (e.getCropName() == null ? "" : " · " + e.getCropName());
        body.add(new WrapLabel(DATE.format(e.getDate()) + " · " + about, Theme.SMALL_BOLD, Theme.LEAF_2));
        body.add(WrapLabel.body(e.getNote()));
        body.add(WrapLabel.small("Written by " + e.getAuthorName()));
        card.add(body);

        boolean armed = e.getId().equals(armedDelete);
        PillButton delete = armed ? PillButton.danger("Really delete?") : PillButton.secondary("Delete");
        delete.addActionListener(ev -> {
            if (!armed) {
                armedDelete = e.getId();
                render();
                return;
            }
            Tasks.run(() -> {
                ctx.journal.delete(e.getId());
                return e.getId();
            }, id -> {
                armedDelete = null;
                nav.flash("Note deleted.", false);
                onShow();
            }, this::fail);
        });
        card.add(Widgets.left(delete));
        return card;
    }
}
