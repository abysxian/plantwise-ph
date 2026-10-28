package com.agristep.repository;

import com.agristep.domain.User;

import java.util.List;
import java.util.Optional;

/** Who shares whose garden, and who has been invited. */
public interface SharingRepository {
    /** The owner of the garden this person has joined, if any. */
    Optional<Long> ownerOf(long memberId);

    void addMember(long ownerId, long memberId);

    void removeMember(long memberId);

    List<User> membersOf(long ownerId);

    void addInvite(long ownerId, long inviteeId, java.time.LocalDateTime now);

    boolean inviteExists(long ownerId, long inviteeId);

    void deleteInvite(long ownerId, long inviteeId);

    /** People this owner has invited who have not answered yet. */
    List<User> invitedBy(long ownerId);

    /** Owners who have invited this person. */
    List<User> invitationsFor(long inviteeId);
}
