package com.agristep.service;

import com.agristep.domain.User;
import com.agristep.domain.ValidationException;
import com.agristep.repository.AccountData;
import com.agristep.repository.Transactions;
import com.agristep.repository.UserRepository;

import java.util.List;

/** Changing the password and deleting the account. Both ask for the current password first. */
public class AccountService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Session session;
    private final AccountData data;
    private final PhotoStore photos;
    private final Transactions transactions;

    public AccountService(UserRepository users, PasswordHasher hasher, Session session, AccountData data,
                          PhotoStore photos, Transactions transactions) {
        this.users = users;
        this.hasher = hasher;
        this.session = session;
        this.data = data;
        this.photos = photos;
        this.transactions = transactions;
    }

    public void changePassword(String current, String newPassword, String confirm) {
        User me = verified(current);
        if (newPassword == null || newPassword.length() < 6) throw new ValidationException("The new password must be at least 6 characters.");
        if (newPassword.length() > 100) throw new ValidationException("The new password is too long (100 characters at most).");
        if (!newPassword.equals(confirm)) throw new ValidationException("The two new passwords don't match.");
        if (newPassword.equals(current)) throw new ValidationException("Choose a password different from the current one.");
        users.updatePasswordHash(me.getId(), hasher.hash(newPassword));
    }

    /**
     * Deletes the signed-in account, its garden and everything in it. People who had joined this garden go back to
     * their own. The caller signs out afterwards.
     */
    public void deleteAccount(String password) {
        User me = verified(password);
        long id = me.getId();
        List<String> photoFiles = data.journalPhotoFiles(id);
        transactions.run(() -> {
            data.deleteAccount(id);
            return null;
        });
        photoFiles.forEach(photos::delete);
    }

    private User verified(String password) {
        User me = session.require();
        User stored = users.findById(me.getId()).orElseThrow(() -> new ValidationException("This account no longer exists."));
        if (password == null || password.isEmpty() || !hasher.matches(password, stored.getPasswordHash())) {
            throw new ValidationException("The current password is not right.");
        }
        return stored;
    }
}
