package com.agristep.service;

import com.agristep.domain.ValidationException;

import java.util.List;

/** Shrinking the grid would remove beds that have plants in them, so the person must confirm first. */
public class GridResizeException extends ValidationException {
    private final List<String> plantedBeds;

    public GridResizeException(List<String> plantedBeds) {
        super(message(plantedBeds));
        this.plantedBeds = List.copyOf(plantedBeds);
    }

    public List<String> getPlantedBeds() { return plantedBeds; }

    private static String message(List<String> beds) {
        String list = beds.size() <= 3 ? String.join(", ", beds) : String.join(", ", beds.subList(0, 3)) + " and " + (beds.size() - 3) + " more";
        return "This smaller grid removes " + (beds.size() == 1 ? "a bed" : beds.size() + " beds")
                + " with plants in them (" + list + "). Their care plans will be deleted.";
    }
}
