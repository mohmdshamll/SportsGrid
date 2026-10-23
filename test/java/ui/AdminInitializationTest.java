package ui;

import dao.UserDAO;
import model.Admin;
import model.Role;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.AuthService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminInitializationTest {

    private AuthService authService;
    private UserDAO userDAO;

    @BeforeEach
    void setup() {
        authService = new AuthService();
        userDAO = new UserDAO();
    }

    @Test
    @DisplayName("Verify default admin initialization, authentication, and role assignment")
    void testDefaultAdminInitialization() {
        // Run normal startup initialization
        authService.initializeDefaultAdmin();

        // Verify account exists in database
        Optional<User> adminOpt = userDAO.findByEmail("admin@sportgrid.com");
        assertTrue(adminOpt.isPresent(), "Default admin account must exist after initialization");

        User admin = adminOpt.get();
        assertEquals(Role.ADMIN, admin.getRole(), "User role must be ADMIN");
        assertTrue(admin instanceof Admin, "User instance must be Admin");

        // Verify login works with default password
        User loggedIn = assertDoesNotThrow(() -> authService.login("admin@sportgrid.com", "Admin@123"));
        assertNotNull(loggedIn);
        assertEquals(admin.getUserId(), loggedIn.getUserId());
    }

    @Test
    @DisplayName("Verify idempotency: repeated startup initialization does not duplicate or alter accounts")
    void testInitializationIdempotency() {
        // First run ensures admin is present
        authService.initializeDefaultAdmin();

        // Second run must return false (already present, skipped)
        boolean createdAgain = authService.initializeDefaultAdmin();
        assertFalse(createdAgain, "Second initialization must not create a new account");

        // Count how many users have this email
        long count = userDAO.findAll().stream()
                .filter(u -> "admin@sportgrid.com".equalsIgnoreCase(u.getEmail()))
                .count();
        assertEquals(1, count, "Exactly one administrator account with default email must exist");
    }

    @Test
    @DisplayName("Verify existing admin password and details are never overwritten")
    void testNeverOverwriteExistingAdmin() {
        String tag = String.valueOf(System.currentTimeMillis() % 1_000_000);
        String customEmail = "admin_custom_" + tag + "@sportgrid.com";
        String initialPassword = "InitialSecurePassword123";

        // Seed custom admin
        boolean firstSeed = authService.seedAdminIfMissing(customEmail, initialPassword);
        assertTrue(firstSeed, "First seed must succeed");

        // Verify can login with initial password
        User adminUser = assertDoesNotThrow(() -> authService.login(customEmail, initialPassword));
        assertNotNull(adminUser);

        // Attempt to re-seed with a DIFFERENT password
        boolean secondSeed = authService.seedAdminIfMissing(customEmail, "HackedOrOverwrittenPassword123");
        assertFalse(secondSeed, "Re-seeding with different password must return false");

        // Original password must STILL WORK
        User originalLogin = assertDoesNotThrow(() -> authService.login(customEmail, initialPassword));
        assertEquals(adminUser.getUserId(), originalLogin.getUserId());

        // New password MUST FAIL
        assertThrows(IllegalArgumentException.class, () -> {
            authService.login(customEmail, "HackedOrOverwrittenPassword123");
        }, "Login with unapplied new password must fail");
    }

    @Test
    @DisplayName("Verify avoid creating duplicate admin accounts when custom admin exists")
    void testAvoidDuplicateAdmins() {
        String tag = String.valueOf(System.currentTimeMillis() % 1_000_000);
        String customEmail = "lead_admin_" + tag + "@sportgrid.com";

        // Ensure hasAdmin is true by seeding a custom admin
        authService.seedAdminIfMissing(customEmail, "LeadAdmin@123");
        assertTrue(authService.hasAnyAdmin(), "hasAnyAdmin() must return true");

        // Now initializeDefaultAdmin with a non-existent email
        String dummyDefaultEmail = "never_created_" + tag + "@sportgrid.com";
        boolean created = authService.initializeDefaultAdmin(dummyDefaultEmail, "Password@123");

        // Because an admin already exists in the system, initializeDefaultAdmin must NOT create duplicate admin
        assertFalse(created, "Must skip creating new default admin when an admin already exists");
        assertFalse(userDAO.findByEmail(dummyDefaultEmail).isPresent(), "Dummy admin must not have been created");
    }

    @Test
    @DisplayName("Verify handling of invalid or blank admin credentials")
    void testBlankCredentialsHandling() {
        assertFalse(authService.initializeDefaultAdmin(null, "Password123"));
        assertFalse(authService.initializeDefaultAdmin("", "Password123"));
        assertFalse(authService.initializeDefaultAdmin("admin@test.com", null));
        assertFalse(authService.initializeDefaultAdmin("admin@test.com", ""));
    }
}
