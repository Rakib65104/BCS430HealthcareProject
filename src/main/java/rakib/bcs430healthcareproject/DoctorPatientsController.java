package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the doctor-side patient list.
 */
public class DoctorPatientsController {

    private static final DateTimeFormatter REFERRAL_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML private Label doctorNameLabel;
    @FXML private Label statusLabel;
    @FXML private VBox patientsListVBox;
    @FXML private ScrollPane patientsScrollPane;

    private FirebaseService firebaseService;
    private UserContext userContext;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        String doctorName = userContext.getName() != null ? userContext.getName() : "Doctor";
        doctorNameLabel.setText("Patients for Dr. " + doctorName);

        loadPatients();
    }

    private void loadPatients() {
        showStatus("Loading patient list...", false);
        firebaseService.getDoctorPatients(userContext.getUid())
                .thenAccept(patients -> Platform.runLater(() -> renderPatients(patients)))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Failed to load patients: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void renderPatients(List<PatientProfile> patients) {
        patientsListVBox.getChildren().clear();

        if (patients == null || patients.isEmpty()) {
            showStatus("No patients are linked to this doctor yet. Once appointments are booked, they will appear here.", false);
            return;
        }

        showStatus("Found " + patients.size() + " patient(s).", false);

        for (PatientProfile patient : patients) {
            patientsListVBox.getChildren().add(createPatientCard(patient));
        }
    }

    private VBox createPatientCard(PatientProfile patient) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #D1FAE5; -fx-border-radius: 12;");

        Label nameLabel = new Label(patient.getName() != null ? patient.getName() : "Unnamed Patient");
        nameLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 17; -fx-font-weight: bold;");

        Label emailLabel = new Label("Email: " + fallback(patient.getEmail()));
        emailLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label phoneLabel = new Label("Phone: " + fallback(patient.getPhoneNumber()));
        phoneLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label medsLabel = new Label("Current Medications: " + fallback(patient.getCurrentMedications()));
        medsLabel.setWrapText(true);
        medsLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        HBox buttonRow = new HBox(10);

        Button viewProfileButton = new Button("View Profile");
        viewProfileButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        viewProfileButton.setOnAction(event -> onViewProfile(patient));

        Button sendPrescriptionButton = new Button("Send Prescription");
        sendPrescriptionButton.setStyle("-fx-background-color: #14B8A6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        sendPrescriptionButton.setOnAction(event -> onSendPrescription(patient));

        Button sendTextButton = new Button("Send Text");
        sendTextButton.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        sendTextButton.setOnAction(event -> onSendText(patient));

        Button referButton = new Button("Refer Hospital");
        referButton.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        referButton.setOnAction(event -> onReferHospital(patient));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonRow.getChildren().addAll(
                viewProfileButton,
                sendPrescriptionButton,
                sendTextButton,
                referButton,
                spacer
        );

        card.getChildren().addAll(nameLabel, emailLabel, phoneLabel, medsLabel, buttonRow);
        return card;
    }

    private void onViewProfile(PatientProfile patient) {
        userContext.setSelectedPatientUid(patient.getUid());
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("patient-profile-view.fxml", "Patient Profile");
    }

    private void onSendPrescription(PatientProfile patient) {
        userContext.setSelectedPatientUid(patient.getUid());
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-prescription-view.fxml", "Send Prescription");
    }

    private void onSendText(PatientProfile patient) {
        userContext.setSelectedPatientUid(patient.getUid());
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-message-view.fxml", "Send Message");
    }

    private void onReferHospital(PatientProfile patient) {
        firebaseService.getAllHospitals()
                .thenAccept(hospitals -> Platform.runLater(() -> openReferralDialog(patient, hospitals)))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load hospitals: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void openReferralDialog(PatientProfile patient, List<HospitalProfile> hospitals) {
        if (patient == null) {
            showStatus("Patient is required for hospital referral.", true);
            return;
        }
        if (hospitals == null || hospitals.isEmpty()) {
            showStatus("No hospital accounts are available for referral yet.", true);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Refer to Hospital");
        dialog.setHeaderText("Create a hospital referral for " + fallback(patient.getName()));

        ButtonType createType = new ButtonType("Create Referral", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        ComboBox<HospitalProfile> hospitalComboBox = new ComboBox<>();
        hospitalComboBox.getItems().addAll(hospitals);
        hospitalComboBox.setMaxWidth(Double.MAX_VALUE);
        hospitalComboBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(HospitalProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : hospitalLabel(item));
            }
        });
        hospitalComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(HospitalProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : hospitalLabel(item));
            }
        });
        hospitalComboBox.getSelectionModel().selectFirst();

        ComboBox<String> serviceComboBox = new ComboBox<>();
        serviceComboBox.getItems().addAll("Surgery", "Diagnostic Scan", "Lab Testing", "Specialist Procedure", "Follow-up Evaluation");
        serviceComboBox.setEditable(true);
        serviceComboBox.setValue("Diagnostic Scan");
        serviceComboBox.setMaxWidth(Double.MAX_VALUE);

        DatePicker referralDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<String> timeComboBox = new ComboBox<>();
        timeComboBox.getItems().addAll(
                "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
                "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM"
        );
        timeComboBox.setEditable(true);
        timeComboBox.setValue("09:00 AM");
        timeComboBox.setMaxWidth(Double.MAX_VALUE);

        TextArea referralNotesArea = new TextArea();
        referralNotesArea.setPromptText("Why is the patient being referred? Include context the hospital should know.");
        referralNotesArea.setWrapText(true);
        referralNotesArea.setPrefRowCount(5);

        VBox content = new VBox(10,
                new Label("Hospital"),
                hospitalComboBox,
                new Label("Service / referral type"),
                serviceComboBox,
                new Label("Appointment date"),
                referralDatePicker,
                new Label("Appointment time"),
                timeComboBox,
                new Label("Referral notes"),
                referralNotesArea
        );
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (result != createType) {
            return;
        }

        HospitalProfile selectedHospital = hospitalComboBox.getValue();
        String serviceType = serviceComboBox.getEditor() != null && !serviceComboBox.getEditor().getText().isBlank()
                ? serviceComboBox.getEditor().getText().trim()
                : serviceComboBox.getValue();
        LocalDate date = referralDatePicker.getValue();
        String time = timeComboBox.getEditor() != null && !timeComboBox.getEditor().getText().isBlank()
                ? timeComboBox.getEditor().getText().trim()
                : timeComboBox.getValue();
        String referralNotes = referralNotesArea.getText() == null ? "" : referralNotesArea.getText().trim();

        if (selectedHospital == null) {
            showStatus("Please choose a hospital for the referral.", true);
            return;
        }
        if (serviceType == null || serviceType.isBlank()) {
            showStatus("Please enter the service the hospital should provide.", true);
            return;
        }
        if (date == null) {
            showStatus("Please choose a referral date.", true);
            return;
        }
        if (time == null || time.isBlank()) {
            showStatus("Please choose a referral time.", true);
            return;
        }

        createHospitalReferral(patient, selectedHospital, serviceType, date, time, referralNotes);
    }

    private void createHospitalReferral(PatientProfile patient,
                                        HospitalProfile hospital,
                                        String serviceType,
                                        LocalDate date,
                                        String time,
                                        String referralNotes) {
        LocalTime localTime;
        try {
            localTime = LocalTime.parse(time.trim().toUpperCase(Locale.ENGLISH), REFERRAL_TIME_FORMAT);
        } catch (Exception ex) {
            showStatus("Referral time must look like 9:00 AM.", true);
            return;
        }

        long appointmentEpoch = LocalDateTime.of(date, localTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        Appointment appointment = new Appointment(
                patient.getUid(),
                userContext.getUid(),
                patient.getName(),
                userContext.getName(),
                appointmentEpoch
        );
        appointment.setAppointmentDate(date.toString());
        appointment.setAppointmentSlot(time.trim().toUpperCase(Locale.ENGLISH));
        appointment.setAppointmentTime(date + " " + appointment.getAppointmentSlot());
        appointment.setStatus("SCHEDULED");
        appointment.setReason(serviceType);
        appointment.setReferralType(serviceType);
        appointment.setNotes(referralNotes);
        appointment.setReferralNotes(referralNotes);
        appointment.setHospitalUid(hospital.getUid());
        appointment.setHospitalName(hospital.getHospitalName());
        appointment.setHospitalDepartment(serviceType);
        appointment.setNewPatient(false);

        firebaseService.createHospitalReferralAppointment(appointment)
                .thenAccept(appointmentId -> Platform.runLater(() -> {
                    firebaseService.notifyPatient(
                            patient.getUid(),
                            "Hospital Referral Scheduled",
                            "Dr. " + userContext.getName() + " referred you to "
                                    + fallback(hospital.getHospitalName()) + " for " + serviceType
                                    + " on " + date + " at " + appointment.getAppointmentSlot() + ".",
                            "HOSPITAL_REFERRAL",
                            appointmentId
                    );
                    showStatus("Hospital referral scheduled successfully.", false);
                    loadPatients();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to create referral: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }

    private String hospitalLabel(HospitalProfile hospital) {
        return fallback(hospital.getHospitalName())
                + " • "
                + fallback(hospital.getCity())
                + ", "
                + fallback(hospital.getState());
    }
}
