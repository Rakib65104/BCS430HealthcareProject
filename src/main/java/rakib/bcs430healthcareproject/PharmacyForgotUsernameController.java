package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PharmacyForgotUsernameController {

    @FXML private TextField recoveryEmailField;
    @FXML private Label statusLabel;

    @FXML
    private void onRetrieveUsername() {
        String email = recoveryEmailField.getText() == null ? "" : recoveryEmailField.getText().trim();

        if (email.isEmpty()) {
            showMessage("Please enter your recovery email address.", true);
            return;
        }

        // Add your Firebase retrieval/email logic here
        // For example: firebaseService.sendUsernameReminder(email)...

        showMessage("If an account exists, your username has been sent.", false);
    }

    @FXML
    private void onBackToLogin() {
        // Navigates back to the main Pharmacy login view (ensure the FXML name matches your project exactly)
        SceneRouter.go("pharmacy-login-view.fxml", "Pharmacy Sign In");
    }

    private void showMessage(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill:#DC2626;" : "-fx-text-fill:#0F766E;");
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }
}