package com.agristep.service;

import com.agristep.domain.User;
import com.agristep.domain.ValidationException;
import com.agristep.repository.LegacyData;
import com.agristep.repository.SharingRepository;
import com.agristep.repository.Transactions;
import com.agristep.repository.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Creating accounts, signing in and out, and keeping track of who is signed in. */
public class AuthService {
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo1234";

    static final int MAX_FAILED_TRIES = 5;
    static final Duration LOCKOUT = Duration.ofSeconds(30);

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,20}");

    private record Attempts(int failures, Instant lockedUntil) {}

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Session session;
    private final GardenProvisioner provisioner;
    private final LegacyData legacy;
    private final Transactions transactions;
    private final Clock clock;
    private final SharingRepository sharing;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final String unknownUserHash;

    public AuthService(UserRepository users, PasswordHasher hasher, Session session, GardenProvisioner provisioner,
                       LegacyData legacy, Transactions transactions, Clock clock, SharingRepository sharing) {
        this.users = users;
        this.hasher = hasher;
        this.session = session;
        this.provisioner = provisioner;
        this.legacy = legacy;
        this.transactions = transactions;
        this.clock = clock;
        this.sharing = sharing;
        this.unknownUserHash = hasher.hash("not-a-real-password");   // so unknown names take as long as wrong passwords
    }

    public Optional<User> currentUser() { return session.user(); }

    /** Creates an account and signs it in. The first account ever created also takes over any older, ownerless data. */
    public User register(String username, String displayName, String password, String confirm) {
        String name = username == null ? "" : username.trim();
        if (!USERNAME.matcher(name).matches()) {
            throw new ValidationException("Username must be 3 to 20 letters, numbers, dots, dashes or underscores.");
        }
        String shown = displayName == null || displayName.isBlank() ? name : displayName.trim();
        if (shown.length() > 40) throw new ValidationException("The name is too long (40 letters at most).");
        if (password == null || password.length() < 6) throw new ValidationException("Password must be at least 6 characters.");
        if (password.length() > 100) throw new ValidationException("Password is too long (100 characters at most).");
        if (!password.equals(confirm)) throw new ValidationException("The two passwords don't match.");
        if (users.findByUsername(name).isPresent()) throw new ValidationException("That username is already taken.");

        User user = new User(name, shown, hasher.hash(password), LocalDateTime.now(clock));
        try {
            transactions.run(() -> {
                boolean first = users.count() == 0;
                users.save(user);
                session.signIn(user);
                if (first && legacy.exists()) legacy.assignTo(user.getId());
                provisioner.provision();
                return null;
            });
        } catch (RuntimeException e) {
            session.signOut();
            throw e;
        }
        return user;
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Enter your username and password.");
        }
        String key = User.keyOf(username);
        Instant now = clock.instant();
        Attempts current = attempts.get(key);
        if (current != null && current.lockedUntil() != null && now.isBefore(current.lockedUntil())) {
            long wait = Math.max(1, Duration.between(now, current.lockedUntil()).toSeconds() + 1);
            throw new AuthenticationException("Too many wrong attempts. Try again in " + wait + " seconds.");
        }
        Optional<User> found = users.findByUsername(username);
        boolean ok = hasher.matches(password, found.map(User::getPasswordHash).orElse(unknownUserHash)) && found.isPresent();
        if (!ok) {
            int failures = (current == null || current.lockedUntil() != null ? 0 : current.failures()) + 1;
            attempts.put(key, failures >= MAX_FAILED_TRIES ? new Attempts(0, now.plus(LOCKOUT)) : new Attempts(failures, null));
            throw new AuthenticationException("Wrong username or password.");
        }
        attempts.remove(key);
        signInWithGarden(found.get());
        return found.get();
    }

    /** Signs in without a password. Only for "keep me signed in", which the person switched on for this computer. */
    public Optional<User> signInRemembered(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        Optional<User> found = users.findByUsername(username);
        found.ifPresent(this::signInWithGarden);
        return found;
    }

    /** Signs in, and if this person joined someone else's garden, opens that garden. */
    private void signInWithGarden(User user) {
        session.signIn(user);
        sharing.ownerOf(user.getId()).ifPresent(session::useGardenOf);
    }

    public void logout() { session.signOut(); }

    /** On a brand-new database, makes the demo account so the app can be tried straight away. */
    public void ensureDemoAccount() {
        if (users.count() > 0) return;
        register(DEMO_USERNAME, "Demo farmer", DEMO_PASSWORD, DEMO_PASSWORD);
        session.signOut();
    }
}
