package com.agristep.persistence;

import com.agristep.domain.User;
import com.agristep.repository.SharingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcSharingRepository implements SharingRepository {
    private static final String USER_COLUMNS = "u.id, u.username, u.display_name, u.password_hash, u.created_at";

    private final Database db;

    public JdbcSharingRepository(Database db) {
        this.db = db;
    }

    @Override public Optional<Long> ownerOf(long memberId) {
        return db.withConnection(c -> Sql.query(c, "SELECT owner_id FROM garden_members WHERE member_id = ?",
                rs -> rs.getLong(1), memberId).stream().findFirst());
    }

    @Override public void addMember(long ownerId, long memberId) {
        db.withConnection(c -> Sql.update(c, "INSERT INTO garden_members (member_id, owner_id) VALUES (?,?)", memberId, ownerId));
    }

    @Override public void removeMember(long memberId) {
        db.withConnection(c -> Sql.update(c, "DELETE FROM garden_members WHERE member_id = ?", memberId));
    }

    @Override public List<User> membersOf(long ownerId) {
        return users("FROM garden_members m JOIN users u ON u.id = m.member_id WHERE m.owner_id = ? ORDER BY u.username_key", ownerId);
    }

    @Override public void addInvite(long ownerId, long inviteeId, LocalDateTime now) {
        db.withConnection(c -> Sql.update(c, "INSERT INTO garden_invites (owner_id, invitee_id, created_at) VALUES (?,?,?)",
                ownerId, inviteeId, now));
    }

    @Override public boolean inviteExists(long ownerId, long inviteeId) {
        return db.withConnection(c -> Sql.count(c, "SELECT COUNT(*) FROM garden_invites WHERE owner_id = ? AND invitee_id = ?",
                ownerId, inviteeId)) > 0;
    }

    @Override public void deleteInvite(long ownerId, long inviteeId) {
        db.withConnection(c -> Sql.update(c, "DELETE FROM garden_invites WHERE owner_id = ? AND invitee_id = ?", ownerId, inviteeId));
    }

    @Override public List<User> invitedBy(long ownerId) {
        return users("FROM garden_invites i JOIN users u ON u.id = i.invitee_id WHERE i.owner_id = ? ORDER BY i.id", ownerId);
    }

    @Override public List<User> invitationsFor(long inviteeId) {
        return users("FROM garden_invites i JOIN users u ON u.id = i.owner_id WHERE i.invitee_id = ? ORDER BY i.id", inviteeId);
    }

    private List<User> users(String tail, Object... params) {
        return db.withConnection(c -> Sql.query(c, "SELECT " + USER_COLUMNS + " " + tail, rs -> {
            User u = new User(rs.getString("username"), rs.getString("display_name"), rs.getString("password_hash"),
                    Sql.dateTime(rs, "created_at"));
            u.assignId(rs.getLong("id"));
            return u;
        }, params));
    }
}
