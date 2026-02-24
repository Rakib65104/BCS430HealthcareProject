package rakib.bcs430healthcareproject;

import javafx.application.Platform;
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

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onCreateDoctorAccount() {
        String name = safe(nameField);
        String email = safe(emailField);
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        String specialty = safe(specialtyField);
        String clinic = safe(clinicNameField);
        String address = safe(addressField);
        String city = safe(cityField);
        String state = safe(stateField).toUpperCase();
        String zip = safe(zipField);

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()
                || specialty.isEmpty() || clinic.isEmpty()
                || address.isEmpty() || city.isEmpty()
                || state.isEmpty() || zip.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!pass.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (pass.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!zip.matches("\\d{5}")) {
            showError("ZIP must be 5 digits.");
            return;
        }
        if (!state.matches("[A-Z]{2}")) {
            showError("State must be 2 letters (ex: NY).");
            return;
        }

        boolean accepting = acceptingNewPatientsCheck.isSelected();

        showError("Creating your doctor account...");

        firebaseService.createDoctor(
                email, pass, name,
                specialty, clinic,
                address, city, state, zip,
                accepting
        ).thenAccept(uid -> Platform.runLater(() -> {
            showSuccess("Doctor account created successfully!");
            SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
        })).exceptionally(ex -> {
            Platform.runLater(() -> showError(ex.getMessage()));
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
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: #00aa00;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}