package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DoctorProfileController {

    @FXML private Button backButton;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;

    @FXML private TextField nameField;
    @FXML private TextField specialtyField;
    @FXML private TextField licenseField;
    @FXML private TextArea bioArea;

    @FXML private TextField clinicNameField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField zipField;

    @FXML private TextField phoneField;
    @FXML private TextField publicEmailField;

    @FXML private CheckBox acceptingNewPatientsCheck;
    @FXML private TextArea insuranceArea;
    @FXML private TextArea hoursArea;

    @FXML private ComboBox<String> visitTypeComboBox;
    @FXML private TextArea notesArea;

    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // Snapshot of original values for Cancel
    private DoctorProfileSnapshot original;

    @FXML
    public void initialize() {
        visitTypeComboBox.getItems().setAll(
                "In-Person",
                "Telehealth",
                "In-Person + Telehealth"
        );

        // Start in view mode
        setEditing(false);

        // Optional: load mock data for now
        loadMockData();
        original = snapshot();
    }

    private void loadMockData() {
        nameField.setText("Dr. Rakib Ahmed");
        specialtyField.setText("Family Medicine");
        clinicNameField.setText("TealCare Clinic");
        addressField.setText("123 Main St");
        cityField.setText("Farmingdale");
        stateField.setText("NY");
        zipField.setText("11735");
        phoneField.setText("(555) 555-5555");
        publicEmailField.setText("office@tealcare.com");
        acceptingNewPatientsCheck.setSelected(true);
        insuranceArea.setText("Aetna, BCBS, UnitedHealthcare");
        hoursArea.setText("Mon-Fri 9am-5pm\nSat 9am-1pm");
        visitTypeComboBox.setValue("In-Person + Telehealth");
        bioArea.setText("Board-certified physician focused on preventive care and patient education.");
    }

    @FXML
    private void onBack() {
        // change this to your doctor home view later
        SceneRouter.go("signup-role-view.fxml", "Sign Up");
    }

    @FXML
    private void onEdit() {
        original = snapshot();
        setEditing(true);
        showStatus("Editing enabled", false);
    }

    @FXML
    private void onCancel() {
        if (original != null) restore(original);
        setEditing(false);
        showStatus("Changes cancelled", false);
    }

    @FXML
    private void onSave() {
        // Basic validation
        if (nameField.getText().trim().isEmpty()
                || specialtyField.getText().trim().isEmpty()
                || clinicNameField.getText().trim().isEmpty()
                || addressField.getText().trim().isEmpty()
                || cityField.getText().trim().isEmpty()
                || stateField.getText().trim().length() != 2
                || !zipField.getText().trim().matches("\\d{5}")) {
            showStatus("Please fill required fields (State=2 letters, ZIP=5 digits).", true);
            return;
        }

        // TODO later: Save to Firebase/DB

        setEditing(false);
        showStatus("Profile saved successfully ✅", false);
    }

    private void setEditing(boolean editing) {
        // In edit mode = fields enabled
        boolean disabled = !editing;

        nameField.setDisable(disabled);
        specialtyField.setDisable(disabled);
        licenseField.setDisable(disabled);
        bioArea.setDisable(disabled);

        clinicNameField.setDisable(disabled);
        addressField.setDisable(disabled);
        cityField.setDisable(disabled);
        stateField.setDisable(disabled);
        zipField.setDisable(disabled);

        phoneField.setDisable(disabled);
        publicEmailField.setDisable(disabled);

        acceptingNewPatientsCheck.setDisable(disabled);
        insuranceArea.setDisable(disabled);
        hoursArea.setDisable(disabled);

        visitTypeComboBox.setDisable(disabled);
        notesArea.setDisable(disabled);

        // Toggle buttons
        editButton.setVisible(!editing);
        editButton.setManaged(!editing);

        saveButton.setVisible(editing);
        saveButton.setManaged(editing);

        cancelButton.setVisible(editing);
        cancelButton.setManaged(editing);
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #dc2626; -fx-font-size: 11; -fx-padding: 5 0 0 0;"
                : "-fx-text-fill: #dcfce7; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
    }

    private DoctorProfileSnapshot snapshot() {
        return new DoctorProfileSnapshot(
                text(nameField), text(specialtyField), text(licenseField), bioArea.getText(),
                text(clinicNameField), text(addressField), text(cityField), text(stateField), text(zipField),
                text(phoneField), text(publicEmailField),
                acceptingNewPatientsCheck.isSelected(),
                insuranceArea.getText(), hoursArea.getText(),
                visitTypeComboBox.getValue(),
                notesArea.getText()
        );
    }

    private void restore(DoctorProfileSnapshot s) {
        nameField.setText(s.name);
        specialtyField.setText(s.specialty);
        licenseField.setText(s.license);
        bioArea.setText(s.bio);

        clinicNameField.setText(s.clinicName);
        addressField.setText(s.address);
        cityField.setText(s.city);
        stateField.setText(s.state);
        zipField.setText(s.zip);

        phoneField.setText(s.phone);
        publicEmailField.setText(s.publicEmail);

        acceptingNewPatientsCheck.setSelected(s.acceptingNewPatients);
        insuranceArea.setText(s.insurance);
        hoursArea.setText(s.hours);

        visitTypeComboBox.setValue(s.visitType);
        notesArea.setText(s.notes);
    }

    private String text(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private static class DoctorProfileSnapshot {
        final String name, specialty, license, bio;
        final String clinicName, address, city, state, zip;
        final String phone, publicEmail;
        final boolean acceptingNewPatients;
        final String insurance, hours;
        final String visitType, notes;

        DoctorProfileSnapshot(String name, String specialty, String license, String bio,
                              String clinicName, String address, String city, String state, String zip,
                              String phone, String publicEmail,
                              boolean acceptingNewPatients,
                              String insurance, String hours,
                              String visitType, String notes) {
            this.name = name;
            this.specialty = specialty;
            this.license = license;
            this.bio = bio;
            this.clinicName = clinicName;
            this.address = address;
            this.city = city;
            this.state = state;
            this.zip = zip;
            this.phone = phone;
            this.publicEmail = publicEmail;
            this.acceptingNewPatients = acceptingNewPatients;
            this.insurance = insurance;
            this.hours = hours;
            this.visitType = visitType;
            this.notes = notes;
        }
    }
}