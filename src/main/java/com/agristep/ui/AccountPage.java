package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.config.LanguagePreference;
import com.agristep.domain.User;

import javax.swing.JPanel;
import java.util.List;
import java.util.Optional;

/** Password, language, shared garden and deleting the account. */
final class AccountPage extends Page {
    private record Loaded(Optional<User> joinedOwner, List<User> members, List<User> sent, List<User> invitations) {}

    /** Where the chosen language is remembered. Tests swap this for a temporary file. */
    static LanguagePreference languagePreference = LanguagePreference.atDefaultLocation();

    private final PasswordInput current = new PasswordInput("Current password");
    private final PasswordInput fresh = new PasswordInput("New password (6 or more characters)");
    private final PasswordInput again = new PasswordInput("New password again");
    private final SearchField inviteName = new SearchField("Their username");
    private final PasswordInput deletePassword = new PasswordInput("Your password");
    private boolean deleteArmed;
    private Long armedRemoval;
    private Loaded loaded;
    private int generation;

    AccountPage(AppContext ctx, Navigator nav) {
        super(ctx, nav);
        inviteName.addActionListener(e -> invite());
    }

    @Override void onShow() {
        deleteArmed = false;
        armedRemoval = null;
        int mine = ++generation;
        setContent(TodayPage.loadingCard("Opening your account…"));
        Tasks.run(() -> new Loaded(ctx.sharing.joinedOwner(), ctx.sharing.members(), ctx.sharing.sentInvites(), ctx.sharing.invitations()),
                result -> {
                    if (mine != generation) return;
                    loaded = result;
                    render();
                }, this::fail);
    }

    private void render() {
        User me = ctx.session.require();
        Card header = banner("Your account", me.getDisplayName() + " · @" + me.getUsername(), Scenery.Photo.PHILODENDRON);
        setContent(header, languageCard(), sharingCard(), passwordCard(), deleteCard());
    }

    // ---- language ----

    private Card languageCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h3("Language"));
        card.add(WrapLabel.muted("Menus, screen titles and the main buttons switch to Filipino. Longer explanations stay in English."));
        JPanel row = new JPanel(new FlowGridLayout(130, 2, 8, 0));
        row.setOpaque(false);
        for (I18n.Language language : I18n.Language.values()) {
            PillButton b = I18n.current() == language ? PillButton.primary(language.label()) : PillButton.secondary(language.label());
            b.addActionListener(e -> chooseLanguage(language));
            row.add(b);
        }
        card.add(row);
        return card;
    }

    void chooseLanguage(I18n.Language language) {
        if (language == I18n.current()) return;
        I18n.use(language);
        languagePreference.write(language.code());
        nav.restart(Navigator.ACCOUNT);
    }

    // ---- shared garden ----

    private Card sharingCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h3("Shared garden"));
        if (loaded.joinedOwner().isPresent()) {
            User owner = loaded.joinedOwner().get();
            card.add(WrapLabel.body("You are working in " + owner.getDisplayName() + "'s garden. The beds, plants, harvests, "
                    + "journal and location you see are theirs. Your own garden is kept as it was."));
            PillButton leave = PillButton.secondary("Leave garden");
            leave.addActionListener(e -> leave());
            card.add(Widgets.left(leave));
            return card;
        }
        card.add(WrapLabel.muted("Invite a family member or neighbour who has a PlantWise account. When they accept, you "
                + "both work in your garden until they leave."));
        JPanel inviteRow = new JPanel(new RowLayout(8, false));
        inviteRow.setOpaque(false);
        inviteRow.add(inviteName, RowLayout.CENTER);
        PillButton invite = PillButton.primary("Invite");
        invite.addActionListener(e -> invite());
        inviteRow.add(invite, RowLayout.EAST);
        card.add(inviteRow);

        for (User member : loaded.members()) {
            boolean armed = member.getId().equals(armedRemoval);
            PillButton remove = armed ? PillButton.danger("Really remove?") : PillButton.secondary("Remove");
            remove.addActionListener(e -> {
                if (!armed) {
                    armedRemoval = member.getId();
                    render();
                    return;
                }
                run(() -> ctx.sharing.remove(member.getId()), member.getDisplayName() + " is no longer in your garden.");
            });
            card.add(personRow(member, "In your garden", remove));
        }
        for (User invited : loaded.sent()) {
            PillButton cancel = PillButton.secondary("Cancel");
            cancel.addActionListener(e -> run(() -> ctx.sharing.cancelInvite(invited.getId()), "Invitation cancelled."));
            card.add(personRow(invited, "Invited, waiting for an answer", cancel));
        }
        for (User owner : loaded.invitations()) {
            PillButton accept = PillButton.primary("Accept");
            accept.addActionListener(e -> run(() -> ctx.sharing.accept(owner.getId()),
                    "You joined " + owner.getDisplayName() + "'s garden."));
            PillButton decline = PillButton.secondary("Decline");
            decline.addActionListener(e -> run(() -> ctx.sharing.decline(owner.getId()), "Invitation declined."));
            JPanel buttons = new JPanel(new FlowGridLayout(100, 2, 8, 0));
            buttons.setOpaque(false);
            buttons.add(accept);
            buttons.add(decline);
            Card invitation = new Card(Theme.LIME_SOFT, Theme.LIME).padding(12).gap(8);
            invitation.add(WrapLabel.bold(owner.getDisplayName() + " invited you to their garden"));
            invitation.add(buttons);
            card.add(invitation);
        }
        return card;
    }

    private static JPanel personRow(User person, String status, PillButton action) {
        JPanel row = new JPanel(new RowLayout(10, false));
        row.setOpaque(false);
        row.add(Emoji.label("👤", 26), RowLayout.WEST);
        JPanel text = new JPanel(new StackLayout(1));
        text.setOpaque(false);
        text.add(WrapLabel.bold(person.getDisplayName() + " · @" + person.getUsername()));
        text.add(WrapLabel.small(status));
        row.add(text, RowLayout.CENTER);
        row.add(action, RowLayout.EAST);
        return row;
    }

    void invite() {
        String name = inviteName.getText();
        Tasks.run(() -> {
            ctx.sharing.invite(name);
            return name.trim();
        }, sent -> {
            inviteName.setText("");
            nav.flash("Invitation sent to " + sent + ". They will see it on their Account screen.", false);
            onShow();
        }, this::fail);
    }

    private void leave() {
        run(ctx.sharing::leave, "You are back in your own garden.");
    }

    private void run(Runnable work, String done) {
        Tasks.run(() -> {
            work.run();
            return done;
        }, message -> {
            nav.flash(message, false);
            onShow();
        }, this::fail);
    }

    // ---- password ----

    private Card passwordCard() {
        Card card = new Card().gap(10);
        card.add(WrapLabel.h3("Change password"));
        card.add(Widgets.labelled("Current password", current));
        card.add(Widgets.labelled("New password", fresh));
        card.add(Widgets.labelled("New password again", again));
        PillButton save = PillButton.primary("Change password");
        save.addActionListener(e -> changePassword());
        card.add(Widgets.left(save));
        return card;
    }

    void changePassword() {
        String old = current.takeText();
        String next = fresh.takeText();
        String confirm = again.takeText();
        Tasks.run(() -> {
            ctx.account.changePassword(old, next, confirm);
            return true;
        }, ok -> nav.flash("Password changed. Use the new one next time you sign in.", false), this::fail);
    }

    // ---- delete ----

    private Card deleteCard() {
        Card card = new Card(Theme.WHITE, Theme.SILI).gap(10);
        card.add(WrapLabel.h3("Delete account"));
        card.add(WrapLabel.muted("This permanently removes your account, beds, harvest history, journal and photos. "
                + "It cannot be undone. People who joined your garden go back to their own."));
        card.add(Widgets.labelled("Type your password to confirm", deletePassword));
        PillButton delete = deleteArmed ? PillButton.danger("Yes, delete everything") : PillButton.danger("Delete account");
        delete.addActionListener(e -> {
            if (!deleteArmed) {
                deleteArmed = true;
                render();
                return;
            }
            deleteAccount();
        });
        card.add(Widgets.left(delete));
        return card;
    }

    void deleteAccount() {
        String password = deletePassword.takeText();
        Tasks.run(() -> {
            ctx.account.deleteAccount(password);
            return true;
        }, ok -> nav.signOut(), t -> {
            deleteArmed = false;
            fail(t);
            render();
        });
    }
}
