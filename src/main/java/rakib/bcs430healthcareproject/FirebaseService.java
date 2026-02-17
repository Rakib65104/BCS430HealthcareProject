package rakib.bcs430healthcareproject;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.HttpsURLConnection;

/**
 * Service class for Firebase operations related to patient account management.
 * Handles user creation in Firebase Authentication and patient profile storage in Realtime Database.
 */
public class FirebaseService {

    private final FirebaseAuth auth;
    private final DatabaseReference database;

    public FirebaseService() {
        this.auth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Creates a patient account in Firebase.
     * 1. Creates a new user in Firebase Authentication with email and password
     * 2. Stores patient profile data (name, zip, role) in Realtime Database
     *
     * @param email    Patient's email address
     * @param password Patient's password (must be at least 6 characters)
     * @param name     Patient's full name
     * @param zip      Patient's ZIP code (5 digits)
     * @return CompletableFuture containing the patient ID (UID) on success
     * @throws RuntimeException if patient creation fails
     */
    public CompletableFuture<String> createPatient(String email, String password, String name, String zip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Create user in Firebase Authentication
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password)
                        .setDisplayName(name);

                UserRecord userRecord = auth.createUser(request);
                String uid = userRecord.getUid();

                // Step 2: Store patient profile in Realtime Database
                Map<String, Object> patientProfile = new HashMap<>();
                patientProfile.put("uid", uid);
                patientProfile.put("name", name);
                patientProfile.put("email", email);
                patientProfile.put("zip", zip);
                patientProfile.put("role", "PATIENT");
                patientProfile.put("createdAt", System.currentTimeMillis());

                // Store under /patients/{uid} with completion listener
                database.child("patients").child(uid).setValue(
                        patientProfile,
                        (error, ref) -> {
                            if (error != null) {
                                System.err.println("Failed to save patient profile: " + error.getMessage());
                            } else {
                                System.out.println("Patient profile saved to database");
                            }
                        }
                );

                System.out.println("Patient created successfully with UID: " + uid);
                return uid;

            } catch (FirebaseAuthException e) {
                String errorMessage = handleAuthException(e);
                throw new RuntimeException(errorMessage);
            }
        });
    }

    /**
     * Retrieves a patient's profile from the database.
     *
     * @param uid Patient's UID
     * @return CompletableFuture containing the patient profile data
     */
    public CompletableFuture<Map<String, Object>> getPatientProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            // This is a placeholder for getting data
            // In a real implementation, you would retrieve from database
            Map<String, Object> patientData = new HashMap<>();
            patientData.put("uid", uid);
            return patientData;
        });
    }

    /**
     * Handles Firebase Authentication exceptions and returns user-friendly error messages.
     *
     * @param e FirebaseAuthException thrown by Firebase
     * @return User-friendly error message
     */
    private String handleAuthException(FirebaseAuthException e) {
        String details = e.getMessage().toLowerCase();
        
        if (details.contains("email-already-exists")) {
            return "Email is already registered.";
        } else if (details.contains("invalid-password") || details.contains("weak-password")) {
            return "Password must be at least 6 characters.";
        } else if (details.contains("invalid-email")) {
            return "Invalid email address.";
        } else if (details.contains("email-already-in-use")) {
            return "This email is already in use.";
        } else if (details.contains("invalid-argument")) {
            return "Invalid input provided.";
        }
        
        return "Failed to create account: " + e.getMessage();
    }

    /**
     * Authenticates a user with email and password using Firebase REST API.
     * Verifies credentials and returns the user's UID if authentication is successful.
     *
     * @param email User's email
     * @param password User's password
     * @return CompletableFuture containing the user's UID on successful authentication
     */
    public CompletableFuture<String> authenticateUser(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Firebase REST API endpoint for sign in
                String apiKey = getFirebaseApiKey();
                String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;

                // Create request body
                Gson gson = new Gson();
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("email", email);
                requestBody.addProperty("password", password);
                requestBody.addProperty("returnSecureToken", true);

                String jsonPayload = gson.toJson(requestBody);

                // Send HTTP POST request
                HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                // Read response
                int responseCode = connection.getResponseCode();
                String response;

                try (Scanner scanner = new Scanner(connection.getInputStream())) {
                    response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }

                if (responseCode == 200) {
                    // Parse response to extract UID
                    JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                    String uid = responseObj.get("localId").getAsString();
                    System.out.println("User authenticated successfully with UID: " + uid);
                    return uid;
                } else {
                    // Error response
                    try (Scanner scanner = new Scanner(connection.getErrorStream())) {
                        String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        JsonObject errorObj = gson.fromJson(errorResponse, JsonObject.class);
                        JsonObject error = errorObj.getAsJsonObject("error");
                        String message = error.get("message").getAsString();
                        throw new RuntimeException(handleAuthError(message));
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Authentication error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves the Firebase API key from the project configuration.
     * This is extracted from the initialized Firebase app.
     *
     * @return Firebase API Key for REST API calls
     */
    private String getFirebaseApiKey() {
        // The Firebase API key is typically found in the Firebase Console
        // For this implementation, we'll use a hardcoded key or extract from project
        // You should replace this with your actual Firebase API key
        // Find this at: Firebase Console → Project Settings → General → Web API Key
        return "AIzaSyDcxHnS1oHPY7V8_5vVz6F1r8K3Q2L9M8N"; // Replace with your actual API key
    }

    /**
     * Handles Firebase authentication error messages.
     *
     * @param errorMessage Error message from Firebase
     * @return User-friendly error message
     */
    private String handleAuthError(String errorMessage) {
        if (errorMessage.contains("INVALID_LOGIN_CREDENTIALS") || errorMessage.contains("INVALID_PASSWORD")) {
            return "Invalid email or password.";
        } else if (errorMessage.contains("USER_DISABLED")) {
            return "This account has been disabled.";
        } else if (errorMessage.contains("EMAIL_NOT_FOUND")) {
            return "No account found with this email.";
        } else if (errorMessage.contains("INVALID_EMAIL")) {
            return "Invalid email address.";
        } else if (errorMessage.contains("TOO_MANY_ATTEMPTS_LOGIN_RETRY_ACCOUNT")) {
            return "Too many failed login attempts. Please try again later.";
        }
        return "Login failed: " + errorMessage;
    }
}
