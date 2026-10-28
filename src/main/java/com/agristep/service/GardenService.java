package com.agristep.service;

import com.agristep.domain.*;
import com.agristep.repository.CareTaskRepository;
import com.agristep.repository.CropRepository;
import com.agristep.repository.HarvestRecordRepository;
import com.agristep.repository.PlotRepository;
import com.agristep.repository.Transactions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Everything the user can do to their garden: plant, tick tasks, harvest, add beds. */
public class GardenService {

    private final PlotRepository plots;
    private final CropRepository crops;
    private final CareTaskRepository tasks;
    private final HarvestRecordRepository harvests;
    private final TaskPlanner planner;
    private final Clock clock;
    private final Transactions transactions;

    public GardenService(PlotRepository plots, CropRepository crops, CareTaskRepository tasks,
                         HarvestRecordRepository harvests, TaskPlanner planner, Clock clock,
                         Transactions transactions) {
        this.plots = plots;
        this.crops = crops;
        this.tasks = tasks;
        this.harvests = harvests;
        this.planner = planner;
        this.clock = clock;
        this.transactions = transactions;
    }

    public LocalDate today() { return LocalDate.now(clock); }

    public List<Plot> allPlots() { return plots.findAllByOrderBySlotNumberAsc(); }

    public Plot getPlot(Long id) {
        return plots.findById(id).orElseThrow(() -> new NotFoundException("That bed does not exist."));
    }

    public Plot plant(Long plotId, Long cropId, LocalDate plantedOn) {
        Plot plot = getPlot(plotId);
        Crop crop = crops.findById(cropId).orElseThrow(() -> new NotFoundException("That crop does not exist."));
        LocalDate date = plantedOn == null ? today() : plantedOn;
        if (date.isAfter(today())) {
            throw new IllegalArgumentException("The planting date can't be in the future.");
        }
        plot.plant(crop, date, planner.plan(crop, date));
        return plots.save(plot);
    }

    /** Ticks or un-ticks one task on a bed. */
    public CareTask toggleTask(Long plotId, Long taskId) {
        CareTask task = getPlot(plotId).getTasks().stream()
                .filter(t -> taskId.equals(t.getId())).findFirst()
                .orElseThrow(() -> new NotFoundException("That task does not exist."));
        task.toggle(LocalDateTime.now(clock));
        return tasks.save(task);
    }

    /** Logs the harvest and empties the bed, as one all-or-nothing step. */
    public HarvestRecord harvest(Long plotId) {
        return transactions.run(() -> {
            Plot plot = getPlot(plotId);
            HarvestRecord record = plot.harvest(today());
            plots.save(plot);
            return harvests.save(record);
        });
    }

    public void clear(Long plotId) {
        Plot plot = getPlot(plotId);
        plot.clear();
        plots.save(plot);
    }

    public Plot addPlot() {
        int next = plots.findTopByOrderBySlotNumberDesc().map(p -> p.getSlotNumber() + 1).orElse(1);
        return plots.save(new Plot(next));
    }

    public List<HarvestRecord> recentHarvests() { return harvests.findTop10ByOrderByHarvestedOnDescIdDesc(); }

    public List<HarvestRecord> allHarvests() {
        return harvests.findAll().stream()
                .sorted(java.util.Comparator.comparing(HarvestRecord::getHarvestedOn).reversed()
                        .thenComparing(HarvestRecord::getId, java.util.Comparator.reverseOrder()))
                .toList();
    }

    /** A care task that is overdue or coming up soon. {@code daysAway} is negative when overdue. */
    public record Reminder(Plot plot, CareTask task, long daysAway) {}

    /** Unfinished tasks that are overdue or due within the next {@code days} days, soonest first. */
    public List<Reminder> reminders(int days) {
        LocalDate today = today();
        LocalDate last = today.plusDays(days);
        List<Reminder> found = new java.util.ArrayList<>();
        for (Plot plot : plots.findAllByOrderBySlotNumberAsc()) {
            if (plot.isEmpty()) continue;
            for (CareTask t : plot.getTasks()) {
                if (!t.isCompleted() && !t.getDueDate().isAfter(last)) {
                    found.add(new Reminder(plot, t, java.time.temporal.ChronoUnit.DAYS.between(today, t.getDueDate())));
                }
            }
        }
        found.sort(java.util.Comparator.comparingLong(Reminder::daysAway).thenComparing(r -> r.plot().getSlotNumber()));
        return found;
    }

    public long dueTaskCount() { return tasks.countByCompletedFalseAndDueDateLessThanEqual(today()); }

    public long readyPlotCount() {
        LocalDate today = today();
        return plots.findAllByOrderBySlotNumberAsc().stream()
                .filter(p -> p.status(today) == PlotStatus.READY)
                .count();
    }
}
