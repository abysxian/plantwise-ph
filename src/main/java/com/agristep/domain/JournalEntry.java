package com.agristep.domain;

import java.time.LocalDate;

/** One note (and optionally one photo) in the garden journal. */
public class JournalEntry {
    public static final int MAX_NOTE = 2000;

    private Long id;
    private final String authorName;
    private final String plotName;       // null for a general note
    private final String cropName;
    private final LocalDate date;
    private final String note;
    private final String photoFile;      // null when there is no photo

    public JournalEntry(String authorName, String plotName, String cropName, LocalDate date, String note, String photoFile) {
        if (note == null || note.isBlank()) throw new ValidationException("Write a note first.");
        if (note.trim().length() > MAX_NOTE) throw new ValidationException("The note is too long (" + MAX_NOTE + " letters at most).");
        this.authorName = authorName;
        this.plotName = plotName;
        this.cropName = cropName;
        this.date = date;
        this.note = note.trim();
        this.photoFile = photoFile;
    }

    public void assignId(Long id) {
        if (this.id != null && !this.id.equals(id)) throw new IllegalStateException("This entry already has id " + this.id);
        this.id = id;
    }

    public Long getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getPlotName() { return plotName; }
    public String getCropName() { return cropName; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getPhotoFile() { return photoFile; }
}
