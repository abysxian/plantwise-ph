package com.agristep.ui;

import javax.swing.JPanel;
import java.awt.Component;

/** The data-source credits shown at the bottom of every screen. */
final class Footer {
    private Footer() {}

    static final String CREDITS = "Weather and climate data: Open-Meteo.com (CC BY 4.0). "
            + "Crop ranges: FAO EcoCrop where marked, otherwise typical garden guidance. "
            + "Emoji pictures: Twemoji (CC BY 4.0).";

    static Component create() {
        JPanel box = new JPanel(new StackLayout(0));
        box.setOpaque(false);
        box.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 4, 0, 4));
        box.add(WrapLabel.small(CREDITS).align(WrapLabel.CENTER));
        return box;
    }
}
