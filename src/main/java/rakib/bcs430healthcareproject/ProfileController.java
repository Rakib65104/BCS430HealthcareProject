package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for the Patient Profile View.
 * Allows patients to view and edit their profile information.
 */
public class ProfileController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private TextField insuranceNumberField;
    @FXML private TextField insuranceCompanyField;
    @FXML private TextArea allergiesArea;
    @FXML private TextArea medicalHistoryArea;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile currentProfile;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        
        // Setup gender combo box
        genderComboBox.getItems().addAll("Not specified", "Male", "Female", "Other");
        
        // Load current profile
        if (userContext.isLoggedIn()) {
            currentProfile = userContext.getProfile();
            if (currentProfile != null) {
                loadProfileData();
            } else {
                showStatus("Profile not available", true);
            }
        } else {
            showStatus("Not logged in", true);
            SceneRouter.go("login-view.fxml", "Login");
        }
        
        // Setup button visibility
        updateButtonVisibility();
    }

    /**
     * Load profile data into the UI fields (read-only mode)
     */
    private void loadProfileData() {
        if (currentProfile == null) return;
        
        nameField.setText(currentProfile.getName() != null ? currentProfile.getName() : "");
        ageField.setText(currentProfile.getAge() != null ? currentProfile.getAge().toString() : "");
        
        if (currentProfile.getDateOfBirth() != null) {
            try {
                dateOfBirthPicker.setValue(java.time.LocalDate.parse(currentProfile.getDateOfBirth()));
            } catch (Exception e) {
                System.err.println("Failed to parse date: " + e.getMessage());
            }
        }
        
        String gender = currentProfile.getGender() != null ? currentProfile.getGender() : "Not specified";
        genderComboBox.setValue(gender);
        
        insuranceNumberField.setText(currentProfile.getInsuranceNumber() != null ? currentProfile.getInsuranceNumber() : "");
        insuranceCompanyField.setText(currentProfile.getInsuranceCompany() != null ? currentProfile.getInsuranceCompany() : "");
        allergiesArea.setText(currentProfile.getAllergies() != null ? currentProfile.getAllergies() : "");
        medicalHistoryArea.setText(currentProfile.getMedicalHistory() != null ? currentProfile.getMedicalHistory() : "");
        
        setFieldsEditable(false);
    }

    @FXML
    private void onEdit() {
        isEditMode = true;
        setFieldsEditable(true);
        updateButtonVisibility();
        showStatus("Editing mode enabled", false);
    }

    @FXML
    private void onSave() {
        // Validate required fields
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showStatus("Name is required", true);
            return;
        }
        
        System.out.println("Saving profile for UID: " + userContext.getUid());
        
        // Update profile with new values
        currentProfile.setName(nameField.getText().trim());
        
        if (dateOfBirthPicker.getValue() != null) {
            currentProfile.setDateOfBirth(dateOfBirthPicker.getValue().toString());
            System.out.println("Set DOB: " + dateOfBirthPicker.getValue().toString());
        }
        
        try {
            if (ageField.getText() != null && !ageField.getText().trim().isEmpty()) {
                currentProfile.setAge(Integer.parseInt(ageField.getText().trim()));
                System.out.println("Set Age: " + ageField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid age value", true);
            return;
        }
        
        if (genderComboBox.getValue() != null) {
            currentProfile.setGender(genderComboBox.getValue());
            System.out.println("Set Gender: " + genderComboBox.getValue());
        }
        
        if (insuranceNumberField.getText() != null) {
            currentProfile.setInsuranceNumber(insuranceNumberField.getText().trim());
        }
        
        if (insuranceCompanyField.getText() != null) {
            currentProfile.setInsuranceCompany(insuranceCompanyField.getText().trim());
        }
        
        if (allergiesArea.getText() != null) {
            currentProfile.setAllergies(allergiesArea.getText().trim());
        }
        
        if (medicalHistoryArea.getText() != null) {
            currentProfile.setMedicalHistory(medicalHistoryArea.getText().trim());
        }
        
        // Save to Firestore
        showStatus("Saving profile...", false);
        firebaseService.updatePatientProfile(userContext.getUid(), currentProfile)
                .thenAccept(v -> {
                    Platform.runLater(() -> {
                        System.out.println("Profile saved successfully to Firestore");
                        userContext.updatePatientProfile(currentProfile);
                        isEditMode = false;
                        setFieldsEditable(false);
                        updateButtonVisibility();
                        showStatus("Profile saved successfully!", false);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        System.err.println("Error saving profile: " + e.getMessage());
                        e.printStackTrace();
                        showStatus("Failed to save profile: " + e.getMessage(), true);
                    });
                    return null;
                });
    }

    @FXML
    private void onCancel() {
        isEditMode = false;
        loadProfileData();
        updateButtonVisibility();
        showStatus("Changes cancelled", false);
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    /**
     * Set editable state for all profile fields
     */
    private void setFieldsEditable(boolean editable) {
        nameField.setEditable(editable);
        dateOfBirthPicker.setDisable(!editable);
        ageField.setEditable(editable);
        genderComboBox.setDisable(!editable);
        insuranceNumberField.setEditable(editable);
        insuranceCompanyField.setEditable(editable);
        allergiesArea.setEditable(editable);
        medicalHistoryArea.setEditable(editable);
    }

    /**
     * Update button visibility based on current mode
     */
    private void updateButtonVisibility() {
        if (isEditMode) {
            editButton.setManaged(false);
            editButton.setVisible(false);
            saveButton.setManaged(true);
            saveButton.setVisible(true);
            cancelButton.setManaged(true);
            cancelButton.setVisible(true);
        } else {
            editButton.setManaged(true);
            editButton.setVisible(true);
            saveButton.setManaged(false);
            saveButton.setVisible(false);
            cancelButton.setManaged(false);
            cancelButton.setVisible(false);
        }
    }

    /**
     * Display status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
        }
        
        // Auto-hide status message after 5 seconds
        if (!isError) {
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(5000);
                    Platform.runLater(() -> {
                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}
