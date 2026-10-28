package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.CareTask;
import com.agristep.domain.Crop;
import com.agristep.domain.HarvestRecord;
import com.agristep.domain.Plot;
import com.agristep.domain.PlotStatus;
import com.agristep.weather.WeatherForecast;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The garden: a grid of beds the person can resize, plus a side panel for the selected bed. */
final class GardenPage extends Page {
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private record GardenData(List<Plot> plots, List<HarvestRecord> harvests, List<Crop> crops,
                              Optional<WeatherForecast> forecast, LocalDate today) {}

    private GardenData data;
    private com.agristep.service.GridShape shape = new com.agristep.service.GridShape(3, 3);
    private boolean editingGrid;
    private int wantRows;
    private int wantColumns;
    private String gridWarning = "";
    private boolean needsConfirm;
    private Long selectedId;
    private int generation;

    GardenPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    @Override void onShow() { reload(); }

    private void reload() {
        int mine = ++generation;
        if (data == null) setContent(TodayPage.loadingCard("Opening your garden…"));
        Tasks.run(() -> {
            List<Plot> plots = ctx.garden.allPlots();
            GardenData d = new GardenData(plots, ctx.garden.recentHarvests(), ctx.crops.findAllByOrderByNameAsc(),
                    ctx.weather.forecast(ctx.settings.current()), ctx.garden.today());
            return new Object[] {d, ctx.grid.shape()};
        }, both -> {
            if (mine != generation) return;
            GardenData loaded = (GardenData) both[0];
            shape = (com.agristep.service.GridShape) both[1];
            data = loaded;
            boolean stillThere = loaded.plots().stream().anyMatch(p -> p.getId().equals(selectedId));
            if (!stillThere) {
                selectedId = loaded.plots().stream().filter(p -> !p.isEmpty()).findFirst()
                        .or(() -> loaded.plots().stream().findFirst()).map(Plot::getId).orElse(null);
            }
            render();
        }, this::fail);
    }

    private void render() {
        PillButton edit = editingGrid ? PillButton.secondary("Done") : PillButton.primary("Edit grid");
        edit.addActionListener(e -> toggleGridEditor());
        JPanel action = new JPanel(new RowLayout(10, false));
        action.setOpaque(false);
        action.add(edit, RowLayout.WEST);
        Card header = banner("Your garden", shape.rows() + " × " + shape.columns() + " grid, " + data.plots().size()
                + (data.plots().size() == 1 ? " bed" : " beds") + ". Each bed fills with green as its crop grows. "
                + "Tap a bed to see what to do next.", Scenery.Photo.MONSTERA, action);

        int cols = Math.max(1, shape.columns());
        JPanel beds = new JPanel(new FlowGridLayout(cols <= 3 ? 96 : cols <= 5 ? 66 : 50, cols, cols <= 4 ? 10 : 6, 1.0));
        beds.setOpaque(false);
        for (Plot plot : data.plots()) {
            beds.add(new BedCard(plot, data.today(), plot.getId().equals(selectedId), () -> select(plot.getId())));
        }

        JPanel split = new JPanel(new SplitLayout(820, 350, 14));
        split.setOpaque(false);
        split.add(beds);
        split.add(sidePanel());

        if (editingGrid) setContent(header, gridEditor(), split, historyCard());
        else setContent(header, split, historyCard());
    }

    private void select(Long plotId) {
        selectedId = plotId;
        render();
    }

    // ---- grid editor ----

    private void toggleGridEditor() {
        editingGrid = !editingGrid;
        wantRows = shape.rows();
        wantColumns = shape.columns();
        gridWarning = "";
        needsConfirm = false;
        render();
    }

    private Card gridEditor() {
        Card card = new Card().gap(12);
        card.add(WrapLabel.h3("Grid size"));
        card.add(WrapLabel.muted("Choose how many rows and columns of beds you have. New beds are added empty at the end; "
                + "a smaller grid removes beds from the end."));

        Stepper rows = new Stepper("rows", wantRows, 1, com.agristep.service.GardenGridService.MAX_ROWS);
        Stepper columns = new Stepper("columns", wantColumns, 1, com.agristep.service.GardenGridService.MAX_COLUMNS);
        WrapLabel summary = WrapLabel.bold("");
        Runnable refresh = () -> {
            wantRows = rows.value();
            wantColumns = columns.value();
            summary.setText(wantRows + " × " + wantColumns + " = " + (wantRows * wantColumns) + " beds");
            gridWarning = "";
            needsConfirm = false;
        };
        rows.onChange(v -> refresh.run());
        columns.onChange(v -> refresh.run());
        summary.setText(wantRows + " × " + wantColumns + " = " + (wantRows * wantColumns) + " beds");

        JPanel pair = new JPanel(new FlowGridLayout(170, 2, 12, 0));
        pair.setOpaque(false);
        pair.add(labelledStepper("Rows", rows));
        pair.add(labelledStepper("Columns", columns));
        card.add(pair);

        JPanel presets = new JPanel(new FlowGridLayout(70, 5, 8, 0));
        presets.setOpaque(false);
        for (int[] preset : new int[][] {{2, 2}, {3, 3}, {4, 4}, {4, 6}, {6, 6}}) {
            PillButton b = PillButton.secondary(preset[0] + " × " + preset[1]);
            b.addActionListener(e -> {
                rows.setQuietly(preset[0]);
                columns.setQuietly(preset[1]);
                refresh.run();
            });
            presets.add(b);
        }
        card.add(presets);
        card.add(summary);

        if (!gridWarning.isEmpty()) {
            Card warn = new Card(Theme.SILI_SOFT, Theme.SILI).padding(12).gap(8);
            warn.add(new WrapLabel(gridWarning, Theme.BODY, Theme.SILI));
            card.add(warn);
        }
        PillButton apply = needsConfirm ? PillButton.danger("Remove beds and resize") : PillButton.primary("Apply grid");
        apply.addActionListener(e -> applyGrid(needsConfirm));
        card.add(apply);
        return card;
    }

    private static JPanel labelledStepper(String label, Stepper stepper) {
        JPanel box = new JPanel(new StackLayout(4));
        box.setOpaque(false);
        box.add(new WrapLabel(label, Theme.SMALL_BOLD, Theme.MUTED));
        box.add(stepper);
        return box;
    }

    private void applyGrid(boolean confirmed) {
        int r = wantRows;
        int c = wantColumns;
        Tasks.run(() -> ctx.grid.resize(r, c, confirmed), done -> {
            editingGrid = false;
            gridWarning = "";
            needsConfirm = false;
            nav.flash("Your garden is now " + r + " × " + c + " (" + (r * c) + " beds).", false);
            reload();
        }, t -> {
            gridWarning = Tasks.messageOf(t);
            needsConfirm = t instanceof com.agristep.service.GridResizeException
                    || (t.getCause() instanceof com.agristep.service.GridResizeException);
            render();
        });
    }

    // ---- side panel ----

    private Card sidePanel() {
        Plot plot = data.plots().stream().filter(p -> p.getId().equals(selectedId)).findFirst().orElse(null);
        Card card = new Card().gap(10);
        if (plot == null) {
            card.add(WrapLabel.h3("No beds yet"));
            card.add(WrapLabel.muted("Tap \"Edit grid\" to make your first beds."));
        } else if (plot.isEmpty()) {
            emptyBedPanel(card, plot);
        } else {
            plantedBedPanel(card, plot);
        }
        return card;
    }

    private void emptyBedPanel(Card card, Plot plot) {
        card.add(WrapLabel.h2(plot.getName() + " is empty"));
        card.add(WrapLabel.muted("Choose a vegetable and its care plan is made for you, with dates counted from today."));
        JComboBox<Crop> box = new JComboBox<>(data.crops().toArray(new Crop[0]));
        box.setRenderer((JList<? extends Crop> list, Crop value, int index, boolean selected, boolean focus) -> {
            JLabel label = new JLabel(value == null ? "" : value.getName() + "  (" + value.getEnglishName() + ")");
            if (value != null) label.setIcon(Emoji.icon(value.getEmoji(), 22));
            label.setIconTextGap(8);
            label.setOpaque(true);
            label.setFont(Theme.BODY);
            label.setForeground(Theme.INK);
            label.setBackground(selected ? Theme.LEAF_SOFT : Theme.WHITE);
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        });
        card.add(box);
        JPanel hints = new JPanel(new StackLayout(6));
        hints.setOpaque(false);
        card.add(hints);
        Runnable refreshHints = () -> showHints(plot, (Crop) box.getSelectedItem(), hints);
        box.addActionListener(e -> refreshHints.run());
        refreshHints.run();
        PillButton plant = PillButton.primary("Plant here");
        plant.addActionListener(e -> {
            Crop crop = (Crop) box.getSelectedItem();
            if (crop == null) return;
            Tasks.run(() -> ctx.garden.plant(plot.getId(), crop.getId(), null), planted -> {
                nav.flash("Planted " + crop.getName() + " in " + planted.getName() + ".", false);
                reload();
            }, this::fail);
        });
        PillButton advice = PillButton.secondary("What fits best?");
        advice.addActionListener(e -> nav.show(Navigator.TODAY));
        JPanel buttons = new JPanel(new FlowGridLayout(110, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(plant);
        buttons.add(advice);
        card.add(buttons);
    }

    /** Companion-planting and crop-rotation suggestions for the crop chosen in the drop-down. */
    private void showHints(Plot plot, Crop crop, JPanel into) {
        if (crop == null) return;
        int columns = shape.columns();
        List<Plot> plots = data.plots();
        LocalDate today = data.today();
        Tasks.run(() -> ctx.planting.hintsFor(crop, plot, plots, columns, ctx.garden.allHarvests(), today), hints -> {
            into.removeAll();
            for (com.agristep.service.PlantingAdvisor.Hint hint : hints) {
                Card c = switch (hint.kind()) {
                    case GOOD -> new Card(Theme.LEAF_SOFT, Theme.LEAF_2);
                    case AVOID -> new Card(Theme.LIME_SOFT, Theme.LIME);
                    case ROTATE -> new Card(Theme.RAIN_SOFT, Theme.RAIN);
                };
                String label = switch (hint.kind()) {
                    case GOOD -> "Good neighbours: ";
                    case AVOID -> "Better apart: ";
                    case ROTATE -> "Rotate crops: ";
                };
                c.padding(10).add(WrapLabel.body(label + hint.text()));
                into.add(c);
            }
            into.revalidate();
            into.repaint();
            revalidate();
        }, this::fail);
    }

    private void plantedBedPanel(Card card, Plot plot) {
        Crop crop = plot.getCrop();
        LocalDate today = data.today();
        PlotStatus status = plot.status(today);

        JPanel head = new JPanel(new RowLayout(12, false));
        head.setOpaque(false);
        head.add(Emoji.label(crop.getEmoji(), 44), RowLayout.WEST);
        JPanel titles = new JPanel(new StackLayout(2));
        titles.setOpaque(false);
        titles.add(WrapLabel.h2(crop.getName() + " · " + plot.getName()));
        long day = Math.max(0, ChronoUnit.DAYS.between(plot.getPlantedOn(), today));
        titles.add(WrapLabel.small("Planted " + SHORT_DATE.format(plot.getPlantedOn()) + " · day " + day
                + " of about " + crop.getCycleDays()));
        head.add(titles, RowLayout.CENTER);
        card.add(head);

        JPanel statusRow = new JPanel(new RowLayout(10, false));
        statusRow.setOpaque(false);
        Badge badge = status == PlotStatus.READY ? new Badge(status.getLabel(), Theme.LEAF, Theme.LIME)
                : new Badge(status.getLabel(), Theme.LEAF, Theme.LEAF_SOFT);
        statusRow.add(badge, RowLayout.WEST);
        statusRow.add(new Meter(plot.progressPercent(), Theme.LEAF_2), RowLayout.CENTER);
        statusRow.add(WrapLabel.small(plot.progressPercent() + "%"), RowLayout.EAST);
        card.add(statusRow);

        card.add(WrapLabel.h3("Checklist"));
        for (CareTask task : plot.getTasks()) card.add(taskRow(plot, task, today));

        JPanel buttons = new JPanel(new FlowGridLayout(130, 2, 8, 0));
        buttons.setOpaque(false);
        PillButton harvest = status == PlotStatus.READY ? PillButton.primary("Harvest") : PillButton.secondary("Harvest");
        harvest.addActionListener(e -> harvest(plot, status));
        PillButton clear = PillButton.danger("Clear bed");
        clear.addActionListener(e -> clear(plot));
        PillButton guide = PillButton.secondary("Care guide");
        guide.addActionListener(e -> nav.showCrop(crop));
        PillButton journal = PillButton.secondary("Write a note");
        journal.addActionListener(e -> nav.showJournalFor(plot.getId()));
        buttons.add(harvest);
        buttons.add(clear);
        buttons.add(guide);
        buttons.add(journal);
        card.add(buttons);
    }

    private JComponent taskRow(Plot plot, CareTask task, LocalDate today) {
        boolean done = task.isCompleted();
        boolean overdue = task.isOverdue(today);
        Card row = new Card(done ? Theme.PAPER : Theme.WHITE, overdue ? Theme.SILI : Theme.LINE).padding(10);
        JPanel line = new JPanel(new RowLayout(10, true));
        line.setOpaque(false);
        line.add(new CheckToggle(done, overdue, () -> toggle(plot, task)), RowLayout.WEST);

        JPanel body = new JPanel(new StackLayout(2));
        body.setOpaque(false);
        body.add(WrapLabel.bold(task.getTitle()).strike(done).color(done ? Theme.MUTED : Theme.INK));
        body.add(new WrapLabel(dueText(task, today), Theme.SMALL_BOLD,
                done ? Theme.LEAF : overdue ? Theme.SILI : Theme.MUTED));
        if (!done) {
            body.add(new WrapLabel(task.getInstructions(), Theme.SMALL, Theme.INK));
            data.forecast().flatMap(task::weatherHint)
                    .ifPresent(hint -> body.add(new WrapLabel(hint, Theme.SMALL_BOLD, Theme.RAIN)));
        }
        line.add(body, RowLayout.CENTER);
        line.add(Emoji.label(task.getIcon(), 24), RowLayout.EAST);
        row.add(line);
        return row;
    }

    private static String dueText(CareTask task, LocalDate today) {
        if (task.isCompleted()) {
            return task.getCompletedAt() == null ? "Done"
                    : "Done " + SHORT_DATE.format(task.getCompletedAt().toLocalDate());
        }
        long days = ChronoUnit.DAYS.between(today, task.getDueDate());
        String date = SHORT_DATE.format(task.getDueDate());
        if (days < 0) return "Overdue by " + (-days) + (days == -1 ? " day" : " days") + " · was due " + date;
        if (days == 0) return "Due today";
        if (days == 1) return "Due tomorrow · " + date;
        return "Due in " + days + " days · " + date;
    }

    private void toggle(Plot plot, CareTask task) {
        Tasks.run(() -> ctx.garden.toggleTask(plot.getId(), task.getId()), t -> reload(), this::fail);
    }

    private void harvest(Plot plot, PlotStatus status) {
        if (status != PlotStatus.READY && !confirm("It isn't ready yet.",
                "Harvest " + plot.getCrop().getName() + " from " + plot.getName() + " anyway?")) return;
        String crop = plot.getCrop().getName();
        Tasks.run(() -> ctx.garden.harvest(plot.getId()), record -> {
            nav.flash("Harvested " + crop + " from " + plot.getName() + ". Saved to your harvest history.", false);
            reload();
        }, this::fail);
    }

    private void clear(Plot plot) {
        if (!confirm("Clear " + plot.getName() + "?",
                "The care plan for " + plot.getCrop().getName() + " is removed and nothing is saved as a harvest.")) return;
        Tasks.run(() -> {
            ctx.garden.clear(plot.getId());
            return plot.getName();
        }, name -> {
            nav.flash(name + " is empty again.", false);
            reload();
        }, this::fail);
    }

    private boolean confirm(String title, String message) {
        Component parent = SwingUtilities.getWindowAncestor(this);
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    // ---- harvest history ----

    private Card historyCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h2("Harvest history"));
        if (data.harvests().isEmpty()) {
            card.add(WrapLabel.muted("Nothing harvested yet. When you harvest a bed it is saved here."));
            return card;
        }
        for (HarvestRecord h : data.harvests()) {
            JPanel row = new JPanel(new RowLayout(12, false));
            row.setOpaque(false);
            row.add(Emoji.label(h.getCropEmoji(), 30), RowLayout.WEST);
            JPanel text = new JPanel(new StackLayout(1));
            text.setOpaque(false);
            text.add(WrapLabel.bold(h.getCropName() + " · " + h.getPlotName()));
            text.add(WrapLabel.small("Planted " + SHORT_DATE.format(h.getPlantedOn()) + ", harvested "
                    + SHORT_DATE.format(h.getHarvestedOn()) + " · " + h.daysGrown() + " days"));
            row.add(text, RowLayout.CENTER);
            row.add(new Badge(h.getCompletionPercent() + "% grown", Theme.LEAF, Theme.LEAF_SOFT), RowLayout.EAST);
            card.add(row);
        }
        return card;
    }
}
