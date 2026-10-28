package com.agristep.ui;

import com.agristep.domain.Crop;

/** What a screen can ask the main window to do. */
interface Navigator {
    String TODAY = "today";
    String GARDEN = "garden";
    String CROPS = "crops";
    String LOCATION = "location";
    String CROP_DETAIL = "crop";
    String CALENDAR = "calendar";
    String JOURNAL = "journal";
    String INSIGHTS = "insights";
    String ACCOUNT = "account";

    void show(String page);

    void showCrop(Crop crop);

    /** Opens the journal with the given bed already chosen for a new note. */
    void showJournalFor(Long plotId);

    void signOut();

    /** Rebuilds every screen (after the language changes) and opens the given one. */
    void restart(String page);

    /** A short message at the top of the window that fades away after a few seconds. */
    void flash(String message, boolean error);
}
