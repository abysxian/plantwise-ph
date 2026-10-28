package com.agristep.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One bed in the backyard grid.
 * Encapsulation: the crop and task list change only through plant(), harvest() and clear(),
 * so a bed can never be half-planted or hold tasks without a crop.
 */
public class Plot {
    private Long id;
    private final int slotNumber;
    private String name;
    private Crop crop;
    private LocalDate plantedOn;
    private final List<CareTask> tasks = new ArrayList<>();

    public Plot(int slotNumber) {
        this.slotNumber = slotNumber;
        this.name = "Bed " + slotNumber;
    }

    /** Rebuilds a bed that was saved earlier, with its crop (or none) and tasks. */
    public static Plot restore(Long id, int slotNumber, String name, Crop crop, LocalDate plantedOn,
                               List<CareTask> savedTasks) {
        Plot plot = new Plot(slotNumber);
        plot.id = id;
        plot.name = name;
        plot.crop = crop;
        plot.plantedOn = plantedOn;
        for (CareTask task : savedTasks) {
            task.assignTo(plot);
            plot.tasks.add(task);
        }
        return plot;
    }

    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("This bed already has id " + this.id);
        }
        this.id = id;
    }

    public boolean isEmpty() { return crop == null; }

    public void plant(Crop crop, LocalDate plantedOn, List<? extends CareTask> newTasks) {
        if (!isEmpty()) throw new PlotOccupiedException(name);
        this.crop = crop;
        this.plantedOn = plantedOn;
        for (CareTask task : newTasks) {
            task.assignTo(this);
            tasks.add(task);
        }
    }

    /** Logs the harvest and empties the bed. */
    public HarvestRecord harvest(LocalDate today) {
        if (isEmpty()) throw new IllegalStateException(name + " is empty, so there is nothing to harvest.");
        HarvestRecord record = new HarvestRecord(crop.getName(), crop.getEmoji(), name,
                plantedOn, today, progressPercent());
        clear();
        return record;
    }

    /** Empties the bed without logging a harvest. */
    public void clear() {
        this.crop = null;
        this.plantedOn = null;
        this.tasks.clear();
    }

    public int progressPercent() {
        if (tasks.isEmpty()) return 0;
        long done = tasks.stream().filter(CareTask::isCompleted).count();
        return (int) Math.round(done * 100.0 / tasks.size());
    }

    public PlotStatus status(LocalDate today) {
        if (isEmpty()) return PlotStatus.EMPTY;
        boolean harvestDue = tasks.stream()
                .anyMatch(t -> t instanceof HarvestTask && !t.getDueDate().isAfter(today));
        if (harvestDue) return PlotStatus.READY;
        boolean anyDone = tasks.stream().anyMatch(CareTask::isCompleted);
        return anyDone ? PlotStatus.GROWING : PlotStatus.PLANTED;
    }

    public Optional<CareTask> nextTask() {
        return tasks.stream().filter(t -> !t.isCompleted()).findFirst();
    }

    public Long getId() { return id; }
    public int getSlotNumber() { return slotNumber; }
    public String getName() { return name; }
    public Crop getCrop() { return crop; }
    public LocalDate getPlantedOn() { return plantedOn; }
    public List<CareTask> getTasks() { return Collections.unmodifiableList(tasks); }
}
