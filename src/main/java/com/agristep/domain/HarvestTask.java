package com.agristep.domain;

import java.time.LocalDate;

public class HarvestTask extends CareTask {
    public HarvestTask(int dayOffset, LocalDate plantedOn, Double amount, String readySigns, String howToPick) {
        super(dayOffset, plantedOn, amount, readySigns, howToPick);
    }

    @Override public TaskKind getKind() { return TaskKind.HARVEST; }

    @Override public String getInstructions() {
        return join("Ready when " + detail() + ".", extra());
    }
}
