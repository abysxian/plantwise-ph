package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.config.RememberedLogin;
import com.agristep.domain.User;

/** Moves between the sign-in screen and the app, and looks after "keep me signed in". */
public final class AppController {
    private final Shell shell;
    private final AppContext ctx;
    private final RememberedLogin remembered;
    private final String dataNote;
    private MainPanel main;

    public AppController(Shell shell, AppContext ctx, RememberedLogin remembered, String dataNote) {
        this.shell = shell;
        this.ctx = ctx;
        this.remembered = remembered;
        this.dataNote = dataNote;
    }

    /** @param alreadySignedIn true when the remembered user was found and signed in at start-up */
    public void start(boolean alreadySignedIn) {
        if (alreadySignedIn && ctx.session.isSignedIn()) showApp(false);
        else showSignIn();
    }

    public void showSignIn() {
        ctx.auth.logout();
        main = null;
        shell.setWindowTitle("PlantWise PH");
        shell.showBody(new javax.swing.JScrollPane(new LoginPage(ctx, dataNote, this::signedIn),
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
            { setBorder(null); getViewport().setBackground(Theme.PAPER); getVerticalScrollBar().setUnitIncrement(24); }
        });
    }

    private void signedIn(User user, boolean remember, boolean newAccount) {
        if (remember) remembered.write(user.getUsername());
        else remembered.clear();
        showApp(newAccount);
    }

    private void showApp(boolean firstTime) {
        User user = ctx.session.require();
        main = new MainPanel(ctx, this::signOut, this::restart);
        shell.setWindowTitle("PlantWise PH - " + user.getDisplayName());
        shell.showBody(main);
        if (!firstTime) announce();
        if (firstTime) {
            main.show(Navigator.LOCATION);
            main.flash("Welcome, " + user.getDisplayName() + "! Set where your garden is so the weather and crop matches are right.", false);
        }
    }

    /** On opening the app, says how many garden tasks are due, in the window and as a desktop notification. */
    private void announce() {
        MainPanel panel = main;
        Tasks.run(() -> ctx.garden.reminders(0).size(), due -> {
            if (due > 0 && panel == main) {
                String text = due == 1 ? "1 garden task is due today or overdue." : due + " garden tasks are due today or overdue.";
                panel.flash(text, false);
                Notifier.desktop("PlantWise PH", I18n.t(text));
            }
        }, t -> { /* reminders are a nicety; the Today screen shows any problem */ });
    }

    /** Rebuilds the main window (for example after the language changed) and opens one screen. */
    private void restart(String page) {
        if (!ctx.session.isSignedIn()) return;
        User user = ctx.session.require();
        main = new MainPanel(ctx, this::signOut, this::restart);
        shell.setWindowTitle("PlantWise PH - " + user.getDisplayName());
        shell.showBody(main);
        main.show(page);
    }

    public void signOut() {
        remembered.clear();
        showSignIn();
    }

    MainPanel mainPanel() { return main; }
}
