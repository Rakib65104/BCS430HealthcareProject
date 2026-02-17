package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private FirebaseService firebaseService;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email.");
            return;
        }

        // Show loading status
        showError("Logging in...");
        
        // Authenticate user with Firebase
        firebaseService.authenticateUser(email, pass)
                .thenAccept(uid -> {
                    // Success: Run on JavaFX thread
                    Platform.runLater(() -> {
                        System.out.println("User logged in with UID: " + uid);
                        // Route to patient dashboard
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
    private void onGoSignup() {
        SceneRouter.go("signup-role-view.fxml", "Sign Up");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: #cc0000;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
}
