package com.agristep.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A permanent log entry written when a bed is harvested. Text is copied so it survives bed changes. */
public class HarvestRecord {
    private Long id;
    private final String cropName;
    private final String cropEmoji;
    private final String plotName;
    private final LocalDate plantedOn;
    private final LocalDate harvestedOn;
    private final int completionPercent;

    public HarvestRecord(String cropName, String cropEmoji, String plotName,
                         LocalDate plantedOn, LocalDate harvestedOn, int completionPercent) {
        this.cropName = cropName;
        this.cropEmoji = cropEmoji;
        this.plotName = plotName;
        this.plantedOn = plantedOn;
        this.harvestedOn = harvestedOn;
        this.completionPercent = completionPercent;
    }

    public long daysGrown() { return ChronoUnit.DAYS.between(plantedOn, harvestedOn); }

    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("This record already has id " + this.id);
        }
        this.id = id;
    }

    public Long getId() { return id; }
    public String getCropName() { return cropName; }
    public String getCropEmoji() { return cropEmoji; }
    public String getPlotName() { return plotName; }
    public LocalDate getPlantedOn() { return plantedOn; }
    public LocalDate getHarvestedOn() { return harvestedOn; }
    public int getCompletionPercent() { return completionPercent; }
}
