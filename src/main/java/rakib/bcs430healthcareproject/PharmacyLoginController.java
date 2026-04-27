package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PharmacyLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please enter email and password.", true);
            return;
        }

        showMessage("Signing in...", false);

        firebaseService.authenticatePharmacy(email, password)
                .thenCompose(uid -> firebaseService.getPharmacyProfile(uid)
                        .thenApply(profile -> {
                            UserContext.getInstance().setPharmacyUserData(uid, profile);
                            return profile;
                        }))
                .thenAccept(profile -> Platform.runLater(() ->
                        SceneRouter.go("pharmacy-prescriptions-view.fxml", "Pharmacy Portal")))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showMessage(cleanErrorMessage(ex), true));
                    return null;
                });
    }

    @FXML
    private void onGoSignup() {
        SceneRouter.go("pharmacy-signup-view.fxml", "Pharmacy Sign Up");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("pharmacy-auth-view.fxml", "Pharmacy Portal");
    }

    @FXML
    private void onForgotPassword() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            showAlert(Alert.AlertType.WARNING, "Email Required",
                    "Please enter your registered email address in the Email field before requesting a password reset.");
            return;
        }

        showMessage("Sending reset link...", false);

        firebaseService.sendPasswordResetEmail(email)
                .thenAccept(v -> Platform.runLater(() -> {
                    showMessage("", false); // Clear the message label
                    showAlert(Alert.AlertType.INFORMATION, "Password Reset Sent",
                            "A password reset link has been successfully sent to " + email + ". Please check your inbox and spam folder.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showMessage("", false);
                        showAlert(Alert.AlertType.ERROR, "Reset Failed", cleanErrorMessage(ex));
                    });
                    return null;
                });
    }

    @FXML
    private void onForgotUsername() {
        // Because Firebase uses email as the primary login identifier, users who forget
        // their login email usually need to contact administrative support.
        showAlert(Alert.AlertType.INFORMATION, "Forgot Username/Email",
                "Your username is the email address you used to register.\n\n" +
                        "If you no longer remember which email you used, please contact the system administrator or IT support to recover your account.");
    }

    private void showMessage(String message, boolean isError) {
        if (message == null || message.isEmpty()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            return;
        }
        errorLabel.setText(message);
        errorLabel.setStyle(isError ? "-fx-text-fill:#DC2626;" : "-fx-text-fill:#0F766E;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String cleanErrorMessage(Throwable throwable) {
        Throwable cause = throwable != null && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}