package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class HospitalMedicalRecordsController implements Initializable {

    @FXML private Label hospitalNameLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<DiagnosticReport> reportsTable;
    @FXML private TableColumn<DiagnosticReport, String> colDate;
    @FXML private TableColumn<DiagnosticReport, String> colPatient;
    @FXML private TableColumn<DiagnosticReport, String> colDoctor;
    @FXML private TableColumn<DiagnosticReport, String> colType;
    @FXML private TableColumn<DiagnosticReport, String> colTitle;

    @FXML private Label selectedTitleLabel;
    @FXML private Label selectedPatientLabel;
    @FXML private Label selectedDoctorLabel;
    @FXML private Label selectedDateLabel;
    @FXML private TextArea findingsArea;
    @FXML private TextArea resultsArea;

    private FirebaseService firebaseService;
    private UserContext userContext;

    private final ObservableList<DiagnosticReport> reportsList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isHospital()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        setupTable();
        loadReports();
    }

    private void setupTable() {
        colDate.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(formatDate(cell.getValue().getUploadedAt())));

        colPatient.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(valueOrDefault(cell.getValue().getPatientName(), "Unknown Patient")));

        colDoctor.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(valueOrDefault(cell.getValue().getDoctorName(), "Unknown Doctor")));

        colType.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(valueOrDefault(cell.getValue().getReportType(), "Diagnostic Result")));

        colTitle.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(valueOrDefault(cell.getValue().getReportTitle(), "Untitled Report")));

        reportsTable.setItems(reportsList);

        reportsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            showReportDetails(newValue);
        });
    }

    private void loadReports() {
        HospitalProfile hospital = userContext.getHospitalProfile();

        if (hospital == null || hospital.getUid() == null || hospital.getUid().isBlank()) {
            statusLabel.setText("Hospital profile could not be loaded.");
            return;
        }

        hospitalNameLabel.setText(valueOrDefault(hospital.getHospitalName(), "Hospital"));
        statusLabel.setText("Loading medical records...");

        firebaseService.getDiagnosticReportsForHospital(hospital.getUid())
                .thenAccept(reports -> Platform.runLater(() -> {
                    reportsList.clear();

                    if (reports != null) {
                        reportsList.addAll(reports);
                    }

                    if (reportsList.isEmpty()) {
                        statusLabel.setText("No uploaded medical records found yet.");
                    } else {
                        statusLabel.setText("Showing " + reportsList.size() + " uploaded medical record(s).");
                        reportsTable.getSelectionModel().selectFirst();
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("Failed to load records: " + cleanErrorMessage(e)));
                    return null;
                });
    }

    private void showReportDetails(DiagnosticReport report) {
        if (report == null) {
            selectedTitleLabel.setText("No report selected");
            selectedPatientLabel.setText("Patient: -");
            selectedDoctorLabel.setText("Doctor: -");
            selectedDateLabel.setText("Uploaded: -");
            findingsArea.clear();
            resultsArea.clear();
            return;
        }

        selectedTitleLabel.setText(valueOrDefault(report.getReportTitle(), "Untitled Report"));
        selectedPatientLabel.setText("Patient: " + valueOrDefault(report.getPatientName(), "Unknown Patient"));
        selectedDoctorLabel.setText("Doctor: " + valueOrDefault(report.getDoctorName(), "Unknown Doctor"));
        selectedDateLabel.setText("Uploaded: " + formatDate(report.getUploadedAt()));

        findingsArea.setText(valueOrDefault(report.getHospitalFindings(), ""));
        resultsArea.setText(valueOrDefault(report.getDiagnosticResults(), ""));
    }

    @FXML
    private void onRefresh() {
        loadReports();
    }

    @FXML
    private void onBack() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }

    private String formatDate(Long millis) {
        if (millis == null) {
            return "Date not available";
        }

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMAT);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() == null ? "Unknown error" : cause.getMessage();
    }
}