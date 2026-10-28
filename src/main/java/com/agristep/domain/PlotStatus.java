package com.agristep.domain;

public enum PlotStatus {
    EMPTY("Empty bed", "empty"),
    PLANTED("Just planted", "planted"),
    GROWING("Growing", "growing"),
    READY("Ready to harvest", "ready");

    private final String label;
    private final String css;

    PlotStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() { return label; }
    public String getCss() { return css; }
}
