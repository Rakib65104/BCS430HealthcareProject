package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HospitalScheduleController {

    @FXML private DatePicker datePicker;
    @FXML private Label scheduleHeaderLabel;
    @FXML private VBox scheduleListVBox;

    private final FirebaseService firebaseService = new FirebaseService();
    private final UserContext userContext = UserContext.getInstance();

    private static final DateTimeFormatter DATE_HEADER_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
        loadAppointmentsForDate(LocalDate.now());
    }

    @FXML
    private void onLoadForDate() {
        LocalDate selectedDate = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
        loadAppointmentsForDate(selectedDate);
    }

    @FXML
    private void onToday() {
        datePicker.setValue(LocalDate.now());
        loadAppointmentsForDate(LocalDate.now());
    }

    private void loadAppointmentsForDate(LocalDate date) {
        HospitalProfile hospital = userContext.getHospitalProfile();

        if (hospital == null) {
            showEmpty("No hospital is currently loaded.");
            return;
        }

        firebaseService.getAppointmentsForHospital(hospital.getUid())
                .thenAccept(appointments -> Platform.runLater(() -> {
                    List<Appointment> filtered = new ArrayList<>();
                    if (appointments != null) {
                        for (Appointment appointment : appointments) {
                            if (appointment != null && isSameDate(appointment, date) && !isCancelled(appointment)) {
                                filtered.add(appointment);
                            }
                        }
                    }

                    scheduleHeaderLabel.setText("Appointments • " + date.format(DATE_HEADER_FORMAT));
                    renderAppointments(filtered);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showEmpty("Unable to load appointments."));
                    return null;
                });
    }

    private void renderAppointments(List<Appointment> appointments) {
        scheduleListVBox.getChildren().clear();

        if (appointments == null || appointments.isEmpty()) {
            showEmpty("No appointments found for this date.");
            return;
        }

        for (Appointment appointment : appointments) {
            scheduleListVBox.getChildren().add(buildAppointmentCard(appointment));
        }
    }

    private VBox buildAppointmentCard(Appointment appointment) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #D1FAE5;" +
                        "-fx-border-radius: 14;"
        );

        Label patientLabel = new Label(valueOrDefault(appointment.getPatientName(), "Unknown Patient"));
        patientLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 16; -fx-font-weight: bold;");

        Label timeLabel = new Label("Time: " + formatTime(appointment));
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label doctorLabel = new Label("Doctor: " + valueOrDefault(appointment.getDoctorName(), "Not assigned"));
        doctorLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label reasonLabel = new Label("Reason: " + valueOrDefault(appointment.getReferralType(), valueOrDefault(appointment.getReason(), "General visit")));
        reasonLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
        reasonLabel.setWrapText(true);

        Label referralLabel = new Label("Referral notes: " + valueOrDefault(appointment.getReferralNotes(), valueOrDefault(appointment.getNotes(), "No referral notes shared")));
        referralLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
        referralLabel.setWrapText(true);

        Label statusLabel = new Label("Status: " + valueOrDefault(appointment.getStatus(), "SCHEDULED"));
        statusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 12; -fx-font-weight: bold;");

        HBox actionRow = new HBox(8);

        Button historyButton = new Button("Patient History");
        historyButton.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #134E4A; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8;");
        historyButton.setOnAction(event -> openPatientHistory(appointment));

        Button uploadButton = new Button("Upload Results");
        uploadButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8;");
        uploadButton.setOnAction(event -> openUploadResultsDialog(appointment));

        actionRow.getChildren().addAll(historyButton, uploadButton);

        card.getChildren().addAll(patientLabel, timeLabel, doctorLabel, reasonLabel, referralLabel, statusLabel, actionRow);
        return card;
    }

    private void showEmpty(String text) {
        scheduleListVBox.getChildren().clear();

        VBox card = new VBox();
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-radius: 12;"
        );

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
        card.getChildren().add(label);

        scheduleListVBox.getChildren().add(card);
    }

    private boolean isSameDate(Appointment appointment, LocalDate targetDate) {
        try {
            LocalDate appointmentDate = Instant.ofEpochMilli(appointment.getAppointmentDateTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return appointmentDate.equals(targetDate);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCancelled(Appointment appointment) {
        return appointment.getStatus() != null && appointment.getStatus().equalsIgnoreCase("CANCELLED");
    }

    private String formatTime(Appointment appointment) {
        try {
            return Instant.ofEpochMilli(appointment.getAppointmentDateTime())
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
        } catch (Exception e) {
            return valueOrDefault(appointment.getAppointmentTime(), "Unknown");
        }
    }

    @FXML private void onDashboard() { SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard"); }
    @FXML private void onPatients() { SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients"); }
    @FXML private void onSchedule() {}
    @FXML private void onProfile() { SceneRouter.go("hospital-profile-view.fxml", "Hospital Profile"); }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void openPatientHistory(Appointment appointment) {
        if (appointment == null || appointment.getPatientUid() == null || appointment.getPatientUid().isBlank()) {
            return;
        }

        firebaseService.getPatientProfile(appointment.getPatientUid())
                .thenAccept(profile -> Platform.runLater(() -> {
                    userContext.setSelectedPatientUid(appointment.getPatientUid());
                    userContext.setSelectedPatientProfile(profile);
                    SceneRouter.go("doctor-patient-history-view.fxml", "Patient Medical History");
                }))
                .exceptionally(e -> null);
    }

    private void openUploadResultsDialog(Appointment appointment) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Upload Diagnostic Results");
        dialog.setHeaderText("Update results for " + valueOrDefault(appointment.getPatientName(), "this patient"));

        ButtonType saveType = new ButtonType("Publish Results", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextArea findingsArea = new TextArea();
        findingsArea.setPromptText("Summarize what happened during the hospital visit.");
        findingsArea.setWrapText(true);
        findingsArea.setPrefRowCount(4);
        findingsArea.setText(valueOrDefault(appointment.getHospitalFindings(), ""));

        TextArea resultsArea = new TextArea();
        resultsArea.setPromptText("Enter diagnostic results, imaging impressions, or lab findings.");
        resultsArea.setWrapText(true);
        resultsArea.setPrefRowCount(6);
        resultsArea.setText(valueOrDefault(appointment.getDiagnosticResults(), ""));

        VBox content = new VBox(10,
                new Label("Hospital findings"),
                findingsArea,
                new Label("Diagnostic results"),
                resultsArea
        );
        content.setPrefWidth(430);
        dialog.getDialogPane().setContent(content);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != saveType) {
            return;
        }

        String findings = findingsArea.getText() == null ? "" : findingsArea.getText().trim();
        String results = resultsArea.getText() == null ? "" : resultsArea.getText().trim();

        if (findings.isBlank() && results.isBlank()) {
            return;
        }

        appointment.setHospitalFindings(findings);
        appointment.setDiagnosticResults(results);
        appointment.setDiagnosticResultsUploadedAt(System.currentTimeMillis());
        appointment.setVisitSummary(findings.isBlank() ? results : findings);
        appointment.setStatus("COMPLETED");
        appointment.setCompletedAt(System.currentTimeMillis());

        firebaseService.publishHospitalDiagnosticResults(appointment)
                .thenRun(() -> Platform.runLater(() -> loadAppointmentsForDate(
                        datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue()
                )))
                .exceptionally(e -> {
                    Platform.runLater(() -> showEmpty("Unable to publish diagnostic results."));
                    return null;
                });
    }
}
