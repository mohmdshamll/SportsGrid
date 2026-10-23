package service;

import dao.UserDAO;
import db.DBConnection;
import db.DataAccessException;
import db.DuplicateEntryException;
import model.Role;
import model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Registration and login. Passwords are never stored as text: each one is salted and
 * hashed with PBKDF2 (built into Java, no extra library needed).
 *
 * Methods throw IllegalArgumentException with a message that is safe to show in a JOptionPane.
 */
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    private static final int ITERATIONS = 65_536;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDAO userDAO = new UserDAO();

    /** Public sign-up: only PLAYER and OWNER accounts can be created this way. */
    public User register(String name, String email, String phone, String password, Role role) {
        if (role != Role.PLAYER && role != Role.OWNER) {
            throw new IllegalArgumentException("You can only sign up as a Player or a Turf Owner");
        }
        return createUser(name, email, phone, password, role);
    }

    /** Returns the logged-in user, or throws IllegalArgumentException("Invalid email or password"). */
    public User login(String email, String password) {
        if (email == null || password == null) throw new IllegalArgumentException("Enter email and password");
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> found = userDAO.findByEmail(normalizedEmail);
        if (found.isEmpty()) {
            System.err.println("[AuthService] Login failed: No user registered with email '" + normalizedEmail + "'");
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!verifyPassword(password, found.get().getPasswordHash())) {
            System.err.println("[AuthService] Login failed: Incorrect password for email '" + normalizedEmail + "'");
            throw new IllegalArgumentException("Invalid email or password");
        }
        return found.get();
    }

    /**
     * Creates the first admin account if no account with that email exists yet.
     * Never overwrites existing passwords or account details.
     *
     * @return true if a new admin account was created; false if an account already exists.
     */
    public boolean seedAdminIfMissing(String email, String password) {
        if (email == null || password == null) return false;
        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.findByEmail(normalizedEmail).isEmpty()) {
            createUser("Administrator", normalizedEmail, "0000000000", password, Role.ADMIN);
            return true;
        }
        return false;
    }

    /** Checks if any administrator account exists in the database. */
    public boolean hasAnyAdmin() {
        return userDAO.hasAdmin();
    }

    /**
     * Initializes the default administrator account during application startup.
     *
     * Safe startup guarantees:
     * 1. Checks if the default admin or any administrator already exists.
     * 2. Never overwrites existing admin passwords or account details.
     * 3. Prevents duplicate admin creation.
     * 4. Does not display passwords in logs or output.
     * 5. Handles database connection errors gracefully without crashing the app.
     *
     * @return true if default admin was seeded; false if verified or skipped.
     */
    public boolean initializeDefaultAdmin() {
        return initializeDefaultAdmin(DBConnection.getAdminEmail(), DBConnection.getAdminPassword());
    }

    /**
     * Initializes an administrator with the specified credentials if not already present.
     */
    public boolean initializeDefaultAdmin(String email, String password) {
        try {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                System.err.println("[AuthService] Administrator initialization skipped: email or password is empty.");
                return false;
            }
            String normalizedEmail = email.trim().toLowerCase();

            Optional<User> existing = userDAO.findByEmail(normalizedEmail);
            if (existing.isPresent()) {
                System.out.println("[AuthService] Administrator account verified (" + normalizedEmail + ").");
                return false;
            }

            // If another admin already exists in the system, avoid creating duplicate administrator accounts
            if (userDAO.hasAdmin()) {
                System.out.println("[AuthService] Existing administrator account found in database. Skipping default seed.");
                return false;
            }

            boolean created = seedAdminIfMissing(normalizedEmail, password);
            if (created) {
                System.out.println("[AuthService] Initialized default administrator account (" + normalizedEmail + ").");
            }
            return created;
        } catch (DataAccessException e) {
            System.err.println("[AuthService] Warning: Could not verify or initialize administrator account (database unreachable: " + e.getMessage() + ").");
            return false;
        } catch (Exception e) {
            System.err.println("[AuthService] Warning: Administrator initialization could not complete: " + e.getMessage());
            return false;
        }
    }

    private User createUser(String name, String email, String phone, String password, Role role) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (name.trim().length() > 100) throw new IllegalArgumentException("Name is too long");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw new IllegalArgumentException("Enter a valid email");
        if (phone == null || !PHONE.matcher(phone.trim()).matches()) throw new IllegalArgumentException("Phone must be 10 digits");
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        User user = User.create(role, 0, name.trim(), email.trim().toLowerCase(), phone.trim(), hashPassword(password));
        try {
            userDAO.insert(user);
        } catch (DuplicateEntryException e) {
            throw new IllegalArgumentException("This email is already registered");
        }
        return user;
    }

    // ---------- password hashing (format: iterations:salt:hash, both Base64) ----------

    static String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);   // constant-time comparison
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }
}
