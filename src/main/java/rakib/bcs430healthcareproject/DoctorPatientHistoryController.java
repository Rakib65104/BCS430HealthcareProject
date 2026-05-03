package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorPatientHistoryController {

    @FXML private Label patientNameLabel;
    @FXML private Label statusLabel;

    // Overview
    @FXML private Label overviewAgeLabel;
    @FXML private Label overviewGenderLabel;
    @FXML private Label overviewDobLabel;

    // Personal Info
    @FXML private Label fullNameLabel;
    @FXML private Label ageLabel;
    @FXML private Label genderLabel;
    @FXML private Label dobLabel;

    // Contact Info
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label zipLabel;

    // Insurance Info
    @FXML private Label insuranceCompanyLabel;
    @FXML private Label insuranceNumberLabel;
    @FXML private Label preferredPharmacyLabel;
    @FXML private Label preferredPharmacyAddressLabel;
    @FXML private Label preferredPharmacyPhoneLabel;

    // Medical Info
    @FXML private Label bloodTypeLabel;
    @FXML private Label vaccinationStatusLabel;
    @FXML private Label heightLabel;
    @FXML private Label weightLabel;

    @FXML private TextArea allergiesArea;
    @FXML private TextArea medicationsArea;
    @FXML private TextArea chronicConditionsArea;
    @FXML private TextArea medicalHistoryArea;

    // Emergency Contact
    @FXML private Label emergencyContactNameLabel;
    @FXML private Label emergencyContactRelationshipLabel;
    @FXML private Label emergencyContactPhoneLabel;

    // Hospital Referral
    @FXML private ComboBox<String> hospitalComboBox;
    @FXML private ComboBox<String> departmentComboBox;
    @FXML private DatePicker visitDatePicker;
    @FXML private ComboBox<String> visitTimeComboBox;
    @FXML private TextArea referralNotesArea;

    private FirebaseService firebaseService;
    private UserContext userContext;

    private final Map<String, HospitalProfile> hospitalMap = new HashMap<>();

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        setReadOnly(allergiesArea);
        setReadOnly(medicationsArea);
        setReadOnly(chronicConditionsArea);
        setReadOnly(medicalHistoryArea);

        setupReferralSection();
        loadPatientHistory();
        loadHospitals();
    }

    private void setupReferralSection() {
        departmentComboBox.getItems().setAll(
                "Emergency",
                "Cardiology",
                "Dermatology",
                "Pediatrics",
                "Orthopedics",
                "Radiology",
                "Neurology",
                "Laboratory",
                "General Medicine",
                "Surgery"
        );

        visitTimeComboBox.getItems().setAll(
                "8:00 AM",
                "8:30 AM",
                "9:00 AM",
                "9:30 AM",
                "10:00 AM",
                "10:30 AM",
                "11:00 AM",
                "11:30 AM",
                "12:00 PM",
                "12:30 PM",
                "1:00 PM",
                "1:30 PM",
                "2:00 PM",
                "2:30 PM",
                "3:00 PM",
                "3:30 PM",
                "4:00 PM",
                "4:30 PM",
                "5:00 PM"
        );

        visitDatePicker.setValue(LocalDate.now());
    }

    private void loadHospitals() {
        statusLabel.setText("Loading hospitals...");

        firebaseService.getAllHospitals()
                .thenAccept(hospitals -> Platform.runLater(() -> {
                    hospitalMap.clear();
                    hospitalComboBox.getItems().clear();

                    for (HospitalProfile hospital : hospitals) {
                        if (hospital == null || hospital.getHospitalName() == null) {
                            continue;
                        }

                        String displayName = hospital.getHospitalName();

                        hospitalMap.put(displayName, hospital);
                        hospitalComboBox.getItems().add(displayName);
                    }

                    statusLabel.setText("Patient information loaded.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            statusLabel.setText("Failed to load hospitals.")
                    );
                    ex.printStackTrace();
                    return null;
                });
    }

    private void loadPatientHistory() {
        if (!userContext.isLoggedIn() || (!userContext.isDoctor() && !userContext.isHospital())) {
            statusLabel.setText("Access denied.");
            return;
        }

        String selectedPatientUid = userContext.getSelectedPatientUid();
        PatientProfile selectedPatientProfile = userContext.getSelectedPatientProfile();

        if (selectedPatientProfile != null) {
            populateFields(selectedPatientProfile);
            statusLabel.setText("Patient information loaded.");
            return;
        }

        if (selectedPatientUid == null || selectedPatientUid.isBlank()) {
            statusLabel.setText("No patient selected.");
            return;
        }

        statusLabel.setText("Loading patient information...");

        firebaseService.getPatientProfile(selectedPatientUid)
                .thenAccept(profile -> Platform.runLater(() -> {
                    userContext.setSelectedPatientProfile(profile);
                    populateFields(profile);
                    statusLabel.setText("Patient information loaded.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            statusLabel.setText("Failed to load patient information.")
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

        String ageText = profile.getAge() == null ? "Not provided" : String.valueOf(profile.getAge());
        String genderText = valueOrDefault(profile.getGender());
        String dobText = valueOrDefault(profile.getDateOfBirth());

        patientNameLabel.setText(valueOrDefault(profile.getName()));

        overviewAgeLabel.setText(ageText);
        overviewGenderLabel.setText(genderText);
        overviewDobLabel.setText(dobText);

        fullNameLabel.setText(valueOrDefault(profile.getName()));
        ageLabel.setText(ageText);
        genderLabel.setText(genderText);
        dobLabel.setText(dobText);

        emailLabel.setText(valueOrDefault(profile.getEmail()));
        phoneLabel.setText(valueOrDefault(profile.getPhoneNumber()));
        zipLabel.setText(valueOrDefault(profile.getZip()));

        insuranceCompanyLabel.setText(valueOrDefault(profile.getInsuranceCompany()));
        insuranceNumberLabel.setText(valueOrDefault(profile.getInsuranceNumber()));
        preferredPharmacyLabel.setText(valueOrDefault(profile.getPreferredPharmacyName()));
        preferredPharmacyAddressLabel.setText(valueOrDefault(profile.getPreferredPharmacyAddress()));
        preferredPharmacyPhoneLabel.setText(valueOrDefault(profile.getPreferredPharmacyPhoneNumber()));

        bloodTypeLabel.setText(valueOrDefault(profile.getBloodType()));
        vaccinationStatusLabel.setText(valueOrDefault(profile.getVaccinationStatus()));
        heightLabel.setText(valueOrDefault(profile.getHeight()));
        weightLabel.setText(profile.getWeight() == null ? "Not provided" : profile.getWeight().toString());

        allergiesArea.setText(valueOrDefault(profile.getAllergies()));
        medicationsArea.setText(valueOrDefault(profile.getCurrentMedications()));
        chronicConditionsArea.setText(valueOrDefault(profile.getChronicConditions()));
        medicalHistoryArea.setText(valueOrDefault(profile.getMedicalHistory()));

        emergencyContactNameLabel.setText(valueOrDefault(profile.getEmergencyContactName()));
        emergencyContactRelationshipLabel.setText(valueOrDefault(profile.getEmergencyContactRelationship()));
        emergencyContactPhoneLabel.setText(valueOrDefault(profile.getEmergencyContactPhone()));
    }

    @FXML
    private void handleAuthorizeHospitalReferral() {
        if (!userContext.isDoctor()) {
            statusLabel.setText("Only doctors can authorize hospital referrals.");
            return;
        }

        PatientProfile patient = userContext.getSelectedPatientProfile();
        DoctorProfile doctor = userContext.getDoctorProfile();

        if (patient == null) {
            statusLabel.setText("No patient selected.");
            return;
        }

        if (doctor == null) {
            statusLabel.setText("Doctor profile not found.");
            return;
        }

        String selectedHospitalName = hospitalComboBox.getValue();
        String selectedDepartment = departmentComboBox.getValue();
        LocalDate selectedDate = visitDatePicker.getValue();
        String selectedTime = visitTimeComboBox.getValue();

        if (selectedHospitalName == null || selectedHospitalName.isBlank()) {
            statusLabel.setText("Please select a hospital.");
            return;
        }

        if (selectedDepartment == null || selectedDepartment.isBlank()) {
            statusLabel.setText("Please select a department.");
            return;
        }

        if (selectedDate == null) {
            statusLabel.setText("Please select a visit date.");
            return;
        }

        if (selectedTime == null || selectedTime.isBlank()) {
            statusLabel.setText("Please select a visit time.");
            return;
        }

        HospitalProfile selectedHospital = hospitalMap.get(selectedHospitalName);

        if (selectedHospital == null) {
            statusLabel.setText("Selected hospital could not be found.");
            return;
        }

        try {
            LocalTime time = LocalTime.parse(selectedTime, TIME_FORMAT);
            LocalDateTime dateTime = LocalDateTime.of(selectedDate, time);
            long epochMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            Appointment appointment = new Appointment();

            appointment.setPatientUid(patient.getUid());
            appointment.setPatientName(patient.getName());

            appointment.setDoctorUid(doctor.getUid());
            appointment.setDoctorName(doctor.getName());

            appointment.setHospitalUid(selectedHospital.getUid());
            appointment.setHospitalName(selectedHospital.getHospitalName());

            appointment.setDepartmentName(selectedDepartment);
            appointment.setHospitalDepartment(selectedDepartment);

            appointment.setAppointmentDateTime(epochMillis);
            appointment.setAppointmentDate(selectedDate.toString());
            appointment.setAppointmentSlot(selectedTime);
            appointment.setAppointmentTime(selectedDate + " " + selectedTime);

            appointment.setStatus("SCHEDULED");
            appointment.setNewPatient(false);

            appointment.setReferralType("HOSPITAL_VISIT");
            appointment.setReferralAuthorizedByDoctorUid(doctor.getUid());
            appointment.setReferralAuthorizedByDoctorName(doctor.getName());
            appointment.setReferralNotes(referralNotesArea.getText());

            appointment.setReason("Hospital referral to " + selectedDepartment);
            appointment.setCreatedAt(System.currentTimeMillis());

            statusLabel.setText("Authorizing hospital referral...");

            firebaseService.createHospitalReferralAppointment(appointment)
                    .thenAccept(appointmentId -> Platform.runLater(() -> {
                        statusLabel.setText("Hospital referral authorized successfully.");

                        hospitalComboBox.getSelectionModel().clearSelection();
                        departmentComboBox.getSelectionModel().clearSelection();
                        visitTimeComboBox.getSelectionModel().clearSelection();
                        referralNotesArea.clear();
                        visitDatePicker.setValue(LocalDate.now());
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() ->
                                statusLabel.setText("Failed to authorize hospital referral.")
                        );
                        ex.printStackTrace();
                        return null;
                    });

        } catch (Exception e) {
            statusLabel.setText("Invalid visit date or time.");
            e.printStackTrace();
        }
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
        if (userContext.isHospital()) {
            SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients");
            return;
        }
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }
}