package rakib.bcs430healthcareproject;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using PBKDF2-like approach.
 * Provides secure password handling for the application.
 */
public class PasswordHasher {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * Generates a salt for password hashing
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password with a salt using SHA-256
     *
     * @param password Plain text password
     * @param salt     Salt for hashing
     * @return Hashed password
     * @throws RuntimeException if hashing fails
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies a plain text password against a hashed password and salt
     *
     * @param password       Plain text password to verify
     * @param hashedPassword Stored hashed password
     * @param salt           Stored salt
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hashedPassword, String salt) {
        try {
            String passwordHash = hashPassword(password, salt);
            return passwordHash.equals(hashedPassword);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
}
