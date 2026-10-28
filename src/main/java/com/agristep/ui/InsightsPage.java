package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.HarvestRecord;
import com.agristep.service.HarvestStats;
import com.agristep.service.PlantingCalendar;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;

/** How the harvests are going: totals, a monthly chart, the favourite crops, and files to save or print. */
final class InsightsPage extends Page {
    enum Export { HARVESTS_CSV, TASKS_CSV, REPORT_PDF }

    private int generation;

    InsightsPage(AppContext ctx, Navigator nav) { super(ctx, nav); }

    @Override void onShow() {
        int mine = ++generation;
        setContent(TodayPage.loadingCard("Counting your harvests…"));
        Tasks.run(() -> HarvestStats.summarise(ctx.garden.allHarvests(), ctx.garden.today(), 12), summary -> {
            if (mine == generation) render(summary);
        }, this::fail);
    }

    private void render(HarvestStats.Summary s) {
        Card header = banner("Your harvests", s.total() == 0
                ? "Harvest a bed from the Garden screen and your results will be charted here."
                : s.total() + (s.total() == 1 ? " harvest" : " harvests") + " recorded.", Scenery.Photo.HOUSEPLANTS);
        JPanel parts = new JPanel(new StackLayout(12));
        parts.setOpaque(false);

        JPanel tiles = new JPanel(new FlowGridLayout(150, 3, 10, 0));
        tiles.setOpaque(false);
        tiles.add(tile(String.valueOf(s.total()), "harvests"));
        tiles.add(tile(s.total() == 0 ? "–" : Math.round(s.averageDaysToHarvest()) + "", "days from planting to harvest, on average"));
        tiles.add(tile(s.topCrop() == null ? "–" : s.topCrop(), "most harvested crop"));
        parts.add(tiles);

        Card chart = new Card().gap(8);
        chart.add(WrapLabel.h3("Harvests per month"));
        chart.add(s.total() == 0 ? WrapLabel.muted("Nothing to chart yet.") : new BarChart(s.perMonth()));
        parts.add(chart);

        if (!s.byCrop().isEmpty()) {
            Card crops = new Card().gap(8);
            crops.add(WrapLabel.h3("By crop"));
            int max = s.byCrop().get(0).count();
            for (HarvestStats.CropCount c : s.byCrop()) {
                JPanel row = new JPanel(new RowLayout(10, false));
                row.setOpaque(false);
                row.add(Emoji.label(c.emoji(), 26), RowLayout.WEST);
                JPanel mid = new JPanel(new StackLayout(2));
                mid.setOpaque(false);
                mid.add(WrapLabel.bold(c.cropName()));
                mid.add(new Meter((int) Math.round(c.count() * 100.0 / max), Theme.LEAF_2));
                row.add(mid, RowLayout.CENTER);
                row.add(WrapLabel.bold(String.valueOf(c.count())), RowLayout.EAST);
                crops.add(row);
            }
            parts.add(crops);
        }
        parts.add(exportCard());
        setContent(header, parts);
    }

    private static Card tile(String big, String caption) {
        Card card = new Card().gap(2).padding(14);
        card.add(new WrapLabel(big, Theme.H1, Theme.LEAF).maxLines(1));
        card.add(WrapLabel.small(caption));
        return card;
    }

    // ---- export ----

    private Card exportCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h3("Save or print your records"));
        card.add(WrapLabel.muted("CSV files open in Excel or Google Sheets. The PDF report lists your beds, care plans and "
                + "harvest history and can be printed."));
        JPanel buttons = new JPanel(new FlowGridLayout(170, 3, 8, 0));
        buttons.setOpaque(false);
        for (Export kind : Export.values()) {
            String label = switch (kind) {
                case HARVESTS_CSV -> "Export harvests (CSV)";
                case TASKS_CSV -> "Export care plan (CSV)";
                case REPORT_PDF -> "Garden report (PDF)";
            };
            PillButton b = PillButton.secondary(label);
            b.addActionListener(e -> chooseFile(kind));
            buttons.add(b);
        }
        card.add(buttons);
        return card;
    }

    private void chooseFile(Export kind) {
        JFileChooser chooser = new JFileChooser();
        boolean pdf = kind == Export.REPORT_PDF;
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.t(pdf ? "PDF file" : "CSV file"), pdf ? "pdf" : "csv"));
        chooser.setSelectedFile(new java.io.File(switch (kind) {
            case HARVESTS_CSV -> "plantwise-harvests.csv";
            case TASKS_CSV -> "plantwise-care-plan.csv";
            case REPORT_PDF -> "plantwise-garden-report.pdf";
        }));
        if (chooser.showSaveDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
            export(kind, chooser.getSelectedFile().toPath());
        }
    }

    void export(Export kind, Path file) {
        Tasks.run(() -> {
            switch (kind) {
                case HARVESTS_CSV -> ctx.export.harvestsToCsv(file);
                case TASKS_CSV -> ctx.export.careTasksToCsv(file);
                case REPORT_PDF -> ctx.export.reportToPdf(file);
            }
            return file.getFileName().toString();
        }, name -> nav.flash("Saved " + name + ".", false), this::fail);
    }

    // ---- chart ----

    /** Vertical bars, one per month. */
    static final class BarChart extends JComponent {
        private final Map<YearMonth, Integer> values;

        BarChart(Map<YearMonth, Integer> values) { this.values = values; }

        Map<YearMonth, Integer> values() { return values; }

        @Override public java.awt.Dimension getPreferredSize() { return new java.awt.Dimension(300, 190); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int top = 22, bottom = 24;
            int base = h - bottom;
            int max = Math.max(1, values.values().stream().mapToInt(Integer::intValue).max().orElse(1));
            int n = Math.max(1, values.size());
            double slot = (double) w / n;
            int barW = (int) Math.max(6, Math.min(36, slot * 0.6));
            YearMonth now = YearMonth.from(LocalDate.now());
            g2.setColor(Theme.LINE);
            g2.drawLine(0, base, w, base);
            int i = 0;
            for (Map.Entry<YearMonth, Integer> e : values.entrySet()) {
                int x = (int) (i * slot + (slot - barW) / 2);
                int barH = (int) Math.round((base - top) * (e.getValue() / (double) max));
                g2.setColor(e.getKey().equals(now) ? Theme.LEAF : Theme.LEAF_2);
                if (e.getValue() > 0) g2.fillRoundRect(x, base - barH, barW, barH, 6, 6);
                g2.setFont(Theme.SMALL_BOLD);
                g2.setColor(Theme.INK);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String count = String.valueOf(e.getValue());
                if (e.getValue() > 0) g2.drawString(count, x + (barW - fm.stringWidth(count)) / 2, base - barH - 4);
                g2.setFont(Theme.SMALL);
                g2.setColor(Theme.MUTED);
                fm = g2.getFontMetrics();
                String label = PlantingCalendar.shortMonthName(e.getKey().getMonthValue()).toUpperCase(Locale.ROOT).substring(0, 1)
                        + (slot >= 44 ? PlantingCalendar.shortMonthName(e.getKey().getMonthValue()).substring(1) : "");
                g2.drawString(label, (int) (i * slot + (slot - fm.stringWidth(label)) / 2), h - 7);
                i++;
            }
            g2.dispose();
        }
    }
}
