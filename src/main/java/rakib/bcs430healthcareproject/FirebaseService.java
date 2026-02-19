package rakib.bcs430healthcareproject;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
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
import java.util.concurrent.ExecutionException;
import javax.net.ssl.HttpsURLConnection;

/**
 * Service class for Firebase operations related to patient account management.
 * Handles user creation in Firebase Authentication and patient profile storage in Firestore.
 * Uses custom password hashing for login without requiring Firebase Web API Key.
 */
public class FirebaseService {

    private final FirebaseAuth auth;
    private final Firestore firestore;
    private static final String PATIENTS_COLLECTION = "patients";
    private static final String USERS_COLLECTION = "users";

    public FirebaseService() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirestoreClient.getFirestore();
    }

    /**
     * Creates a patient account in Firebase.
     * 1. Creates a new user in Firebase Authentication with email and password
     * 2. Stores patient profile data with hashed password in Firestore
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

                // Step 2: Create PatientProfile with hashed password and store in Firestore
                PatientProfile profile = new PatientProfile(uid, name, email, zip);
                
                // Hash password for storage
                String passwordSalt = PasswordHasher.generateSalt();
                String passwordHash = PasswordHasher.hashPassword(password, passwordSalt);
                profile.setPasswordHash(passwordHash);
                profile.setPasswordSalt(passwordSalt);
                
                // Store under /patients/{uid}
                ApiFuture<?> future = firestore.collection(PATIENTS_COLLECTION).document(uid).set(profile);
                future.get(); // Wait for completion
                
                System.out.println("Patient created successfully with UID: " + uid);
                return uid;

            } catch (FirebaseAuthException e) {
                String errorMessage = handleAuthException(e);
                throw new RuntimeException(errorMessage);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to save patient profile: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Authenticates a patient with email and password using custom Firestore verification.
     * NO EXTERNAL API KEY REQUIRED - uses local Firebase Admin SDK only.
     *
     * @param email    Patient's email
     * @param password Patient's password
     * @return CompletableFuture containing the patient's UID on successful authentication
     */
    public CompletableFuture<String> authenticateUser(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Authenticating user with email: " + email);
                
                // Query Firestore for user by email
                ApiFuture<QuerySnapshot> query = firestore.collection(PATIENTS_COLLECTION)
                        .whereEqualTo("email", email)
                        .get();
                
                QuerySnapshot querySnapshot = query.get();
                
                if (querySnapshot.isEmpty()) {
                    throw new RuntimeException("No account found with this email address.");
                }
                
                if (querySnapshot.size() > 1) {
                    throw new RuntimeException("Multiple accounts found with this email. Please contact support.");
                }
                
                // Get the patient profile
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                PatientProfile profile = document.toObject(PatientProfile.class);
                
                if (profile == null) {
                    throw new RuntimeException("Failed to load patient profile.");
                }
                
                // Verify password
                if (profile.getPasswordHash() == null || profile.getPasswordSalt() == null) {
                    throw new RuntimeException("Account security data not found. Please contact support.");
                }
                
                boolean passwordMatches = PasswordHasher.verifyPassword(
                        password, 
                        profile.getPasswordHash(), 
                        profile.getPasswordSalt()
                );
                
                if (!passwordMatches) {
                    throw new RuntimeException("Invalid email or password.");
                }
                
                String uid = profile.getUid();
                System.out.println("User authenticated successfully with UID: " + uid);
                return uid;
                
            } catch (ExecutionException | InterruptedException e) {
                System.err.println("Authentication database error: " + e.getMessage());
                throw new RuntimeException("Authentication failed: Database error - " + e.getMessage(), e);
            } catch (RuntimeException e) {
                System.err.println("Authentication failed: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                System.err.println("Unexpected authentication error: " + e.getMessage());
                throw new RuntimeException("Authentication error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves a patient's profile from Firestore.
     *
     * @param uid Patient's UID
     * @return CompletableFuture containing the patient profile data
     */
    public CompletableFuture<PatientProfile> getPatientProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<DocumentSnapshot> future = firestore.collection(PATIENTS_COLLECTION).document(uid).get();
                DocumentSnapshot document = future.get();
                
                if (document.exists()) {
                    PatientProfile profile = document.toObject(PatientProfile.class);
                    System.out.println("Patient profile loaded for UID: " + uid);
                    return profile;
                } else {
                    throw new RuntimeException("Patient profile not found");
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to retrieve patient profile: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Updates a patient's profile in Firestore.
     *
     * @param uid Patient's UID
     * @param profile Updated patient profile
     * @return CompletableFuture that completes when the update is done
     */
    public CompletableFuture<Void> updatePatientProfile(String uid, PatientProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                profile.setUpdatedAt(System.currentTimeMillis());
                ApiFuture<?> future = firestore.collection(PATIENTS_COLLECTION).document(uid).set(profile);
                future.get(); // Wait for completion
                System.out.println("Patient profile updated for UID: " + uid);
                return null;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to update patient profile: " + e.getMessage(), e);
            }
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
     * Handles Firebase authentication error messages from REST API.
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

