package com.agristep.repository;

import com.agristep.domain.JournalEntry;

import java.util.List;
import java.util.Optional;

public interface JournalRepository {
    JournalEntry save(JournalEntry entry);

    /** Newest first. */
    List<JournalEntry> findAllNewestFirst();

    Optional<JournalEntry> findById(long id);

    void deleteById(long id);
}
