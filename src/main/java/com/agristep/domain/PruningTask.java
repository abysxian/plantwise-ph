package com.agristep.domain;

import java.time.LocalDate;

public class PruningTask extends CareTask {
    public PruningTask(int dayOffset, LocalDate plantedOn, Double amount, String detail, String extra) {
        super(dayOffset, plantedOn, amount, detail, extra);
    }

    @Override public TaskKind getKind() { return TaskKind.PRUNING; }

    @Override public String getInstructions() { return join(detail(), extra()); }
}
