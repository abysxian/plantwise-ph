package com.agristep.service;

import com.agristep.domain.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Saves the garden's records as files the person can keep, print or open in a spreadsheet. */
public class ExportService {
    private final GardenService garden;
    private final SettingsService settings;
    private final Session session;
    private final Clock clock;

    public ExportService(GardenService garden, SettingsService settings, Session session, Clock clock) {
        this.garden = garden;
        this.settings = settings;
        this.session = session;
        this.clock = clock;
    }

    /** The whole harvest history as a CSV file (opens in Excel). */
    public int harvestsToCsv(Path file) {
        List<HarvestRecord> records = garden.allHarvests();
        StringBuilder csv = new StringBuilder("﻿Crop,Bed,Planted on,Harvested on,Days grown,Care plan completed (%)\r\n");
        for (HarvestRecord r : records) {
            csv.append(cell(r.getCropName())).append(',').append(cell(r.getPlotName())).append(',')
                    .append(r.getPlantedOn()).append(',').append(r.getHarvestedOn()).append(',')
                    .append(r.daysGrown()).append(',').append(r.getCompletionPercent()).append("\r\n");
        }
        write(file, csv.toString().getBytes(StandardCharsets.UTF_8));
        return records.size();
    }

    /** Every care task for every planted bed as a CSV file. */
    public int careTasksToCsv(Path file) {
        StringBuilder csv = new StringBuilder("﻿Bed,Crop,Task,Due date,Done,Instructions\r\n");
        int rows = 0;
        for (Plot plot : garden.allPlots()) {
            if (plot.isEmpty()) continue;
            for (CareTask t : plot.getTasks()) {
                csv.append(cell(plot.getName())).append(',').append(cell(plot.getCrop().getName())).append(',')
                        .append(cell(t.getTitle())).append(',').append(t.getDueDate()).append(',')
                        .append(t.isCompleted() ? "yes" : "no").append(',').append(cell(t.getInstructions())).append("\r\n");
                rows++;
            }
        }
        write(file, csv.toString().getBytes(StandardCharsets.UTF_8));
        return rows;
    }

    /** A printable PDF: the garden, each bed with its care plan, and the harvest history. */
    public void reportToPdf(Path file) {
        LocalDate today = LocalDate.now(clock);
        GardenSettings s = settings.current();
        SimplePdf pdf = new SimplePdf();
        pdf.title("PlantWise PH - Garden report");
        pdf.body("Prepared for " + session.require().getDisplayName() + " on " + today);
        pdf.body("Location: " + s.getLocationName() + "  |  Soil: " + s.getSoil().name().toLowerCase().replace('_', ' ')
                + "  |  Grid: " + s.getGridColumns() + " columns");
        pdf.blank();

        pdf.heading("Beds");
        for (Plot plot : garden.allPlots()) {
            if (plot.isEmpty()) {
                pdf.body(plot.getName() + ": empty");
                continue;
            }
            pdf.body(plot.getName() + ": " + plot.getCrop().getName() + " (" + plot.getCrop().getEnglishName() + "), planted "
                    + plot.getPlantedOn() + ", " + plot.progressPercent() + "% of the care plan done, " + plot.status(today).getLabel());
        }
        pdf.blank();

        pdf.heading("Care plans");
        boolean any = false;
        for (Plot plot : garden.allPlots()) {
            if (plot.isEmpty()) continue;
            any = true;
            pdf.body(plot.getName() + " - " + plot.getCrop().getName());
            for (CareTask t : plot.getTasks()) {
                pdf.body("   [" + (t.isCompleted() ? "x" : " ") + "] " + t.getDueDate() + "  " + t.getTitle() + ": " + t.getInstructions());
            }
            pdf.blank();
        }
        if (!any) {
            pdf.body("No bed is planted yet.");
            pdf.blank();
        }

        pdf.heading("Harvest history");
        List<HarvestRecord> history = garden.allHarvests();
        if (history.isEmpty()) pdf.body("No harvests recorded yet.");
        for (HarvestRecord r : history) {
            pdf.body(r.getHarvestedOn() + "  " + r.getCropName() + " from " + r.getPlotName() + " - " + r.daysGrown()
                    + " days, " + r.getCompletionPercent() + "% of the care plan done");
        }
        try {
            pdf.write(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the PDF: " + e.getMessage(), e);
        }
    }

    private static void write(Path file, byte[] data) {
        try {
            Files.write(file, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the file: " + e.getMessage(), e);
        }
    }

    /** CSV cell with quotes, and a leading apostrophe on anything a spreadsheet could read as a formula. */
    static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
