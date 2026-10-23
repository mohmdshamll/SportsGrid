package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single place that knows how to connect to MySQL.
 * Settings come from a file called db.properties in the project root
 * (copy db.properties.example to db.properties and put your own password in it).
 * db.properties is in .gitignore so nobody's password ends up on GitHub.
 */
public final class DBConnection {

    private static final Properties CONFIG = loadConfig();

    private DBConnection() { }   // utility class, never instantiated

    private static Properties loadConfig() {
        Properties p = new Properties();
        p.setProperty("db.url",
                "jdbc:mysql://localhost:3306/sportgrid?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata");
        p.setProperty("db.user", "root");
        p.setProperty("db.password", "");
        p.setProperty("admin.email", "admin@sportgrid.com");
        p.setProperty("admin.password", "Admin@123");

        java.io.File file = new java.io.File("db.properties");
        if (!file.exists()) {
            file = new java.io.File("extracted/-SportGrid-web-version/db.properties");
        }

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                p.load(in);
                System.out.println("[DBConnection] Loaded config from " + file.getPath());
            } catch (IOException e) {
                System.err.println("[DBConnection] Failed reading " + file.getPath() + ": " + e.getMessage());
            }
        } else {
            System.err.println("[DBConnection] db.properties not found, using defaults (user root, empty password).");
        }
        return p;
    }

    /** Opens a new connection. Always use it inside try-with-resources so it is closed. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }

    /** Retrieves the configured default admin email address. */
    public static String getAdminEmail() {
        String env = System.getenv("SPORTGRID_ADMIN_EMAIL");
        if (env != null && !env.isBlank()) return env.trim();
        return CONFIG.getProperty("admin.email", "admin@sportgrid.com").trim();
    }

    /** Retrieves the configured default admin password. */
    public static String getAdminPassword() {
        String env = System.getenv("SPORTGRID_ADMIN_PASSWORD");
        if (env != null && !env.isBlank()) return env;
        return CONFIG.getProperty("admin.password", "Admin@123");
    }
}
