package web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check REST endpoint for SportGrid Web API.
 * Route: GET /api/health
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthCheckController {

    private final Instant startTime = Instant.now();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "SportGrid Web API");
        response.put("version", "1.0.0");
        response.put("startedAt", startTime.toString());
        response.put("timestamp", Instant.now().toString());
        response.put("jvmVersion", System.getProperty("java.version"));
        return ResponseEntity.ok(response);
    }
}
