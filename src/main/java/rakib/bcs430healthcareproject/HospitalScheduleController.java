package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class HospitalScheduleController implements Initializable {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @FXML private TableView<Schedule> appointmentTable;
    @FXML private TableColumn<Schedule, String> colTime;
    @FXML private TableColumn<Schedule, String> colPatient;
    @FXML private TableColumn<Schedule, String> colType;
    @FXML private TableColumn<Schedule, String> colStatus;
    @FXML private TableColumn<Schedule, String> colNotes;

    @FXML private Label scheduleSummaryLabel;
    @FXML private DatePicker scheduleDatePicker;
    @FXML private Button btnReschedule;
    @FXML private Button btnPrescription;
    @FXML private Button btnEditPatientProfile;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();

    private FirebaseService firebaseService;
    private UserContext userContext;
    private List<Appointment> allAppointments = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isHospital()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        setupTable();
        scheduleDatePicker.setValue(LocalDate.now());
        loadAppointments();
    }

    private void setupTable() {
        colTime.setCellValueFactory(cell -> cell.getValue().timeProperty());
        colPatient.setCellValueFactory(cell -> cell.getValue().patientNameProperty());
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colNotes.setCellValueFactory(cell -> cell.getValue().notesProperty());
        appointmentTable.setItems(scheduleList);
    }

    private void loadAppointments() {
        HospitalProfile hospital = userContext.getHospitalProfile();
        if (hospital == null || hospital.getUid() == null || hospital.getUid().isBlank()) {
            showAlert("Hospital Error", "Hospital account details could not be loaded.");
            return;
        }

        firebaseService.getAppointmentsForHospital(hospital.getUid())
                .thenAccept(appointments -> Platform.runLater(() -> {
                    allAppointments = appointments == null ? new ArrayList<>() : new ArrayList<>(appointments);
                    allAppointments.sort(Comparator.comparing(
                            Appointment::resolveAppointmentEpochMillis,
                            Comparator.nullsLast(Long::compareTo)
                    ));
                    refreshTableForDate();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        scheduleList.clear();
                        scheduleSummaryLabel.setText("Unable to load schedule.");
                        showAlert("Schedule Error", cleanErrorMessage(e));
                    });
                    return null;
                });
    }

    @FXML
    private void handleDateChange() {
        refreshTableForDate();
    }

    @FXML
    private void onToday() {
        scheduleDatePicker.setValue(LocalDate.now());
        refreshTableForDate();
    }

    private void refreshTableForDate() {
        LocalDate selectedDate = scheduleDatePicker.getValue();
        scheduleList.clear();

        if (selectedDate == null) {
            scheduleSummaryLabel.setText("Choose a date to view appointments.");
            return;
        }

        for (Appointment appointment : allAppointments) {
            if (appointment == null || "CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
                continue;
            }

            LocalDate appointmentDate = resolveAppointmentDate(appointment);
            if (appointmentDate == null || !selectedDate.equals(appointmentDate)) {
                continue;
            }

            Schedule schedule = new Schedule(
                    formatAppointmentTime(appointment),
                    valueOrDefault(appointment.getPatientName(), "Unknown Patient"),
                    valueOrDefault(appointment.getReferralType(),
                            valueOrDefault(appointment.getHospitalDepartment(),
                                    valueOrDefault(appointment.getReason(), "Hospital visit"))),
                    valueOrDefault(appointment.getStatus(), "SCHEDULED"),
                    buildScheduleNotes(appointment)
            );
            schedule.setSourceAppointment(appointment);
            scheduleList.add(schedule);
        }

        scheduleList.sort(Comparator.comparing(
                schedule -> parseTime(schedule.getTime()),
                Comparator.nullsLast(LocalTime::compareTo)
        ));

        if (scheduleList.isEmpty()) {
            scheduleSummaryLabel.setText("No appointments scheduled for " + selectedDate + ".");
        } else {
            scheduleSummaryLabel.setText("Showing " + scheduleList.size() + " hospital appointment(s) for " + selectedDate + ".");
        }
    }

    @FXML
    private void handleReschedule() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null || selectedSchedule.getSourceAppointment() == null) {
            showAlert("No Selection", "Please select an appointment to reschedule.");
            return;
        }

        Appointment appointment = selectedSchedule.getSourceAppointment();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reschedule Appointment");
        dialog.setHeaderText("Choose a new time for " + valueOrDefault(appointment.getPatientName(), "this patient"));

        ButtonType saveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        DatePicker dialogDatePicker = new DatePicker(resolveAppointmentDate(appointment));
        ComboBox<String> timeComboBox = new ComboBox<>();
        timeComboBox.getItems().addAll(
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM",
                "11:30 AM", "01:00 PM", "01:30 PM", "02:00 PM", "02:30 PM",
                "03:00 PM", "03:30 PM", "04:00 PM", "04:30 PM"
        );
        timeComboBox.setEditable(true);
        timeComboBox.setValue(formatAppointmentTime(appointment));

        TextArea notesArea = new TextArea(valueOrDefault(appointment.getNotes(), ""));
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(4);
        notesArea.setPromptText("Optional scheduling notes");

        VBox content = new VBox(10,
                new Label("Appointment date"),
                dialogDatePicker,
                new Label("Time slot"),
                timeComboBox,
                new Label("Notes"),
                notesArea
        );
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != saveType) {
            return;
        }

        LocalDate selectedDate = dialogDatePicker.getValue();
        String selectedTime = timeComboBox.getValue() == null ? "" : timeComboBox.getValue().trim();

        if (selectedDate == null || selectedTime.isBlank()) {
            showAlert("Validation Error", "Please choose both a date and time for the reschedule.");
            return;
        }

        long appointmentEpoch = resolveEpochMillis(selectedDate, selectedTime);
        appointment.setAppointmentDate(selectedDate.toString());
        appointment.setAppointmentSlot(selectedTime);
        appointment.setAppointmentTime(selectedDate + " " + selectedTime);
        appointment.setAppointmentDateTime(appointmentEpoch);
        appointment.setNotes(notesArea.getText() == null ? "" : notesArea.getText().trim());
        appointment.setStatus("SCHEDULED");

        firebaseService.updateAppointment(appointment)
                .thenAccept(v -> Platform.runLater(() -> {
                    firebaseService.notifyPatient(
                            appointment.getPatientUid(),
                            "Hospital Appointment Updated",
                            valueOrDefault(appointment.getHospitalName(), "Your hospital")
                                    + " rescheduled your appointment to "
                                    + appointment.getAppointmentDate() + " at " + appointment.getAppointmentSlot(),
                            "APPOINTMENT",
                            appointment.getAppointmentId()
                    );

                    if (appointment.getDoctorUid() != null && !appointment.getDoctorUid().isBlank()) {
                        firebaseService.notifyDoctor(
                                appointment.getDoctorUid(),
                                "Hospital Appointment Updated",
                                valueOrDefault(appointment.getHospitalName(), "Hospital")
                                        + " rescheduled " + valueOrDefault(appointment.getPatientName(), "a patient")
                                        + " to " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentSlot(),
                                "APPOINTMENT",
                                appointment.getAppointmentId()
                        );
                    }

                    loadAppointments();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Update Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    @FXML
    private void handleSendPrescription() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null || selectedSchedule.getSourceAppointment() == null) {
            showAlert("No Patient Selected", "Please select a patient from the schedule to send a prescription.");
            return;
        }

        openPatientContext(
                selectedSchedule.getSourceAppointment().getPatientUid(),
                "hospital-prescription-view.fxml",
                "Send Prescription"
        );
    }

    @FXML
    private void handleEditPatientProfile() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null || selectedSchedule.getSourceAppointment() == null) {
            showAlert("No Patient Selected", "Please select an appointment to open the patient profile.");
            return;
        }

        openPatientContext(
                selectedSchedule.getSourceAppointment().getPatientUid(),
                "patient-profile-view.fxml",
                "Patient Profile"
        );
    }

    @FXML
    private void handleCancelSelected() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null || selectedSchedule.getSourceAppointment() == null) {
            showAlert("No Selection", "Please select an appointment to cancel.");
            return;
        }

        Appointment appointment = selectedSchedule.getSourceAppointment();

        firebaseService.deleteAppointment(appointment.getAppointmentId())
                .thenAccept(v -> Platform.runLater(() -> {
                    firebaseService.notifyPatient(
                            appointment.getPatientUid(),
                            "Hospital Appointment Cancelled",
                            valueOrDefault(appointment.getHospitalName(), "Your hospital")
                                    + " cancelled your appointment on " + appointment.getAppointmentDate()
                                    + " at " + appointment.getAppointmentSlot() + ".",
                            "APPOINTMENT",
                            appointment.getAppointmentId()
                    );
                    loadAppointments();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Cancel Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    @FXML
    private void handleMarkComplete() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null || selectedSchedule.getSourceAppointment() == null) {
            showAlert("No Selection", "Please select an appointment to mark complete.");
            return;
        }

        Appointment appointment = selectedSchedule.getSourceAppointment();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Complete Appointment");
        dialog.setHeaderText("Add hospital findings for " + valueOrDefault(appointment.getPatientName(), "this patient"));

        ButtonType completeType = new ButtonType("Save Results", ButtonBar.ButtonData.OK_DONE);
        ButtonType completeAndEditType = new ButtonType("Save and Edit Profile", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(completeType, completeAndEditType, ButtonType.CANCEL);

        TextArea findingsArea = new TextArea(valueOrDefault(appointment.getHospitalFindings(), ""));
        findingsArea.setPromptText("Summarize the visit or treatment.");
        findingsArea.setWrapText(true);
        findingsArea.setPrefRowCount(4);

        TextArea resultsArea = new TextArea(valueOrDefault(appointment.getDiagnosticResults(), ""));
        resultsArea.setPromptText("Add diagnostic results, imaging findings, or lab notes.");
        resultsArea.setWrapText(true);
        resultsArea.setPrefRowCount(6);

        VBox content = new VBox(10,
                new Label("Hospital findings"),
                findingsArea,
                new Label("Diagnostic results"),
                resultsArea
        );
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.CANCEL) {
            return;
        }

        String findings = findingsArea.getText() == null ? "" : findingsArea.getText().trim();
        String diagnosticResults = resultsArea.getText() == null ? "" : resultsArea.getText().trim();

        if (findings.isBlank() && diagnosticResults.isBlank()) {
            showAlert("Validation Error", "Add hospital findings or diagnostic results before completing the appointment.");
            return;
        }

        appointment.setHospitalFindings(findings);
        appointment.setDiagnosticResults(diagnosticResults);
        appointment.setDiagnosticResultsUploadedAt(System.currentTimeMillis());
        appointment.setVisitSummary(findings.isBlank() ? diagnosticResults : findings);
        appointment.setStatus("COMPLETED");
        appointment.setCompletedAt(System.currentTimeMillis());

        firebaseService.publishHospitalDiagnosticResults(appointment)
                .thenRun(() -> Platform.runLater(() -> {
                    if (result == completeAndEditType) {
                        openPatientContext(appointment.getPatientUid(), "patient-profile-view.fxml", "Patient Profile");
                    } else {
                        loadAppointments();
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Update Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    private void openPatientContext(String patientUid, String destinationFxml, String title) {
        firebaseService.getPatientProfile(patientUid)
                .thenAccept(profile -> Platform.runLater(() -> {
                    if (profile == null) {
                        showAlert("Patient Error", "The selected patient could not be loaded.");
                        return;
                    }
                    userContext.setSelectedPatientProfile(profile);
                    userContext.setSelectedPatientUid(profile.getUid());
                    SceneRouter.go(destinationFxml, title);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Patient Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    private LocalDate resolveAppointmentDate(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        if (appointment.getAppointmentDate() != null && !appointment.getAppointmentDate().isBlank()) {
            try {
                return LocalDate.parse(appointment.getAppointmentDate());
            } catch (Exception ignored) {
                // Fall back to epoch parsing below.
            }
        }

        Long epoch = appointment.resolveAppointmentEpochMillis();
        if (epoch == null) {
            return null;
        }

        return Instant.ofEpochMilli(epoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private long resolveEpochMillis(LocalDate date, String time) {
        LocalTime localTime = parseTime(time);
        if (localTime == null) {
            throw new IllegalArgumentException("Invalid appointment time: " + time);
        }
        LocalDateTime localDateTime = LocalDateTime.of(date, localTime);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(time.trim().toUpperCase(Locale.ENGLISH), TIME_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatAppointmentTime(Appointment appointment) {
        if (appointment.getAppointmentSlot() != null && !appointment.getAppointmentSlot().isBlank()) {
            return appointment.getAppointmentSlot();
        }

        Long epoch = appointment.resolveAppointmentEpochMillis();
        if (epoch == null) {
            return "";
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault())
                .format(TIME_FORMAT);
    }

    private String buildScheduleNotes(Appointment appointment) {
        if ("COMPLETED".equalsIgnoreCase(appointment.getStatus())) {
            String summary = valueOrDefault(appointment.getHospitalFindings(), appointment.getVisitSummary());
            if (!summary.isBlank()) {
                return summary;
            }
        }

        if (appointment.getReferralNotes() != null && !appointment.getReferralNotes().isBlank()) {
            return appointment.getReferralNotes();
        }

        return valueOrDefault(appointment.getNotes(), "");
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unknown error" : message;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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
}
