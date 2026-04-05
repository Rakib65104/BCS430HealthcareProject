package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class DoctorPatientHistoryController {

    @FXML private Label patientNameLabel;
    @FXML private Label patientAgeGenderLabel;
    @FXML private Label patientContactLabel;
    @FXML private Label bloodTypeLabel;
    @FXML private Label vaccinationStatusLabel;
    @FXML private Label heightWeightLabel;
    @FXML private Label emergencyContactLabel;
    @FXML private Label statusLabel;

    @FXML private TextArea allergiesArea;
    @FXML private TextArea medicationsArea;
    @FXML private TextArea chronicConditionsArea;
    @FXML private TextArea medicalHistoryArea;

    private FirebaseService firebaseService;
    private UserContext userContext;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        setReadOnly(allergiesArea);
        setReadOnly(medicationsArea);
        setReadOnly(chronicConditionsArea);
        setReadOnly(medicalHistoryArea);

        loadPatientHistory();
    }

    private void loadPatientHistory() {
        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            statusLabel.setText("Access denied.");
            return;
        }

        String selectedPatientUid = userContext.getSelectedPatientUid();
        PatientProfile selectedPatientProfile = userContext.getSelectedPatientProfile();

        if (selectedPatientProfile != null) {
            populateFields(selectedPatientProfile);
            statusLabel.setText("Patient history loaded.");
            return;
        }

        if (selectedPatientUid == null || selectedPatientUid.isBlank()) {
            statusLabel.setText("No patient selected.");
            return;
        }

        statusLabel.setText("Loading patient history...");

        firebaseService.getPatientProfile(selectedPatientUid)
                .thenAccept(profile -> Platform.runLater(() -> {
                    userContext.setSelectedPatientProfile(profile);
                    populateFields(profile);
                    statusLabel.setText("Patient history loaded.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            statusLabel.setText("Failed to load patient history.")
                    );
                    ex.printStackTrace();
                    return null;
                });
    }

    private void populateFields(PatientProfile profile) {
        if (profile == null) {
            statusLabel.setText("Patient profile not found.");
            return;
        }

        patientNameLabel.setText(valueOrDefault(profile.getName()));

        String ageText = profile.getAge() == null ? "Not provided" : String.valueOf(profile.getAge());
        String genderText = valueOrDefault(profile.getGender());
        patientAgeGenderLabel.setText("Age: " + ageText + "    Gender: " + genderText);

        String phoneText = valueOrDefault(profile.getPhoneNumber());
        String emailText = valueOrDefault(profile.getEmail());
        patientContactLabel.setText("Phone: " + phoneText + "    Email: " + emailText);

        allergiesArea.setText(valueOrDefault(profile.getAllergies()));
        medicationsArea.setText(valueOrDefault(profile.getCurrentMedications()));
        chronicConditionsArea.setText(valueOrDefault(profile.getChronicConditions()));
        medicalHistoryArea.setText(valueOrDefault(profile.getMedicalHistory()));

        bloodTypeLabel.setText(valueOrDefault(profile.getBloodType()));
        vaccinationStatusLabel.setText(valueOrDefault(profile.getVaccinationStatus()));

        String heightText = valueOrDefault(profile.getHeight());
        String weightText = profile.getWeight() == null ? "Not provided" : profile.getWeight().toString();
        heightWeightLabel.setText("Height: " + heightText + "    Weight: " + weightText);

        String emergencyName = valueOrDefault(profile.getEmergencyContactName());
        String emergencyRelationship = valueOrDefault(profile.getEmergencyContactRelationship());
        String emergencyPhone = valueOrDefault(profile.getEmergencyContactPhone());
        emergencyContactLabel.setText(
                emergencyName + "    |    " + emergencyRelationship + "    |    " + emergencyPhone
        );
    }

    private void setReadOnly(TextArea area) {
        area.setEditable(false);
        area.setWrapText(true);
        area.setFocusTraversable(false);
    }

    private String valueOrDefault(String value) {
        return (value == null || value.isBlank()) ? "Not provided" : value;
    }

    @FXML
    private void handleBack() {
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }
}