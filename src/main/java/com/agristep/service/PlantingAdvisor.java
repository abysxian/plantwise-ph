package com.agristep.service;

import com.agristep.domain.Crop;
import com.agristep.domain.HarvestRecord;
import com.agristep.domain.Plot;
import com.agristep.repository.CropRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions shown when choosing a crop for a bed: how it gets on with the plants in the beds next to it
 * (companion planting) and whether the bed grew the same crop or crop family recently (crop rotation).
 */
public class PlantingAdvisor {
    static final int ROTATION_DAYS = 365;

    private final CropRepository crops;

    public PlantingAdvisor(CropRepository crops) {
        this.crops = crops;
    }

    public enum Kind { GOOD, AVOID, ROTATE }

    public record Hint(Kind kind, String text) {}

    public List<Hint> hintsFor(Crop crop, Plot target, List<Plot> allPlots, int gridColumns,
                               List<HarvestRecord> history, LocalDate today) {
        List<Hint> hints = new ArrayList<>();
        String family = CropRelations.familyOf(crop.getCode());

        if (family != null) {
            for (Plot other : neighbours(target, allPlots, gridColumns)) {
                if (other.isEmpty()) continue;
                String otherFamily = CropRelations.familyOf(other.getCrop().getCode());
                if (otherFamily == null) continue;
                String where = other.getName() + " (" + other.getCrop().getName() + ")";
                if (CropRelations.goodTogether(family, otherFamily)) {
                    hints.add(new Hint(Kind.GOOD, crop.getName() + " usually grows well next to " + where + "."));
                } else if (CropRelations.badTogether(family, otherFamily)) {
                    hints.add(new Hint(Kind.AVOID, crop.getName() + " and " + where
                            + " are often kept apart. Consider another bed if you have one."));
                }
            }
        }

        for (HarvestRecord past : history) {
            if (!past.getPlotName().equals(target.getName())) continue;
            long daysAgo = ChronoUnit.DAYS.between(past.getHarvestedOn(), today);
            if (daysAgo > ROTATION_DAYS) continue;
            if (past.getCropName().equalsIgnoreCase(crop.getName())) {
                hints.add(new Hint(Kind.ROTATE, "This bed grew " + past.getCropName() + " " + describeAgo(daysAgo)
                        + ". Planting the same crop again invites pests and disease. Rotate to a different family if you can."));
            } else if (family != null && family.equals(familyOfName(past.getCropName()))) {
                hints.add(new Hint(Kind.ROTATE, "This bed grew " + past.getCropName() + " " + describeAgo(daysAgo)
                        + ", the same plant family as " + crop.getName() + ". Rotating families keeps the soil healthier."));
            }
        }
        return hints;
    }

    /** Beds touching this one, including diagonally, on a grid with the given number of columns. */
    static List<Plot> neighbours(Plot target, List<Plot> all, int columns) {
        int cols = Math.max(1, columns);
        int row = (target.getSlotNumber() - 1) / cols;
        int col = (target.getSlotNumber() - 1) % cols;
        List<Plot> result = new ArrayList<>();
        for (Plot p : all) {
            if (p.getSlotNumber() == target.getSlotNumber()) continue;
            int r = (p.getSlotNumber() - 1) / cols;
            int c = (p.getSlotNumber() - 1) % cols;
            if (Math.abs(r - row) <= 1 && Math.abs(c - col) <= 1) result.add(p);
        }
        return result;
    }

    /** Harvest history keeps the crop's name, so find the crop in the catalogue to learn its family. */
    private String familyOfName(String cropName) {
        return crops.findAll().stream().filter(c -> c.getName().equalsIgnoreCase(cropName))
                .map(c -> CropRelations.familyOf(c.getCode())).findFirst().orElse(null);
    }

    private static String describeAgo(long days) {
        if (days < 31) return "within the last month";
        return "about " + Math.round(days / 30.0) + " months ago";
    }
}
