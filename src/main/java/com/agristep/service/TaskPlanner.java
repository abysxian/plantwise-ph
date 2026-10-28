package com.agristep.service;

import com.agristep.domain.CareTask;
import com.agristep.domain.Crop;
import com.agristep.domain.TaskTemplate;

import java.time.LocalDate;
import java.util.List;

/** Turns a crop's care-plan templates into scheduled tasks for a bed planted on a given date. */
public class TaskPlanner {

    public List<CareTask> plan(Crop crop, LocalDate plantedOn) {
        return crop.getTaskTemplates().stream()
                .map(t -> toTask(t, plantedOn))
                .toList();
    }

    private CareTask toTask(TaskTemplate t, LocalDate plantedOn) {
        return t.getKind().create(t.getDayOffset(), plantedOn, t.getAmount(), t.getDetail(), t.getExtra());
    }
}
