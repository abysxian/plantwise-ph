package com.agristep.repository;

import java.util.List;

/** Removing everything that belongs to one account. */
public interface AccountData {
    /** Names of the journal photo files owned by this garden, so the files can be deleted too. */
    List<String> journalPhotoFiles(long gardenOwnerId);

    /** Deletes the account and its garden, history, journal and sharing links. */
    void deleteAccount(long userId);
}
