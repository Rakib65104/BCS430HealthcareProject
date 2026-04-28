package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HospitalDiagnosticController {

    @FXML private Label hospitalNameLabel;
    @FXML private Label statusLabel;

    @FXML private ComboBox<Appointment> appointmentComboBox;

    @FXML private Label patientNameLabel;
    @FXML private Label doctorNameLabel;
    @FXML private Label appointmentDateLabel;
    @FXML private Label reasonLabel;

    @FXML private TextArea hospitalFindingsTextArea;
    @FXML private TextArea diagnosticResultsTextArea;

    @FXML private Button publishButton;
    @FXML private VBox appointmentPreviewBox;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private HospitalProfile hospitalProfile;

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        hospitalProfile = userContext.getHospitalProfile();

        setupInitialState();
        setupAppointmentComboBox();
        loadHospitalAppointments();
    }

    private void setupInitialState() {
        if (hospitalProfile != null) {
            setLabelText(hospitalNameLabel, valueOrDefault(hospitalProfile.getHospitalName(), "Hospital"));
        } else {
            setLabelText(hospitalNameLabel, "Hospital");
        }

        setLabelText(statusLabel, "Select an appointment to upload diagnostic results.");
        clearAppointmentPreview();

        if (publishButton != null) {
            publishButton.setDisable(true);
        }
    }

    private void setupAppointmentComboBox() {
        if (appointmentComboBox == null) {
            return;
        }

        appointmentComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Appointment appointment, boolean empty) {
                super.updateItem(appointment, empty);

                if (empty || appointment == null) {
                    setText(null);
                } else {
                    setText(formatAppointmentOption(appointment));
                }
            }
        });

        appointmentComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Appointment appointment, boolean empty) {
                super.updateItem(appointment, empty);

                if (empty || appointment == null) {
                    setText("Choose a scheduled hospital appointment");
                } else {
                    setText(formatAppointmentOption(appointment));
                }
            }
        });

        appointmentComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateAppointmentPreview(newValue);

            if (publishButton != null) {
                publishButton.setDisable(newValue == null);
            }
        });
    }

    private void loadHospitalAppointments() {
        if (hospitalProfile == null || hospitalProfile.getUid() == null || hospitalProfile.getUid().isBlank()) {
            setError("Hospital profile could not be loaded.");
            return;
        }

        setLabelText(statusLabel, "Loading scheduled hospital appointments...");

        firebaseService.getAppointmentsForHospital(hospitalProfile.getUid())
                .thenAccept(appointments -> Platform.runLater(() -> populateAppointments(appointments)))
                .exceptionally(e -> {
                    Platform.runLater(() -> setError("Failed to load appointments: " + cleanError(e)));
                    return null;
                });
    }

    private void populateAppointments(List<Appointment> appointments) {
        if (appointmentComboBox == null) {
            return;
        }

        appointmentComboBox.getItems().clear();

        List<Appointment> availableAppointments = appointments == null
                ? List.of()
                : appointments.stream()
                .filter(Objects::nonNull)
                .filter(this::isUsableAppointment)
                .sorted(Comparator.comparing(
                        Appointment::resolveAppointmentEpochMillis,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .collect(Collectors.toList());

        appointmentComboBox.getItems().addAll(availableAppointments);

        if (availableAppointments.isEmpty()) {
            setLabelText(statusLabel, "No eligible hospital appointments found.");
            clearAppointmentPreview();
            if (publishButton != null) {
                publishButton.setDisable(true);
            }
        } else {
            setSuccess("Found " + availableAppointments.size() + " appointment(s) ready for diagnostic upload.");
        }
    }

    private boolean isUsableAppointment(Appointment appointment) {
        if (appointment == null) {
            return false;
        }

        String status = appointment.getStatus();
        if (status != null && status.equalsIgnoreCase("CANCELLED")) {
            return false;
        }

        if (appointment.getPatientUid() == null || appointment.getPatientUid().isBlank()) {
            return false;
        }

        if (appointment.getDoctorUid() == null || appointment.getDoctorUid().isBlank()) {
            return false;
        }

        return true;
    }

    private void updateAppointmentPreview(Appointment appointment) {
        if (appointment == null) {
            clearAppointmentPreview();
            return;
        }

        setLabelText(patientNameLabel, valueOrDefault(appointment.getPatientName(), "Unknown Patient"));
        setLabelText(doctorNameLabel, valueOrDefault(appointment.getDoctorName(), "Unknown Doctor"));
        setLabelText(appointmentDateLabel, formatAppointmentTime(appointment));
        setLabelText(reasonLabel, valueOrDefault(appointment.getReason(), "No reason provided."));

        if (hospitalFindingsTextArea != null) {
            hospitalFindingsTextArea.setText(valueOrDefault(appointment.getHospitalFindings(), ""));
        }

        if (diagnosticResultsTextArea != null) {
            diagnosticResultsTextArea.setText(valueOrDefault(appointment.getDiagnosticResults(), ""));
        }

        if (appointment.getDiagnosticResultsUploadedAt() != null) {
            setLabelText(statusLabel, "This appointment already has diagnostic results. Publishing again will update them.");
        } else {
            setLabelText(statusLabel, "Ready to upload diagnostic results for this appointment.");
        }
    }

    private void clearAppointmentPreview() {
        setLabelText(patientNameLabel, "No appointment selected");
        setLabelText(doctorNameLabel, "No appointment selected");
        setLabelText(appointmentDateLabel, "No appointment selected");
        setLabelText(reasonLabel, "No appointment selected");

        if (hospitalFindingsTextArea != null) {
            hospitalFindingsTextArea.clear();
        }

        if (diagnosticResultsTextArea != null) {
            diagnosticResultsTextArea.clear();
        }
    }

    @FXML
    private void onPublishResults() {
        Appointment selectedAppointment = appointmentComboBox != null
                ? appointmentComboBox.getValue()
                : null;

        if (selectedAppointment == null) {
            setError("Please select an appointment first.");
            return;
        }

        String findings = hospitalFindingsTextArea != null
                ? hospitalFindingsTextArea.getText().trim()
                : "";

        String results = diagnosticResultsTextArea != null
                ? diagnosticResultsTextArea.getText().trim()
                : "";

        if (findings.isBlank()) {
            setError("Hospital findings are required.");
            return;
        }

        if (results.isBlank()) {
            setError("Diagnostic results are required.");
            return;
        }

        if (hospitalProfile != null) {
            selectedAppointment.setHospitalUid(hospitalProfile.getUid());
            selectedAppointment.setHospitalName(hospitalProfile.getHospitalName());
        }

        selectedAppointment.setHospitalFindings(findings);
        selectedAppointment.setDiagnosticResults(results);
        selectedAppointment.setDiagnosticResultsUploadedAt(System.currentTimeMillis());

        if (publishButton != null) {
            publishButton.setDisable(true);
        }

        setLabelText(statusLabel, "Publishing diagnostic results to the hub...");

        firebaseService.publishHospitalDiagnosticResults(selectedAppointment)
                .thenRun(() -> Platform.runLater(() -> {
                    setSuccess("Diagnostic results published. Patient and physician were notified instantly.");
                    showInfo("Success", "Diagnostic results were uploaded to the hub successfully.");
                    loadHospitalAppointments();

                    if (publishButton != null) {
                        publishButton.setDisable(false);
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        setError("Upload failed: " + cleanError(e));

                        if (publishButton != null) {
                            publishButton.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }

    @FXML
    private void onRefresh() {
        loadHospitalAppointments();
    }

    private String formatAppointmentOption(Appointment appointment) {
        return valueOrDefault(appointment.getPatientName(), "Unknown Patient")
                + "  •  "
                + valueOrDefault(appointment.getDoctorName(), "Unknown Doctor")
                + "  •  "
                + formatAppointmentTime(appointment);
    }

    private String formatAppointmentTime(Appointment appointment) {
        try {
            Long millis = appointment.resolveAppointmentEpochMillis();

            if (millis != null) {
                return Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .format(DATE_TIME_FORMAT);
            }

            if (appointment.getAppointmentDate() != null && appointment.getAppointmentSlot() != null) {
                return appointment.getAppointmentDate() + " • " + appointment.getAppointmentSlot();
            }

            return valueOrDefault(appointment.getAppointmentTime(), "Time not available");
        } catch (Exception e) {
            return "Time not available";
        }
    }

    private void setError(String message) {
        setLabelText(statusLabel, message);

        if (statusLabel != null) {
            statusLabel.setStyle("-fx-text-fill: #B91C1C; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }

    private void setSuccess(String message) {
        setLabelText(statusLabel, message);

        if (statusLabel != null) {
            statusLabel.setStyle("-fx-text-fill: #047857; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String cleanError(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error.";
        }

        String message = throwable.getMessage();

        if (message == null && throwable.getCause() != null) {
            message = throwable.getCause().getMessage();
        }

        if (message == null) {
            return "Unknown error.";
        }

        return message.replace("java.lang.RuntimeException:", "").trim();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}