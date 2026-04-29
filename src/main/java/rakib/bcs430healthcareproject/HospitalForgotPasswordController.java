package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HospitalForgotPasswordController {

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
            showStatus("Please enter a valid email address.", true);
            return;
        }

        showStatus("Sending reset link...", false);

        firebaseService.sendPasswordResetEmail(email)
                .thenAccept(v -> Platform.runLater(() -> {
                    showStatus("Password reset email sent! Check your inbox.", false);
                    statusLabel.setStyle("-fx-text-fill: #16A34A;"); // Green color for success
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus(cleanErrorMessage(e), true));
                    return null;
                });
    }

    @FXML
    private void onForgotUsername() {
        showStatus("Your email is your username. If you lost access to your registered email, please contact IT support.", false);
    }

    @FXML
    private void onBackToLogin() {
        // Adjust this string to whatever your Hospital login FXML file is actually named.
        // It might be "hospital-login-view.fxml" or similar.
        SceneRouter.go("hospital-login-view.fxml", "Hospital Sign In");
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #DC2626;" : "-fx-text-fill: #0F766E;");
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    private String cleanErrorMessage(Throwable throwable) {
        Throwable cause = throwable != null && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}