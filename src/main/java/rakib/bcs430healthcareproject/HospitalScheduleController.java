package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class HospitalScheduleController implements Initializable {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static final DateTimeFormatter WEEK_TIME_FORMAT =
            DateTimeFormatter.ofPattern("EEE MM/dd hh:mm a", Locale.ENGLISH);

    @FXML private TableView<Schedule> appointmentTable;
    @FXML private TableColumn<Schedule, String> colTime;
    @FXML private TableColumn<Schedule, String> colPatient;
    @FXML private TableColumn<Schedule, String> colDoctor;
    @FXML private TableColumn<Schedule, String> colDepartment;
    @FXML private TableColumn<Schedule, String> colType;
    @FXML private TableColumn<Schedule, String> colStatus;
    @FXML private TableColumn<Schedule, String> colNotes;

    @FXML private Label scheduleSummaryLabel;
    @FXML private Label scheduleTitleLabel;
    @FXML private DatePicker scheduleDatePicker;
    @FXML private ComboBox<String> comboScheduleView;

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
        setupInputs();
        loadAppointments();
    }

    private void setupTable() {
        colTime.setCellValueFactory(cell -> cell.getValue().timeProperty());
        colPatient.setCellValueFactory(cell -> cell.getValue().patientNameProperty());
        colDoctor.setCellValueFactory(cell -> cell.getValue().doctorNameProperty());
        colDepartment.setCellValueFactory(cell -> cell.getValue().departmentProperty());
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colNotes.setCellValueFactory(cell -> cell.getValue().notesProperty());

        appointmentTable.setItems(scheduleList);
    }

    private void setupInputs() {
        scheduleDatePicker.setValue(LocalDate.now());

        comboScheduleView.getItems().addAll("Day View", "Week View");
        comboScheduleView.setValue("Day View");
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
                    refreshTable();
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
        refreshTable();
    }

    @FXML
    private void handleViewModeChange() {
        refreshTable();
    }

    @FXML
    private void onToday() {
        scheduleDatePicker.setValue(LocalDate.now());
        refreshTable();
    }

    private void refreshTable() {
        LocalDate selectedDate = scheduleDatePicker.getValue();
        scheduleList.clear();

        if (selectedDate == null) {
            scheduleSummaryLabel.setText("Choose a date to view appointments.");
            return;
        }

        boolean weekView = "Week View".equalsIgnoreCase(comboScheduleView.getValue());

        LocalDate weekStart = selectedDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        if (weekView) {
            scheduleTitleLabel.setText("Weekly Hospital Schedule");
        } else {
            scheduleTitleLabel.setText("Daily Hospital Schedule");
        }

        for (Appointment appointment : allAppointments) {
            if (appointment == null || "CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
                continue;
            }

            LocalDate appointmentDate = resolveAppointmentDate(appointment);

            if (appointmentDate == null) {
                continue;
            }

            if (weekView) {
                if (appointmentDate.isBefore(weekStart) || appointmentDate.isAfter(weekEnd)) {
                    continue;
                }
            } else {
                if (!selectedDate.equals(appointmentDate)) {
                    continue;
                }
            }

            Schedule schedule = new Schedule(
                    formatAppointmentTime(appointment, weekView),
                    valueOrDefault(appointment.getPatientName(), "Unknown Patient"),
                    valueOrDefault(appointment.getDoctorName(), "Unknown Doctor"),
                    valueOrDefault(appointment.getHospitalDepartment(), "Unassigned"),
                    valueOrDefault(appointment.getReferralType(),
                            valueOrDefault(appointment.getReason(), "Hospital visit")),
                    valueOrDefault(appointment.getStatus(), "SCHEDULED"),
                    buildScheduleNotes(appointment)
            );

            schedule.setSourceAppointment(appointment);
            scheduleList.add(schedule);
        }

        scheduleList.sort(Comparator.comparing(
                schedule -> {
                    Appointment appointment = schedule.getSourceAppointment();
                    return appointment == null ? null : appointment.resolveAppointmentEpochMillis();
                },
                Comparator.nullsLast(Long::compareTo)
        ));

        if (scheduleList.isEmpty()) {
            if (weekView) {
                scheduleSummaryLabel.setText("No appointments scheduled from " + weekStart + " to " + weekEnd + ".");
            } else {
                scheduleSummaryLabel.setText("No appointments scheduled for " + selectedDate + ".");
            }
        } else {
            if (weekView) {
                scheduleSummaryLabel.setText("Showing " + scheduleList.size() + " appointment(s) from " + weekStart + " to " + weekEnd + ".");
            } else {
                scheduleSummaryLabel.setText("Showing " + scheduleList.size() + " appointment(s) for " + selectedDate + ".");
            }
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

        String serviceType = valueOrDefault(appointment.getReferralType(),
                valueOrDefault(appointment.getHospitalDepartment(),
                        valueOrDefault(appointment.getReason(), "Hospital visit")));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reschedule Appointment");
        dialog.setHeaderText("Choose a new time for " + valueOrDefault(appointment.getPatientName(), "this patient"));

        ButtonType saveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        DatePicker dialogDatePicker = new DatePicker(resolveAppointmentDate(appointment));
        ComboBox<String> timeComboBox = new ComboBox<>();

        populateTimeComboBox(timeComboBox, serviceType);

        timeComboBox.setEditable(true);
        timeComboBox.setValue(formatAppointmentTime(appointment, false));

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

        if (!isTimeSlotValid(serviceType, selectedTime)) {
            showAlert("Invalid Time", serviceType + " appointments are only available between 8:00 AM and 8:00 PM.");
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

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Cancellation");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("Are you sure you want to cancel the appointment with "
                + appointment.getPatientName() + " on " + appointment.getAppointmentDate()
                + " at " + appointment.getAppointmentSlot() + "?");

        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

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

        appointment.setStatus("COMPLETED");
        appointment.setCompletedAt(System.currentTimeMillis());

        firebaseService.updateAppointment(appointment)
                .thenRun(() -> Platform.runLater(() -> {
                    userContext.setSelectedAppointment(appointment);
                    userContext.setSelectedPatientUid(appointment.getPatientUid());
                    SceneRouter.go("hospital-diagnostic.fxml", "Upload Diagnostic Results");
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

    private String formatAppointmentTime(Appointment appointment, boolean weekView) {
        Long epoch = appointment.resolveAppointmentEpochMillis();

        if (epoch != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(epoch),
                    ZoneId.systemDefault()
            );

            return weekView
                    ? dateTime.format(WEEK_TIME_FORMAT)
                    : dateTime.format(TIME_FORMAT);
        }

        if (appointment.getAppointmentSlot() != null && !appointment.getAppointmentSlot().isBlank()) {
            return appointment.getAppointmentSlot();
        }

        return "";
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

    private boolean isTimeSlotValid(String serviceType, String requestedTimeString) {
        LocalTime requestedTime = parseTime(requestedTimeString);

        if (requestedTime == null) {
            return false;
        }

        if (serviceType != null && serviceType.equalsIgnoreCase("Emergency Care")) {
            return true;
        }

        LocalTime openTime = LocalTime.of(8, 0);
        LocalTime closeTime = LocalTime.of(20, 0);

        return !requestedTime.isBefore(openTime) && !requestedTime.isAfter(closeTime);
    }

    private void populateTimeComboBox(ComboBox<String> timeCombo, String serviceType) {
        timeCombo.getItems().clear();

        LocalTime startTime;
        LocalTime endTime;

        if (serviceType != null && serviceType.equalsIgnoreCase("Emergency Care")) {
            startTime = LocalTime.MIDNIGHT;
            endTime = LocalTime.of(23, 30);
        } else {
            startTime = LocalTime.of(8, 0);
            endTime = LocalTime.of(20, 0);
        }

        LocalTime currentTime = startTime;

        while (!currentTime.isAfter(endTime)) {
            timeCombo.getItems().add(currentTime.format(TIME_FORMAT));

            if (currentTime.equals(LocalTime.of(23, 30))) {
                break;
            }

            currentTime = currentTime.plusMinutes(30);
        }
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

    @FXML private void onDashboard() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }

    @FXML private void onPatients() {
        SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients");
    }

    @FXML private void onSchedule() {
    }

    @FXML private void onProfile() {
        SceneRouter.go("hospital-profile-view.fxml", "Hospital Profile");
    }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }
}