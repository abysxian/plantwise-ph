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
        main = new MainPanel(ctx, this::signOut);
        shell.setWindowTitle("PlantWise PH - " + user.getDisplayName());
        shell.showBody(main);
        if (firstTime) {
            main.show(Navigator.LOCATION);
            main.flash("Welcome, " + user.getDisplayName() + "! Set where your garden is so the weather and crop matches are right.", false);
        }
    }

    public void signOut() {
        remembered.clear();
        showSignIn();
    }

    MainPanel mainPanel() { return main; }
}
