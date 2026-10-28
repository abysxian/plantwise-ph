package com.agristep.domain;

import com.agristep.weather.WeatherForecast;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract base class for every job on a bed's checklist.
 * <p>
 * Inheritance: six subclasses share this state, saved in one table with a {@code task_type} column.<br>
 * Polymorphism: each subclass overrides {@link #getInstructions()} and {@link #weatherHint}.<br>
 * Encapsulation: fields are private; completion changes only through {@link #toggle}.
 */
public abstract class CareTask {
    private Long id;
    private Plot plot;
    private final int dayOffset;
    private final LocalDate dueDate;
    private final Double amount;
    private final String detail;
    private final String extra;
    private boolean completed;
    private LocalDateTime completedAt;

    protected CareTask(int dayOffset, LocalDate plantedOn, Double amount, String detail, String extra) {
        this.dayOffset = dayOffset;
        this.dueDate = plantedOn.plusDays(dayOffset);
        this.amount = amount;
        this.detail = detail;
        this.extra = extra;
    }

    /** Rebuilds a task that was saved earlier, keeping its stored due date and completion. */
    public static CareTask restore(Long id, TaskKind kind, int dayOffset, LocalDate dueDate, Double amount,
                                   String detail, String extra, boolean completed, LocalDateTime completedAt) {
        CareTask task = kind.create(dayOffset, dueDate.minusDays(dayOffset), amount, detail, extra);
        task.id = id;
        task.completed = completed;
        task.completedAt = completedAt;
        return task;
    }

    // ---- what each subclass must provide ----

    public abstract TaskKind getKind();

    /** The full, plain-language instruction shown on the checklist. */
    public abstract String getInstructions();

    /** Optional tip based on the forecast. Most tasks have none. */
    public Optional<String> weatherHint(WeatherForecast forecast) {
        return Optional.empty();
    }

    // ---- shared behaviour ----

    public String getTitle() { return getKind().getTitle(); }

    public String getIcon() { return getKind().getIcon(); }

    public void toggle(LocalDateTime now) {
        this.completed = !this.completed;
        this.completedAt = this.completed ? now : null;
    }

    public boolean isOverdue(LocalDate today) {
        return !completed && dueDate.isBefore(today);
    }

    void assignTo(Plot plot) { this.plot = plot; }

    /** Called by the database layer once, when the row is first saved. */
    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("This task already has id " + this.id);
        }
        this.id = id;
    }

    // ---- helpers for subclasses ----

    protected static String join(String... parts) {
        return Stream.of(parts)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(" "));
    }

    protected static String number(Double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    protected Double amount() { return amount; }
    protected String detail() { return detail; }
    protected String extra() { return extra; }

    // ---- getters ----

    public Long getId() { return id; }
    public Double getAmount() { return amount; }
    public String getDetail() { return detail; }
    public String getExtra() { return extra; }
    public Plot getPlot() { return plot; }
    public int getDayOffset() { return dayOffset; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isCompleted() { return completed; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
