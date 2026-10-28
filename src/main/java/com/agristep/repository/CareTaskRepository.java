package com.agristep.repository;

import com.agristep.domain.CareTask;

import java.time.LocalDate;

public interface CareTaskRepository {
    /** Saves a task's completion state. Tasks are created and removed through their {@code Plot}. */
    CareTask save(CareTask task);

    long countByCompletedFalseAndDueDateLessThanEqual(LocalDate date);
}
