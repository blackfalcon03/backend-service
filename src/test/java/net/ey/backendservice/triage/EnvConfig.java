package net.ey.backendservice.triage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to resolve configuration with priority:
 * 1. System Environment Variable (CI / Shell / Docker)
 * 2. JVM System Property (-Dkey=value)
 * 3. Local .env file
 * 4. Default fallback
 */
public class EnvConfig {

    private static final Map<String, String> ENV_FILE_VARS = new HashMap<>();

    static {
        loadDotEnv();
    }

    private static void loadDotEnv() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            // Also check parent directory if running from a sub-module
            envPath = Path.of("..", ".env");
        }

        if (Files.exists(envPath)) {
            try {
                for (String line : Files.readAllLines(envPath)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }

                    int equalsIndex = trimmed.indexOf('=');
                    String key = trimmed.substring(0, equalsIndex).trim();
                    String value = trimmed.substring(equalsIndex + 1).trim();

                    // Strip optional quotes
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    ENV_FILE_VARS.put(key, value);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not read .env file: " + e.getMessage());
            }
        }
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        // Priority 1: System environment variable
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            return val;
        }

        // Priority 2: System property
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) {
            return val;
        }

        // Priority 3: Local .env file
        val = ENV_FILE_VARS.get(key);
        if (val != null && !val.isBlank()) {
            return val;
        }

        return defaultValue;
    }
}
