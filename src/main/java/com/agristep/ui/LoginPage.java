package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.User;
import com.agristep.domain.ValidationException;
import com.agristep.service.AuthService;
import com.agristep.service.AuthenticationException;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;

/** Sign in, or create an account. Shown before anything else when nobody is signed in. */
public final class LoginPage extends ScrollPage {

    /** What happens after a successful sign-in or registration. */
    public interface Listener {
        void signedIn(User user, boolean remember, boolean newAccount);
    }

    private final AppContext ctx;
    private final Listener listener;
    private final String dataNote;

    private boolean creating;
    private final SearchField username = new SearchField("Username");
    private final SearchField displayName = new SearchField("What should we call you? (optional)");
    private final PasswordInput password = new PasswordInput("Password");
    private final PasswordInput confirm = new PasswordInput("Type the password again");
    private final JCheckBox remember = checkBox("Keep me signed in on this computer");
    private final JCheckBox showPassword = checkBox("Show password");
    private final WrapLabel error = new WrapLabel("", Theme.BODY_BOLD, Theme.SILI);
    private final PillButton submit = PillButton.primary("Sign in");
    private final JPanel form = new JPanel(new StackLayout(10));

    public LoginPage(AppContext ctx, String dataNote, Listener listener) {
        this.ctx = ctx;
        this.listener = listener;
        this.dataNote = dataNote;
        setLayout(new StackLayout(14, 460));
        Motion.ambient(this);
        form.setOpaque(false);
        error.setVisible(false);
        submit.addActionListener(e -> submit());
        for (Component c : new Component[] {username, displayName, password, confirm}) {
            if (c instanceof javax.swing.JTextField f) f.addActionListener(e -> submit());
        }
        showPassword.addActionListener(e -> {
            password.setShown(showPassword.isSelected());
            confirm.setShown(showPassword.isSelected());
        });
        build();
    }

    private static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(I18n.t(text));
        box.setOpaque(false);
        box.setFont(Theme.BODY);
        box.setForeground(Theme.INK);
        box.setFocusPainted(false);
        return box;
    }

    boolean isCreating() { return creating; }

    /** When the window is taller than the form, let the landscape fill the whole window. */
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof javax.swing.JViewport vp && vp.getHeight() > getPreferredSize().height;
    }

    /** A daytime landscape behind the sign-in card. */
    @Override protected void paintComponent(java.awt.Graphics g) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        Scenery.paintBackdrop(g2, creating ? Scenery.Photo.HOUSEPLANTS : Scenery.Photo.HILLS, getWidth(), getHeight(), Motion.seconds());
        g2.dispose();
    }

    // ---- layout ----

    private void build() {
        removeAll();
        JPanel brand = new JPanel(new StackLayout(4));
        brand.setOpaque(false);
        brand.add(Brand.mark(96, true));
        brand.add(new WrapLabel("PlantWise PH", Theme.H1, Color.WHITE).align(WrapLabel.CENTER));
        brand.add(new WrapLabel("Know what to plant, and when.", Theme.BODY, new Color(0xE6F4EC)).align(WrapLabel.CENTER));
        add(brand);

        Card card = new Card().gap(12).padding(18);
        JPanel tabs = new JPanel(new FlowGridLayout(120, 2, 8, 0));
        tabs.setOpaque(false);
        PillButton signInTab = creating ? PillButton.secondary("Sign in") : PillButton.primary("Sign in");
        PillButton createTab = creating ? PillButton.primary("Create account") : PillButton.secondary("Create account");
        signInTab.addActionListener(e -> switchTo(false));
        createTab.addActionListener(e -> switchTo(true));
        tabs.add(signInTab);
        tabs.add(createTab);
        card.add(tabs);

        form.removeAll();
        form.add(field("Username", username));
        if (creating) form.add(field("Your name", displayName));
        form.add(field("Password", password));
        if (creating) {
            form.add(field("Confirm password", confirm));
            form.add(WrapLabel.small("Use at least 6 characters."));
        }
        form.add(showPassword);
        form.add(remember);
        form.add(error);
        submit.setText(creating ? "Create account" : "Sign in");
        form.add(submit);
        card.add(form);
        add(card);

        Card demo = new Card(Theme.LIME_SOFT, Theme.LIME).gap(6).padding(14);
        demo.add(WrapLabel.bold("Just want to try it?"));
        demo.add(WrapLabel.body("Use the demo account. Username: " + AuthService.DEMO_USERNAME
                + "   Password: " + AuthService.DEMO_PASSWORD));
        PillButton fill = PillButton.secondary("Fill in the demo account");
        fill.addActionListener(e -> {
            switchTo(false);
            username.setText(AuthService.DEMO_USERNAME);
            password.setText(AuthService.DEMO_PASSWORD);
        });
        demo.add(CropsPage.left(fill));
        add(demo);

        Card note = new Card(new Color(255, 255, 255, 235), Theme.LINE).gap(2).padding(10);
        note.add(WrapLabel.small(dataNote).align(WrapLabel.CENTER));
        note.add(WrapLabel.small("Each account keeps its own beds, plants, location and harvest history.").align(WrapLabel.CENTER));
        add(note);
        revalidate();
        repaint();
    }

    private static JPanel field(String label, Component input) {
        JPanel box = new JPanel(new StackLayout(4));
        box.setOpaque(false);
        box.add(new WrapLabel(label, Theme.SMALL_BOLD, Theme.MUTED));
        box.add(input);
        return box;
    }

    private void switchTo(boolean creating) {
        if (this.creating == creating) return;
        this.creating = creating;
        showError("");
        build();
    }

    // ---- actions ----

    private void showError(String text) {
        error.setText(text);
        error.setVisible(!text.isEmpty());
        revalidate();
        repaint();
    }

    private void submit() {
        if (!submit.isEnabled()) return;
        showError("");
        String name = username.getText();
        String pass = password.takeText();
        String again = confirm.takeText();
        String shown = displayName.getText();
        boolean register = creating;
        boolean keep = remember.isSelected();
        submit.setEnabled(false);
        String label = submit.getText();
        submit.setText("Please wait…");
        Tasks.run(() -> register ? ctx.auth.register(name, shown, pass, again) : ctx.auth.login(name, pass), user -> {
            submit.setEnabled(true);
            submit.setText(label);
            listener.signedIn(user, keep, register);
        }, problem -> {
            submit.setEnabled(true);
            submit.setText(label);
            if (problem instanceof ValidationException || problem instanceof AuthenticationException) {
                showError(problem.getMessage());
            } else {
                showError("Something went wrong: " + Tasks.messageOf(problem));
            }
        });
    }

    Color errorColor() { return error.getForeground(); }
}
