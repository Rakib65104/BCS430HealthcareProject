package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for sending a prescription to a patient's pharmacy.
 */
public class DoctorPrescriptionController {

    @FXML private Label patientNameLabel;
    @FXML private Label doctorNameLabel;
    @FXML private TextField pharmacyNameField;
    @FXML private TextArea pharmacyAddressArea;
    @FXML private TextField pharmacyPhoneField;
    @FXML private TextArea medicationInformationArea;
    @FXML private TextArea instructionsArea;
    @FXML private Label statusLabel;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile selectedPatient;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        selectedPatient = userContext.getSelectedPatientProfile();
        if (selectedPatient == null) {
            SceneRouter.go("doctor-patients-view.fxml", "My Patients");
            return;
        }

        patientNameLabel.setText(
                selectedPatient.getName() != null ? selectedPatient.getName() : "Patient"
        );

        doctorNameLabel.setText(
                userContext.getName() != null
                        ? "Prescribing Doctor: Dr. " + userContext.getName()
                        : "Prescribing Doctor"
        );
    }

    @FXML
    private void onSendPrescription() {

        String pharmacyName = safeTrim(pharmacyNameField.getText());
        String pharmacyAddress = safeTrim(pharmacyAddressArea.getText());
        String pharmacyPhone = safeTrim(pharmacyPhoneField.getText());
        String medicationInformation = safeTrim(medicationInformationArea.getText());
        String instructions = safeTrim(instructionsArea.getText());

        // Validation
        if (pharmacyAddress.isBlank()) {
            showStatus("Pharmacy address is required.", true);
            return;
        }
        if (pharmacyPhone.isBlank()) {
            showStatus("Pharmacy phone number is required.", true);
            return;
        }
        if (medicationInformation.isBlank()) {
            showStatus("Medication information is required.", true);
            return;
        }
        if (instructions.isBlank()) {
            showStatus("Instructions are required.", true);
            return;
        }

        Prescription prescription = new Prescription();
        prescription.setDoctorUid(userContext.getUid());
        prescription.setDoctorName(userContext.getName());
        prescription.setPatientUid(selectedPatient.getUid());
        prescription.setPatientName(selectedPatient.getName());
        prescription.setPharmacyName(pharmacyName);
        prescription.setPharmacyAddress(pharmacyAddress);
        prescription.setPharmacyPhoneNumber(pharmacyPhone);
        prescription.setMedicationInformation(medicationInformation);
        prescription.setInstructions(instructions);

        sendButton.setDisable(true);
        showStatus("Sending prescription...", false);

        firebaseService.savePrescription(prescription)
                .thenAccept(prescriptionId -> Platform.runLater(() -> {
                    showStatus("Prescription sent successfully. Reference ID: " + prescriptionId, false);
                    sendButton.setDisable(false);
                    clearForm();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showStatus("Failed to send prescription: " + cleanErrorMessage(e), true);
                        sendButton.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }

    private void clearForm() {
        pharmacyNameField.clear();
        pharmacyAddressArea.clear();
        pharmacyPhoneField.clear();
        medicationInformationArea.clear();
        instructionsArea.clear();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}