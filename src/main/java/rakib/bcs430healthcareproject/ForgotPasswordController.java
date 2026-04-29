package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onSendResetLink() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            showStatus("Please enter a valid email address.", false);
            return;
        }

        // Show loading status
        showStatus("Sending reset link...", true);

        // Call FirebaseService to send the reset email
        firebaseService.sendPasswordResetEmail(email)
                .thenAccept(v -> Platform.runLater(() -> {
                    // Success message (Green)
                    statusLabel.setText("Password reset email sent! Please check your inbox.");
                    statusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 13; -fx-font-weight: bold;");
                }))
                .exceptionally(e -> {
                    // Error message (Red)
                    Platform.runLater(() -> showStatus("Failed to send reset email. Verify your email address.", false));
                    return null;
                });
    }

    @FXML
    private void onForgotUsername() {
        // Firebase uses the email as the username.
        showStatus("Your email is your username. If you have lost access to your email, please contact IT support.", true);
    }

    @FXML
    private void onBackToLogin() {
        // Route back to the login screen
        SceneRouter.go("login-view.fxml", "HealthConnect Login");
    }

    /**
     * Helper method to display messages to the user.
     * @param msg The message to display.
     * @param isInfo If true, displays in theme color (teal). If false, displays as error (red).
     */
    private void showStatus(String msg, boolean isInfo) {
        statusLabel.setText(msg);
        if (isInfo) {
            statusLabel.setStyle("-fx-text-fill: #0F766E;"); // Teal for info
        } else {
            statusLabel.setStyle("-fx-text-fill: #DC2626;"); // Red for errors
        }
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }
}