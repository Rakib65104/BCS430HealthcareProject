package rakib.bcs430healthcareproject;

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

    @FXML
    private void onCreatePatientAccount() {
        String name = safe(nameField);
        String email = safe(emailField);
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String zip = safe(zipField);

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

        // TODO: Connect Firebase:
        // 1) Create user with email/pass
        // 2) Save role=PATIENT + profile (name, zip)
        // 3) Route to Patient Home

        showError("Patient account validated (Firebase next).");
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
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
}