package com.agristep.repository;

import com.agristep.domain.Plot;

import java.util.List;
import java.util.Optional;

public interface PlotRepository extends Repository<Plot, Long> {
    List<Plot> findAllByOrderBySlotNumberAsc();

    Optional<Plot> findTopByOrderBySlotNumberDesc();

    /** Removes a bed together with its care tasks. */
    void delete(Plot plot);
}
