package com.agristep.service;

import com.agristep.domain.Plot;
import com.agristep.domain.ValidationException;
import com.agristep.repository.PlotRepository;
import com.agristep.repository.Transactions;

import java.util.ArrayList;
import java.util.List;

/** Lets a person choose how many rows and columns of beds their garden has. */
public class GardenGridService {
    public static final int MAX_ROWS = 10;
    public static final int MAX_COLUMNS = 8;

    private final PlotRepository plots;
    private final SettingsService settings;
    private final Transactions transactions;

    public GardenGridService(PlotRepository plots, SettingsService settings, Transactions transactions) {
        this.plots = plots;
        this.settings = settings;
        this.transactions = transactions;
    }

    /** The current layout. Beds fill the grid row by row; the last row may be partly filled in an older garden. */
    public GridShape shape() {
        int columns = Math.max(1, settings.current().getGridColumns());
        long beds = plots.count();
        int rows = (int) Math.max(1, (beds + columns - 1) / columns);
        return new GridShape(rows, columns);
    }

    /**
     * Makes the garden rows x columns beds. Growing adds empty beds at the end; shrinking removes beds from the end.
     * Shrinking never silently removes planted beds: it throws {@link GridResizeException} unless {@code confirmRemoval}.
     */
    public GridShape resize(int rows, int columns, boolean confirmRemoval) {
        if (rows < 1 || rows > MAX_ROWS) throw new ValidationException("Rows must be a whole number from 1 to " + MAX_ROWS + ".");
        if (columns < 1 || columns > MAX_COLUMNS) throw new ValidationException("Columns must be a whole number from 1 to " + MAX_COLUMNS + ".");
        return transactions.run(() -> {
            int total = rows * columns;
            List<Plot> all = plots.findAllByOrderBySlotNumberAsc();
            List<Plot> removing = new ArrayList<>();
            for (Plot p : all) if (p.getSlotNumber() > total) removing.add(p);
            List<String> planted = removing.stream().filter(p -> !p.isEmpty()).map(Plot::getName).toList();
            if (!planted.isEmpty() && !confirmRemoval) throw new GridResizeException(planted);
            removing.forEach(plots::delete);
            int highest = all.stream().filter(p -> p.getSlotNumber() <= total).mapToInt(Plot::getSlotNumber).max().orElse(0);
            for (int slot = Math.max(highest, all.size() - removing.size()) + 1; slot <= total; slot++) plots.save(new Plot(slot));
            var s = settings.current();
            s.useGridColumns(columns);
            settings.save(s);
            return new GridShape(rows, columns);
        });
    }
}
