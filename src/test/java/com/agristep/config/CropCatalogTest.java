package com.agristep.config;

import com.agristep.domain.Crop;
import com.agristep.domain.TaskKind;
import com.agristep.domain.TaskTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the data file itself: every crop must be complete and follow the rules the screens rely on. */
class CropCatalogTest {
    static List<Crop> crops;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = CropCatalogTest.class.getResourceAsStream("/data/crops.json")) {
            crops = CropCatalog.load(in);
        }
    }

    private static Crop crop(String code) {
        return crops.stream().filter(c -> c.getCode().equals(code)).findFirst().orElseThrow();
    }

    @Test void hasThirtySixCropsWithUniqueCodesAndNames() {
        assertEquals(36, crops.size());
        Set<String> codes = new HashSet<>();
        Set<String> names = new HashSet<>();
        crops.forEach(c -> {
            assertTrue(codes.add(c.getCode()), "duplicate code " + c.getCode());
            assertTrue(names.add(c.getName()), "duplicate name " + c.getName());
        });
    }

    @Test void everyCropHasTheFieldsTheScreensShow() {
        for (Crop c : crops) {
            String who = c.getCode();
            assertFalse(c.getName().isBlank(), who);
            assertFalse(c.getEnglishName().isBlank(), who);
            assertFalse(c.getScientificName().isBlank(), who);
            assertFalse(c.getEmoji().isBlank(), who);
            assertFalse(c.getSunlight().isBlank(), who);
            assertFalse(c.getSpacing().isBlank(), who);
            assertTrue(c.getCycleDays() >= 20, who);
            assertFalse(c.getOptimalSoils().isEmpty(), who);
            assertTrue(c.getTolerableSoils().containsAll(c.getOptimalSoils()), who + ": tolerable must include optimal");
            assertTrue(c.careTips().size() >= 3, who + " needs care tips");
            assertTrue(c.pestTips().size() >= 2, who + " needs pest tips");
        }
    }

    @Test void careplanStartsWithSowingAndEndsWithHarvestOnTheLastDay() {
        for (Crop c : crops) {
            List<TaskTemplate> plan = c.getTaskTemplates();
            assertEquals(TaskKind.SOWING, plan.get(0).getKind(), c.getCode());
            assertEquals(0, plan.get(0).getDayOffset(), c.getCode());
            TaskTemplate last = plan.get(plan.size() - 1);
            assertEquals(TaskKind.HARVEST, last.getKind(), c.getCode());
            assertEquals(c.getCycleDays(), last.getDayOffset(), c.getCode());
            for (int i = 1; i < plan.size(); i++) {
                assertTrue(plan.get(i).getDayOffset() >= plan.get(i - 1).getDayOffset(), c.getCode() + " not sorted");
            }
            TaskTemplate pest = plan.stream().filter(t -> t.getKind() == TaskKind.PEST_CONTROL).findFirst()
                    .orElseThrow(() -> new AssertionError(c.getCode() + " has no pest step"));
            assertFalse(pest.getExtra() == null || pest.getExtra().isBlank(), c.getCode() + " pest step needs a remedy");
        }
    }

    @Test void onlyTalongIsMarkedVerifiedAndTheOthersSaySo() {
        for (Crop c : crops) {
            if (c.getCode().equals("talong")) {
                assertTrue(c.isVerified());
                assertTrue(c.getSourceUrl().contains("id=1965"));
                assertEquals(75, c.getCycleDays());
                assertEquals(9, c.getTemperature().getAbsMin());
                assertEquals(40, c.getTemperature().getAbsMax());
            } else {
                assertFalse(c.isVerified(), c.getCode() + " must not claim FAO verification");
                assertTrue(c.getSourceNote().contains("Not yet checked against the FAO EcoCrop datasheet"), c.getCode());
                assertFalse(c.getSourceUrl().isBlank(), c.getCode());
            }
        }
    }

    @Test void coolWeatherCropsDoNotFitHotLowlands() {
        for (String code : List.of("repolyo", "carrot", "broccoli", "kuliplor")) {
            assertTrue(crop(code).getTemperature().fit(28) < 0.5, code + " should struggle at 28 C");
        }
    }

    @Test void heatLovingCropsFitHotLowlands() {
        for (String code : List.of("okra", "sili", "kundol", "kangkong")) {
            assertEquals(1.0, crop(code).getTemperature().fit(28), 1e-9, code);
        }
    }

    @Test void additionsAreInTheCatalogue() {
        for (String code : List.of("pechay", "sibuyas", "bawang", "gabi", "luya", "mais", "kamote", "wombok", "spinach", "bataw")) {
            assertNotNull(crop(code));
        }
    }
}
