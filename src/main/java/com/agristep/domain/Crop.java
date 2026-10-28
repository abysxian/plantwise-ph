package com.agristep.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A crop the app knows how to grow, with the conditions it needs. Saved in the crops table. */
public class Crop {
    private Long id;
    private final String code;
    private final String name;
    private final String englishName;
    private final String scientificName;
    private final String emoji;
    /** Days from sowing to the first harvest. */
    private final int cycleDays;
    private String sunlight;
    private String spacing;
    /** True when the ranges were checked against an FAO EcoCrop datasheet. */
    private boolean verified;
    private String sourceNote;
    private String sourceUrl;
    private final ClimateRange temperature;
    private final ClimateRange rainfall;
    private final ClimateRange ph;
    private Set<SoilType> optimalSoils = new HashSet<>();
    private Set<SoilType> tolerableSoils = new HashSet<>();
    private final List<CropTip> tips = new ArrayList<>();
    private final List<TaskTemplate> taskTemplates = new ArrayList<>();

    public Crop(String code, String name, String englishName, String scientificName, String emoji,
                int cycleDays, ClimateRange temperature, ClimateRange rainfall, ClimateRange ph) {
        this.code = code;
        this.name = name;
        this.englishName = englishName;
        this.scientificName = scientificName;
        this.emoji = emoji;
        this.cycleDays = cycleDays;
        this.temperature = temperature;
        this.rainfall = rainfall;
        this.ph = ph;
    }

    // ---- fluent configuration used by the loader, the database and tests ----

    public Crop withSoils(Set<SoilType> optimal, Set<SoilType> tolerable) {
        this.optimalSoils = new HashSet<>(optimal);
        this.tolerableSoils = new HashSet<>(tolerable);
        this.tolerableSoils.addAll(optimal);   // an ideal soil is also an acceptable one
        return this;
    }

    public Crop withGrowingNotes(String sunlight, String spacing) {
        this.sunlight = sunlight;
        this.spacing = spacing;
        return this;
    }

    public Crop withSource(boolean verified, String note, String url) {
        this.verified = verified;
        this.sourceNote = note;
        this.sourceUrl = url;
        return this;
    }

    public Crop addTip(TipCategory category, String content) {
        tips.add(new CropTip(category, content));
        return this;
    }

    public Crop addTaskTemplate(TaskKind kind, int dayOffset, Double amount, String detail, String extra) {
        taskTemplates.add(new TaskTemplate(kind, dayOffset, amount, detail, extra));
        return this;
    }

    /** Called by the database layer once, when the row is first saved. */
    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("This crop already has id " + this.id);
        }
        this.id = id;
    }

    // ---- behaviour ----

    /** 1.0 for an ideal soil, 0.5 for an acceptable one, 0.0 otherwise. */
    public double soilFit(SoilType soil) {
        if (optimalSoils.contains(soil)) return 1.0;
        if (tolerableSoils.contains(soil)) return 0.5;
        return 0.0;
    }

    public String displayName() {
        return englishName == null || englishName.isBlank() ? name : name + " (" + englishName + ")";
    }

    /** All tips with their category, in the order they were added. */
    public List<CropTip> getTips() { return Collections.unmodifiableList(tips); }

    public List<String> careTips() { return tipsOf(TipCategory.CARE); }

    public List<String> pestTips() { return tipsOf(TipCategory.PEST); }

    private List<String> tipsOf(TipCategory category) {
        return tips.stream().filter(t -> t.getCategory() == category).map(CropTip::getContent).toList();
    }

    /** The care plan in schedule order. */
    public List<TaskTemplate> getTaskTemplates() {
        return taskTemplates.stream().sorted(Comparator.comparingInt(TaskTemplate::getDayOffset)).toList();
    }

    // ---- getters ----

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getEnglishName() { return englishName; }
    public String getScientificName() { return scientificName; }
    public String getEmoji() { return emoji; }
    public int getCycleDays() { return cycleDays; }
    public String getSunlight() { return sunlight; }
    public String getSpacing() { return spacing; }
    public boolean isVerified() { return verified; }
    public String getSourceNote() { return sourceNote; }
    public String getSourceUrl() { return sourceUrl; }
    public ClimateRange getTemperature() { return temperature; }
    public ClimateRange getRainfall() { return rainfall; }
    public ClimateRange getPh() { return ph; }
    public Set<SoilType> getOptimalSoils() { return Collections.unmodifiableSet(optimalSoils); }
    public Set<SoilType> getTolerableSoils() { return Collections.unmodifiableSet(tolerableSoils); }
}
