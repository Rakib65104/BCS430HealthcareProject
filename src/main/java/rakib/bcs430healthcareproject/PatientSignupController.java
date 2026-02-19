package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PatientSignupController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField zipField;
    @FXML private Label errorLabel;

    private FirebaseService firebaseService;

    /**
     * Initialize the controller.
     * This method is called by JavaFX after the FXML is loaded.
     */
    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    /**
     * Handles patient account creation.
     * Validates input, then creates account in Firebase.
     */
    @FXML
    private void onCreatePatientAccount() {
        String name = safe(nameField);
        String email = safe(emailField);
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String zip = safe(zipField);

        // Validate input
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty() || zip.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email.");
            return;
        }
        if (pass.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!pass.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (!zip.matches("\\d{5}")) {
            showError("ZIP must be 5 digits.");
            return;
        }

        // Show loading status
        showError("Creating your patient account...");

        // Create patient account in Firebase
        firebaseService.createPatient(email, pass, name, zip)
                .thenAccept(uid -> {
                    // Success: Run on JavaFX thread
                    Platform.runLater(() -> {
                        showSuccess("Account created successfully! UID: " + uid);
                        // Route to Patient Dashboard
                        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
                    });
                })
                .exceptionally(throwable -> {
                    // Failure: Run on JavaFX thread
                    Platform.runLater(() -> {
                        showError(throwable.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("signup-role-view.fxml", "Sign Up");
    }

    private String safe(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: #cc0000;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: #00aa00;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
}