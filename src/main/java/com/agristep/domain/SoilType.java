package com.agristep.domain;

/** The three garden soil types a backyard farmer can recognise by touch. */
public enum SoilType {
    LOAM("Soft garden soil", "Dark and crumbly. Easy to dig, holds water but still drains.", "🟫"),
    SANDY("Sandy soil", "Loose and gritty, like beach sand. Water drains away fast.", "🏖️"),
    CLAY("Sticky clay", "Heavy and muddy when wet. Holds water for a long time.", "🧱");

    private final String label;
    private final String description;
    private final String emoji;

    SoilType(String label, String description, String emoji) {
        this.label = label;
        this.description = description;
        this.emoji = emoji;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getEmoji() { return emoji; }
}
