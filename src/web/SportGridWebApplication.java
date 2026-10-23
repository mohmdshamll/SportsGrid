package web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the SportGrid Spring Boot Web Application.
 * Runs the REST API alongside the existing desktop application.
 */
@SpringBootApplication
public class SportGridWebApplication {

    public static void main(String[] args) {
        // Ensure headless mode for server runtime
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(SportGridWebApplication.class, args);
    }
}
