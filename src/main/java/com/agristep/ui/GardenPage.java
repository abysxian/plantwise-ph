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

/** The garden: a 3-column grid of beds, plus a side panel for the selected bed. */
final class GardenPage extends Page {
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private record GardenData(List<Plot> plots, List<HarvestRecord> harvests, List<Crop> crops,
                              Optional<WeatherForecast> forecast, LocalDate today) {}

    private GardenData data;
    private Long selectedId;
    private int generation;

    GardenPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    @Override void onShow() { reload(); }

    private void reload() {
        int mine = ++generation;
        if (data == null) setContent(TodayPage.loadingCard("Opening your garden…"));
        Tasks.run(() -> {
            List<Plot> plots = ctx.garden.allPlots();
            return new GardenData(plots, ctx.garden.recentHarvests(), ctx.crops.findAllByOrderByNameAsc(),
                    ctx.weather.forecast(ctx.settings.current()), ctx.garden.today());
        }, loaded -> {
            if (mine != generation) return;
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
        Card header = new Card().gap(10);
        JPanel top = new JPanel(new RowLayout(10, false));
        top.setOpaque(false);
        PillButton add = PillButton.primary("Add a bed");
        add.addActionListener(e -> addBed());
        top.add(WrapLabel.h1("Your garden"), RowLayout.CENTER);
        top.add(add, RowLayout.EAST);
        header.add(top);
        header.add(WrapLabel.muted("Each bed fills with green as its crop grows. Tap a bed to see what to do next."));

        JPanel beds = new JPanel(new FlowGridLayout(96, 3, 10, 1.0));
        beds.setOpaque(false);
        for (Plot plot : data.plots()) {
            beds.add(new BedCard(plot, data.today(), plot.getId().equals(selectedId), () -> select(plot.getId())));
        }

        JPanel split = new JPanel(new SplitLayout(820, 350, 14));
        split.setOpaque(false);
        split.add(beds);
        split.add(sidePanel());

        setContent(header, split, historyCard());
    }

    private void select(Long plotId) {
        selectedId = plotId;
        render();
    }

    private void addBed() {
        Tasks.run(ctx.garden::addPlot, plot -> {
            selectedId = plot.getId();
            nav.flash(plot.getName() + " added.", false);
            reload();
        }, this::fail);
    }

    // ---- side panel ----

    private Card sidePanel() {
        Plot plot = data.plots().stream().filter(p -> p.getId().equals(selectedId)).findFirst().orElse(null);
        Card card = new Card().gap(10);
        if (plot == null) {
            card.add(WrapLabel.h3("No beds yet"));
            card.add(WrapLabel.muted("Tap \"Add a bed\" to make your first one."));
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

        JPanel buttons = new JPanel(new FlowGridLayout(100, 3, 8, 0));
        buttons.setOpaque(false);
        PillButton harvest = status == PlotStatus.READY ? PillButton.primary("Harvest") : PillButton.secondary("Harvest");
        harvest.addActionListener(e -> harvest(plot, status));
        PillButton clear = PillButton.danger("Clear bed");
        clear.addActionListener(e -> clear(plot));
        PillButton guide = PillButton.secondary("Care guide");
        guide.addActionListener(e -> nav.showCrop(crop));
        buttons.add(harvest);
        buttons.add(clear);
        buttons.add(guide);
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
