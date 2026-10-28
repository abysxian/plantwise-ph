package com.agristep.domain;

/** One short piece of advice for a crop (general care or pest control). */
public class CropTip {
    private final TipCategory category;
    private final String content;

    public CropTip(TipCategory category, String content) {
        this.category = category;
        this.content = content;
    }

    public TipCategory getCategory() { return category; }
    public String getContent() { return content; }
}
