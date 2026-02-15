package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class DoctorSignupController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private TextField specialtyField;
    @FXML private TextField clinicNameField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField zipField;

    @FXML private CheckBox acceptingNewPatientsCheck;
    @FXML private Label errorLabel;

    @FXML
    private void onCreateDoctorAccount() {
        String name = safe(nameField);
        String email = safe(emailField);
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        String specialty = safe(specialtyField);
        String clinic = safe(clinicNameField);
        String address = safe(addressField);
        String city = safe(cityField);
        String state = safe(stateField);
        String zip = safe(zipField);

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()
                || specialty.isEmpty() || clinic.isEmpty() || address.isEmpty()
                || city.isEmpty() || state.isEmpty() || zip.isEmpty()) {
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
        if (state.length() != 2) {
            showError("State should be 2 letters (ex: NY).");
            return;
        }

        boolean accepting = acceptingNewPatientsCheck.isSelected();

        // TODO: Connect Firebase:
        // 1) Create user with email/pass
        // 2) Save role=DOCTOR + doctor profile (specialty, address, city/state/zip, etc.)
        // 3) Route to Doctor Home

        showError("Doctor account validated (Firebase next). Accepting new patients: " + accepting);
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