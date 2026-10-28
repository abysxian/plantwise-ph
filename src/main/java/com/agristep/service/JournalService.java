package com.agristep.service;

import com.agristep.domain.JournalEntry;
import com.agristep.domain.NotFoundException;
import com.agristep.domain.Plot;
import com.agristep.repository.JournalRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** The garden journal: dated notes and photos, about a bed or about the garden in general. */
public class JournalService {
    private final JournalRepository entries;
    private final PhotoStore photos;
    private final GardenService garden;
    private final Session session;
    private final Clock clock;

    public JournalService(JournalRepository entries, PhotoStore photos, GardenService garden, Session session, Clock clock) {
        this.entries = entries;
        this.photos = photos;
        this.garden = garden;
        this.session = session;
        this.clock = clock;
    }

    /** @param plotId the bed the note is about, or null for a general note; @param photo an optional picture file */
    public JournalEntry add(Long plotId, String note, Path photo) {
        String plotName = null;
        String cropName = null;
        if (plotId != null) {
            Plot plot = garden.getPlot(plotId);
            plotName = plot.getName();
            cropName = plot.isEmpty() ? null : plot.getCrop().getName();
        }
        // validate the note before a photo is copied
        JournalEntry probe = new JournalEntry(session.require().getDisplayName(), plotName, cropName, LocalDate.now(clock), note, null);
        String stored = photo == null ? null : photos.save(photo);
        try {
            return entries.save(new JournalEntry(probe.getAuthorName(), plotName, cropName, probe.getDate(), probe.getNote(), stored));
        } catch (RuntimeException e) {
            if (stored != null) photos.delete(stored);
            throw e;
        }
    }

    public List<JournalEntry> list() { return entries.findAllNewestFirst(); }

    public void delete(long id) {
        JournalEntry entry = entries.findById(id).orElseThrow(() -> new NotFoundException("That journal entry does not exist."));
        entries.deleteById(id);
        if (entry.getPhotoFile() != null) photos.delete(entry.getPhotoFile());
    }

    public Path photoPath(JournalEntry entry) { return entry.getPhotoFile() == null ? null : photos.pathOf(entry.getPhotoFile()); }
}
