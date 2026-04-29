package rakib.bcs430healthcareproject;

import java.nio.file.Files;
import java.nio.file.Path;

final class JavaFxRuntimeSupport {

    private static boolean configured;

    private JavaFxRuntimeSupport() {
    }

    static synchronized void configure() {
        if (configured) {
            return;
        }

        String existingCacheDir = System.getProperty("javafx.cachedir");
        if (existingCacheDir == null || existingCacheDir.isBlank()) {
            Path cacheDir = Path.of("target", "javafx-cache").toAbsolutePath().normalize();
            try {
                Files.createDirectories(cacheDir);
            } catch (Exception e) {
                System.err.println("Warning: Could not create JavaFX cache directory: " + cacheDir);
                System.err.println("Error: " + e.getMessage());
            }
            System.setProperty("javafx.cachedir", cacheDir.toString());
        }

        configured = true;
    }
}
