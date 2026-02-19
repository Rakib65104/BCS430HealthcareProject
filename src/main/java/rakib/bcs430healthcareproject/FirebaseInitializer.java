package rakib.bcs430healthcareproject;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes Firebase Admin SDK for the Healthcare Application.
 * This should be called once when the application starts.
 */
public class FirebaseInitializer {
    private static boolean initialized = false;

    private FirebaseInitializer() {
        // Utility class, prevent instantiation
    }

    /**
     * Initializes Firebase Admin SDK using the service account key.json file.
     * This method should be called once at application startup.
     *
     * @throws RuntimeException if Firebase initialization fails
     */
    public static void initialize() {
        if (initialized) {
            return; // Already initialized
        }

        try {
            // Load the service account key from resources
            InputStream serviceAccountStream = FirebaseInitializer.class
                    .getResourceAsStream("/rakib/bcs430healthcareproject/key.json");

            if (serviceAccountStream == null) {
                throw new RuntimeException("key.json not found in resources");
            }

            // Create Firebase options with the credential
            try {
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
                
                // Check if Firebase app is already initialized
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseOptions options = new FirebaseOptions.Builder()
                            .setCredentials(credentials)
                            .setDatabaseUrl("https://bcs430seniorproject.firebaseio.com")
                            .build();

                    // Initialize Firebase
                    FirebaseApp.initializeApp(options);
                }
                
                initialized = true;
                System.out.println("Firebase initialized successfully!");
                
            } catch (IllegalStateException e) {
                // Firebase app already initialized
                initialized = true;
                System.out.println("Firebase app already initialized");
            }

        } catch (IOException e) {
            System.err.println("IOException during Firebase initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error during Firebase initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if Firebase has been initialized.
     *
     * @return true if Firebase is initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
