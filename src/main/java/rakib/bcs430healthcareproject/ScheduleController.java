package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleController {

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML private Label doctorNameLabel;
    @FXML private Label statusLabel;
    @FXML private DatePicker scheduleDatePicker;

    @FXML private TableView<Schedule> appointmentTable;
    @FXML private TableColumn<Schedule, String> colTime;
    @FXML private TableColumn<Schedule, String> colPatient;
    @FXML private TableColumn<Schedule, String> colType;
    @FXML private TableColumn<Schedule, String> colStatus;
    @FXML private TableColumn<Schedule, String> colNotes;

    @FXML private Button btnReschedule;
    @FXML private Button btnPrescription;
    @FXML private Button btnMessage;
    @FXML private Button btnBookSchedule;

    @FXML private TextField txtName;
    @FXML private TextField txtContact;
    @FXML private TextArea txtReason;
    @FXML private ComboBox<String> comboTime;
    @FXML private ComboBox<String> comboDuration;
    @FXML private ComboBox<String> comboType;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private final Map<String, List<Schedule>> manualSchedulesByDate = new HashMap<>();

    private FirebaseService firebaseService;
    private UserContext userContext;
    private Schedule editingSchedule;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        String doctorName = userContext.getName() != null ? userContext.getName() : "Doctor";
        doctorNameLabel.setText("Dr. " + doctorName + "'s Daily Schedule");

        setupTable();
        setupInputs();
        loadScheduleForSelectedDate();
    }

    private void setupTable() {
        colTime.setCellValueFactory(cell -> cell.getValue().timeProperty());
        colPatient.setCellValueFactory(cell -> cell.getValue().patientNameProperty());
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colNotes.setCellValueFactory(cell -> cell.getValue().notesProperty());

        appointmentTable.setItems(scheduleList);
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || editingSchedule != null) {
                return;
            }

            txtName.setText(orEmpty(newValue.getPatientName()));
            txtContact.setText(orEmpty(newValue.getContact()));
            txtReason.setText(orEmpty(newValue.getNotes()));
            comboTime.setValue(orEmpty(newValue.getTime()));
            comboType.setValue(orEmpty(newValue.getType()));
        });
    }

    private void setupInputs() {
        scheduleDatePicker.setValue(LocalDate.now());
        scheduleDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> loadScheduleForSelectedDate());

        comboTime.getItems().addAll(
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM",
                "11:00 AM", "11:30 AM", "01:00 PM", "01:30 PM",
                "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM",
                "04:00 PM", "04:30 PM"
        );
        comboDuration.getItems().addAll("15 min", "30 min", "45 min", "1 hr");
        comboDuration.setValue("30 min");
        comboType.getItems().addAll("Check-up", "Consultation", "Emergency", "Follow-up", "Surgery", "General");
    }

    private void loadScheduleForSelectedDate() {
        LocalDate selectedDate = getSelectedDate();
        showStatus("Loading appointments for " + selectedDate + "...", false);

        firebaseService.getDoctorAppointmentsForDate(userContext.getUid(), selectedDate)
                .thenAccept(appointments -> Platform.runLater(() -> renderSchedule(selectedDate, appointments)))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        scheduleList.clear();
                        renderManualSchedules(selectedDate);
                        showStatus("Failed to load appointments: " + cleanErrorMessage(e), true);
                    });
                    return null;
                });
    }

    private void renderSchedule(LocalDate selectedDate, List<Appointment> appointments) {
        scheduleList.clear();

        if (appointments != null) {
            appointments.sort(Comparator.comparingLong(this::resolveSortValue));
            for (Appointment appointment : appointments) {
                scheduleList.add(toSchedule(appointment));
            }
        }

        renderManualSchedules(selectedDate);

        if (scheduleList.isEmpty()) {
            showStatus("No appointments scheduled for " + selectedDate + ".", false);
        } else {
            showStatus("Loaded " + scheduleList.size() + " schedule item(s) for " + selectedDate + ".", false);
        }
    }

    private void renderManualSchedules(LocalDate selectedDate) {
        List<Schedule> manualSchedules = manualSchedulesByDate.getOrDefault(selectedDate.toString(), List.of());
        scheduleList.addAll(manualSchedules);
        scheduleList.sort(Comparator.comparing(schedule -> parseTimeForSort(schedule.getTime())));
    }

    @FXML
    private void handleBookAppointment() {
        if (txtName.getText().isBlank() || comboTime.getValue() == null || comboTime.getValue().isBlank()) {
            showAlert("Validation Error", "Please enter at least a patient name and time.");
            return;
        }

        LocalDate selectedDate = getSelectedDate();
        String newTime = comboTime.getValue().trim().toUpperCase(Locale.ENGLISH);

        if (editingSchedule != null) {
            updateExistingSchedule(editingSchedule, selectedDate, newTime);
            return;
        }

        Schedule newSchedule = new Schedule(
                newTime,
                txtName.getText().trim(),
                comboType.getValue() != null ? comboType.getValue() : "General",
                "SCHEDULED",
                txtReason.getText().trim()
        );
        newSchedule.setAppointmentDate(selectedDate.toString());
        newSchedule.setContact(txtContact.getText().trim());

        manualSchedulesByDate
                .computeIfAbsent(selectedDate.toString(), ignored -> new ArrayList<>())
                .add(newSchedule);

        loadScheduleForSelectedDate();
        clearForm();
        showStatus("Temporary schedule item added for " + selectedDate + ".", false);
    }

    private void updateExistingSchedule(Schedule selectedSchedule, LocalDate selectedDate, String newTime) {
        if (selectedSchedule.isPersistedAppointment()) {
            updatePersistedAppointment(selectedSchedule, selectedDate, newTime);
            return;
        }

        moveManualScheduleIfNeeded(selectedSchedule, selectedDate);
        selectedSchedule.setPatientName(txtName.getText().trim());
        selectedSchedule.setContact(txtContact.getText().trim());
        selectedSchedule.setTime(newTime);
        selectedSchedule.setType(comboType.getValue() != null ? comboType.getValue() : "General");
        selectedSchedule.setNotes(txtReason.getText().trim());
        selectedSchedule.setStatus("SCHEDULED");
        selectedSchedule.setAppointmentDate(selectedDate.toString());

        finishEditing("Schedule updated.", false);
    }

    private void updatePersistedAppointment(Schedule selectedSchedule, LocalDate selectedDate, String newTime) {
        Appointment appointment = selectedSchedule.getSourceAppointment();
        if (appointment == null) {
            finishEditing("This schedule item can no longer be edited.", true);
            return;
        }

        showStatus("Updating appointment...", false);

        firebaseService.isSlotStillAvailable(appointment.getDoctorUid(), selectedDate.toString(), newTime)
                .thenCompose(isAvailable -> {
                    boolean sameSlot = selectedDate.toString().equals(appointment.getAppointmentDate())
                            && newTime.equalsIgnoreCase(appointment.getAppointmentSlot());

                    if (!isAvailable && !sameSlot) {
                        throw new RuntimeException("That time slot is already booked.");
                    }

                    LocalDateTime localDateTime = LocalDateTime.of(selectedDate, parseTimeForSort(newTime));
                    long newTimestamp = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                    appointment.setAppointmentDate(selectedDate.toString());
                    appointment.setAppointmentSlot(newTime);
                    appointment.setAppointmentTime(selectedDate + " " + newTime);
                    appointment.setAppointmentDateTime(newTimestamp);
                    appointment.setReason(txtReason.getText().trim());
                    appointment.setNotes(txtReason.getText().trim());
                    appointment.setStatus("SCHEDULED");

                    return firebaseService.updateAppointment(appointment);
                })
                .thenAccept(ignored -> Platform.runLater(() -> {
                    clearForm();
                    editingSchedule = null;
                    btnBookSchedule.setText("Book Schedule");
                    loadScheduleForSelectedDate();
                    showStatus("Appointment updated successfully.", false);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> finishEditing("Failed to update appointment: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    @FXML
    private void handleReschedule() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();

        if (selectedSchedule == null) {
            showAlert("No Selection", "Please select an appointment from the table to reschedule.");
            return;
        }

        editingSchedule = selectedSchedule;
        txtName.setText(orEmpty(selectedSchedule.getPatientName()));
        txtContact.setText(orEmpty(selectedSchedule.getContact()));
        txtReason.setText(orEmpty(selectedSchedule.getNotes()));
        comboTime.setValue(orEmpty(selectedSchedule.getTime()));
        comboType.setValue(orEmpty(selectedSchedule.getType()));

        LocalDate rowDate = parseDate(selectedSchedule.getAppointmentDate());
        if (rowDate != null) {
            scheduleDatePicker.setValue(rowDate);
        }

        btnBookSchedule.setText("Update Schedule");
        showStatus("Editing selected schedule item. Update the form and click Update Schedule.", false);
    }

    @FXML
    private void handleCancelSelected() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showAlert("No Selection", "Please select a schedule item to cancel.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Schedule");
        confirm.setHeaderText("Cancel selected schedule item?");
        confirm.setContentText("Patient: " + selectedSchedule.getPatientName());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        if (selectedSchedule.isPersistedAppointment()) {
            Appointment appointment = selectedSchedule.getSourceAppointment();
            appointment.setStatus("CANCELLED");
            showStatus("Cancelling appointment...", false);
            firebaseService.updateAppointment(appointment)
                    .thenAccept(ignored -> Platform.runLater(() -> {
                        clearForm();
                        loadScheduleForSelectedDate();
                        showStatus("Appointment cancelled.", false);
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showStatus("Failed to cancel appointment: " + cleanErrorMessage(e), true));
                        return null;
                    });
            return;
        }

        selectedSchedule.setStatus("CANCELLED");
        appointmentTable.refresh();
        showStatus("Schedule item marked as cancelled.", false);
    }

    @FXML
    private void handleMarkComplete() {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showAlert("No Selection", "Please select a schedule item to mark complete.");
            return;
        }

        if (selectedSchedule.isPersistedAppointment()) {
            Appointment appointment = selectedSchedule.getSourceAppointment();
            appointment.setStatus("COMPLETED");
            showStatus("Updating appointment status...", false);
            firebaseService.updateAppointment(appointment)
                    .thenAccept(ignored -> Platform.runLater(() -> {
                        clearForm();
                        loadScheduleForSelectedDate();
                        showStatus("Appointment marked as completed.", false);
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showStatus("Failed to update appointment: " + cleanErrorMessage(e), true));
                        return null;
                    });
            return;
        }

        selectedSchedule.setStatus("COMPLETED");
        appointmentTable.refresh();
        showStatus("Schedule item marked as completed.", false);
    }

    @FXML
    private void handleSendPrescription() {
        openPatientWorkflow("prescription");
    }

    @FXML
    private void handleMessagePatient() {
        openPatientWorkflow("message");
    }

    private void openPatientWorkflow(String action) {
        Schedule selectedSchedule = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showAlert("No Patient Selected", "Please select a patient from the schedule first.");
            return;
        }

        String patientUid = selectedSchedule.getPatientUid();
        if (patientUid == null || patientUid.isBlank()) {
            showAlert(
                    "Patient Record Required",
                    "This schedule entry was added manually and is not linked to a patient account yet."
            );
            return;
        }

        showStatus("Loading patient record...", false);
        firebaseService.getPatientProfile(patientUid)
                .thenAccept(patientProfile -> Platform.runLater(() -> {
                    userContext.setSelectedPatientProfile(patientProfile);
                    if ("prescription".equals(action)) {
                        SceneRouter.go("doctor-prescription-view.fxml", "Send Prescription");
                    } else {
                        SceneRouter.go("doctor-message-view.fxml", "Send Message");
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load patient: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    @FXML
    private void onBackToDashboard() {
        SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
    }

    private Schedule toSchedule(Appointment appointment) {
        String time = appointment.getAppointmentSlot();
        if (time == null || time.isBlank()) {
            time = formatTime(appointment.getAppointmentDateTime());
        }

        Schedule schedule = new Schedule(
                time,
                orFallback(appointment.getPatientName(), "Unknown Patient"),
                resolveType(appointment),
                orFallback(appointment.getStatus(), "SCHEDULED"),
                orFallback(firstNonBlank(appointment.getReason(), appointment.getNotes()), "")
        );
        schedule.setAppointmentDate(appointment.getAppointmentDate());
        schedule.setPatientUid(appointment.getPatientUid());
        schedule.setSourceAppointment(appointment);
        return schedule;
    }

    private void moveManualScheduleIfNeeded(Schedule schedule, LocalDate newDate) {
        String previousDate = schedule.getAppointmentDate();
        String nextDate = newDate.toString();

        if (previousDate == null || previousDate.equals(nextDate)) {
            return;
        }

        List<Schedule> previousList = manualSchedulesByDate.get(previousDate);
        if (previousList != null) {
            previousList.remove(schedule);
        }

        manualSchedulesByDate
                .computeIfAbsent(nextDate, ignored -> new ArrayList<>())
                .add(schedule);
    }

    private void finishEditing(String message, boolean isError) {
        if (!isError) {
            clearForm();
        }
        appointmentTable.refresh();
        showStatus(message, isError);
    }

    private void clearForm() {
        txtName.clear();
        txtContact.clear();
        txtReason.clear();
        comboTime.getSelectionModel().clearSelection();
        comboType.getSelectionModel().clearSelection();
        comboDuration.setValue("30 min");
        editingSchedule = null;
        btnBookSchedule.setText("Book Schedule");
    }

    private LocalDate getSelectedDate() {
        LocalDate selectedDate = scheduleDatePicker.getValue();
        return selectedDate != null ? selectedDate : LocalDate.now();
    }

    private long resolveSortValue(Appointment appointment) {
        Long epoch = appointment.resolveAppointmentEpochMillis();
        return epoch != null ? epoch : Long.MAX_VALUE;
    }

    private LocalTime parseTimeForSort(String timeValue) {
        try {
            return LocalTime.parse(timeValue.trim().toUpperCase(Locale.ENGLISH), DISPLAY_TIME_FORMAT);
        } catch (Exception ignored) {
            return LocalTime.MAX;
        }
    }

    private String formatTime(Long epochMillis) {
        if (epochMillis == null) {
            return "Unknown";
        }

        LocalTime localTime = Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
        return localTime.format(DISPLAY_TIME_FORMAT).toUpperCase(Locale.ENGLISH);
    }

    private LocalDate parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveType(Appointment appointment) {
        if (Boolean.TRUE.equals(appointment.getNewPatient())) {
            return "New Patient";
        }

        String reason = appointment.getReason();
        if (reason == null || reason.isBlank()) {
            return "Consultation";
        }

        String trimmedReason = reason.trim();
        return trimmedReason.length() > 24 ? trimmedReason.substring(0, 24) + "..." : trimmedReason;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String orFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
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

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
