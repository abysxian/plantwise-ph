package com.agristep.domain;

/**
 * A reusable step in a crop's care plan ("on day 14, feed with compost").
 * When a crop is planted, each template becomes a real {@link CareTask} for that bed.
 */
public class TaskTemplate {
    private final TaskKind kind;
    private final int dayOffset;
    /** Depth in cm for sowing, litres for watering. */
    private final Double amount;
    private final String detail;
    private final String extra;

    public TaskTemplate(TaskKind kind, int dayOffset, Double amount, String detail, String extra) {
        this.kind = kind;
        this.dayOffset = dayOffset;
        this.amount = amount;
        this.detail = detail;
        this.extra = extra;
    }

    public TaskKind getKind() { return kind; }
    public int getDayOffset() { return dayOffset; }
    public Double getAmount() { return amount; }
    public String getDetail() { return detail; }
    public String getExtra() { return extra; }
}
