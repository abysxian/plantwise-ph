package com.agristep.service;

import com.agristep.domain.NotFoundException;
import com.agristep.domain.User;
import com.agristep.domain.ValidationException;
import com.agristep.repository.SharingRepository;
import com.agristep.repository.Transactions;
import com.agristep.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Community gardens. An owner invites other accounts by username; a person who accepts works in the owner's
 * garden (beds, plants, harvests, journal, location) until they leave. Their own garden is kept untouched.
 */
public class SharingService {
    private final SharingRepository sharing;
    private final UserRepository users;
    private final Session session;
    private final Transactions transactions;
    private final Clock clock;

    public SharingService(SharingRepository sharing, UserRepository users, Session session, Transactions transactions, Clock clock) {
        this.sharing = sharing;
        this.users = users;
        this.session = session;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** The owner whose garden the signed-in person is currently working in, if it is not their own. */
    public Optional<User> joinedOwner() {
        long me = session.requireUserId();
        return sharing.ownerOf(me).flatMap(users::findById);
    }

    public List<User> members() { return sharing.membersOf(session.requireUserId()); }

    public List<User> sentInvites() { return sharing.invitedBy(session.requireUserId()); }

    public List<User> invitations() { return sharing.invitationsFor(session.requireUserId()); }

    public void invite(String username) {
        long me = session.requireUserId();
        if (sharing.ownerOf(me).isPresent()) throw new ValidationException("Leave the garden you joined before inviting people to your own.");
        if (username == null || username.isBlank()) throw new ValidationException("Type the username of the person to invite.");
        User target = users.findByUsername(username.trim()).orElseThrow(() -> new NotFoundException("No account has that username."));
        if (target.getId() == me) throw new ValidationException("That is your own account.");
        if (sharing.membersOf(me).stream().anyMatch(u -> u.getId().equals(target.getId()))) {
            throw new ValidationException(target.getDisplayName() + " is already in your garden.");
        }
        if (sharing.inviteExists(me, target.getId())) throw new ValidationException("You already invited " + target.getDisplayName() + ".");
        sharing.addInvite(me, target.getId(), LocalDateTime.now(clock));
    }

    public void cancelInvite(long inviteeId) { sharing.deleteInvite(session.requireUserId(), inviteeId); }

    public void decline(long ownerId) { sharing.deleteInvite(ownerId, session.requireUserId()); }

    public void accept(long ownerId) {
        long me = session.requireUserId();
        if (!sharing.inviteExists(ownerId, me)) throw new NotFoundException("That invitation is no longer there.");
        if (sharing.ownerOf(me).isPresent()) throw new ValidationException("Leave your current shared garden first.");
        if (!sharing.membersOf(me).isEmpty()) {
            throw new ValidationException("People are working in your garden. Remove them before joining another garden.");
        }
        transactions.run(() -> {
            sharing.addMember(ownerId, me);
            sharing.deleteInvite(ownerId, me);
            return null;
        });
        session.useGardenOf(ownerId);
    }

    /** Goes back to the person's own garden. */
    public void leave() {
        sharing.removeMember(session.requireUserId());
        session.useGardenOf(null);
    }

    /** Owner only: takes someone out of the garden. */
    public void remove(long memberId) {
        long me = session.requireUserId();
        if (sharing.membersOf(me).stream().noneMatch(u -> u.getId() == memberId)) throw new NotFoundException("That person is not in your garden.");
        sharing.removeMember(memberId);
    }
}
