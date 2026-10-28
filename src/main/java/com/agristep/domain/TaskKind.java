package com.agristep.domain;

import java.time.LocalDate;

/**
 * The kinds of care task. Each constant knows how to build its own {@link CareTask} subclass
 * (Factory Method + polymorphism), so the planner never needs an if/else chain on the kind.
 */
public enum TaskKind {
    SOWING("Sow the seeds", "🌱") {
        @Override public CareTask create(int day, LocalDate planted, Double amount, String detail, String extra) {
            return new SowingTask(day, planted, amount, detail, extra);
        }
    },
    WATERING("Water the plants", "💧") {
        @Override public CareTask create(int day, LocalDate planted, Double amount, String detail, String extra) {
            return new WateringTask(day, planted, amount, detail, extra);
        }
    },
    FEEDING("Feed the soil", "🍂") {
        @Override public CareTask create(int day, LocalDate planted, Double amount, String detail, String extra) {
            return new FeedingTask(day, planted, amount, detail, extra);
        }
    },
    PRUNING("Prune and support", "✂️") {
        @Override public CareTask create(int day, LocalDate planted, Double amount, String detail, String extra) {
            return new PruningTask(day, planted, amount, detail, extra);
        }
    },
    PEST_CONTROL("Check for pests", "🐛") {
        @Override public CareTask create(int day, LocalDate planted, Double amount, String detail, String extra) {
            return new PestControlTask(day, planted, amount, detail, extra);
        }
    },
    HARVEST("Harvest", "🧺") {
        @Override public CareTask create(int day, LocalDate planted, Double amount, String detail, String extra) {
            return new HarvestTask(day, planted, amount, detail, extra);
        }
    };

    private final String title;
    private final String icon;

    TaskKind(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() { return title; }
    public String getIcon() { return icon; }

    public abstract CareTask create(int dayOffset, LocalDate plantedOn, Double amount, String detail, String extra);
}
