package com.agristep.ui;

import com.agristep.domain.Crop;

/** What a screen can ask the main window to do. */
interface Navigator {
    String TODAY = "today";
    String GARDEN = "garden";
    String CROPS = "crops";
    String LOCATION = "location";
    String CROP_DETAIL = "crop";

    void show(String page);

    void showCrop(Crop crop);

    /** A short message at the top of the window that fades away after a few seconds. */
    void flash(String message, boolean error);
}
