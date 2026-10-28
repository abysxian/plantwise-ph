package com.agristep.config;

import com.agristep.domain.ClimateRange;
import com.agristep.domain.Crop;
import com.agristep.domain.SoilType;
import com.agristep.domain.TaskKind;
import com.agristep.domain.TipCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Reads the crop seed file (data/crops.json) and builds {@link Crop} objects from it.
 * Keeping crop data in a file, not in code, means new crops can be added without touching Java.
 */
public final class CropCatalog {
    private CropCatalog() {}

    record RangeSeed(double absMin, double optMin, double optMax, double absMax) {
        ClimateRange toRange() { return new ClimateRange(absMin, optMin, optMax, absMax); }
    }

    record TaskSeed(TaskKind kind, int day, Double amount, String detail, String extra) {}

    record CropSeed(String code, String name, String englishName, String scientificName, String emoji,
                    int cycleDays, String sunlight, String spacing, boolean verified,
                    String sourceNote, String sourceUrl,
                    RangeSeed temperature, RangeSeed rainfall, RangeSeed ph,
                    List<SoilType> optimalSoils, List<SoilType> tolerableSoils,
                    List<String> careTips, List<String> pestTips, List<TaskSeed> tasks) {

        Crop toCrop() {
            Crop crop = new Crop(code, name, englishName, scientificName, emoji, cycleDays,
                    temperature.toRange(), rainfall.toRange(), ph.toRange())
                    .withSoils(EnumSet.copyOf(optimalSoils), EnumSet.copyOf(tolerableSoils))
                    .withGrowingNotes(sunlight, spacing)
                    .withSource(verified, sourceNote, sourceUrl);
            careTips.forEach(t -> crop.addTip(TipCategory.CARE, t));
            pestTips.forEach(t -> crop.addTip(TipCategory.PEST, t));
            tasks.forEach(t -> crop.addTaskTemplate(t.kind(), t.day(), t.amount(), t.detail(), t.extra()));
            return crop;
        }
    }

    public static List<Crop> load(InputStream json) throws IOException {
        List<CropSeed> seeds = new ObjectMapper().readValue(json, new TypeReference<List<CropSeed>>() {});
        List<Crop> crops = new ArrayList<>();
        seeds.forEach(s -> crops.add(s.toCrop()));
        return crops;
    }
}
