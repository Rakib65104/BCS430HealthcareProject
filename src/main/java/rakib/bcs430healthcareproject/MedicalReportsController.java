package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedicalReportsController {

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private VBox reportsContainer;

    private FirebaseService firebaseService;
    private UserContext userContext;

    private static final DateTimeFormatter REPORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        configurePageForRole();
        loadReports();
    }

    private void configurePageForRole() {
        if (userContext.isPatient()) {
            pageTitleLabel.setText("My Medical Reports");
            pageSubtitleLabel.setText("Hospital uploaded diagnostics, scans, and medical findings.");
        } else if (userContext.isDoctor()) {
            pageTitleLabel.setText("Patient Medical Reports");
            pageSubtitleLabel.setText("Shared hospital diagnostics and incoming patient report records.");
        } else if (userContext.isHospital()) {
            pageTitleLabel.setText("Uploaded Diagnostic Reports");
            pageSubtitleLabel.setText("Diagnostic reports published by your hospital.");
        } else {
            pageTitleLabel.setText("Medical Reports");
            pageSubtitleLabel.setText("Shared report hub.");
        }
    }

    private void loadReports() {
        reportsContainer.getChildren().clear();
        reportsContainer.getChildren().add(buildLoadingCard());

        String uid = userContext.getUid();

        if (userContext.isPatient()) {
            firebaseService.getDiagnosticReportsForPatient(uid)
                    .thenAccept(reports -> Platform.runLater(() -> renderReports(reports)));
        } else if (userContext.isDoctor()) {
            firebaseService.getDiagnosticReportsForDoctor(uid)
                    .thenAccept(reports -> Platform.runLater(() -> renderReports(reports)));
        } else if (userContext.isHospital()) {
            firebaseService.getDiagnosticReportsForHospital(uid)
                    .thenAccept(reports -> Platform.runLater(() -> renderReports(reports)));
        }
    }

    private void renderReports(List<DiagnosticReport> reports) {
        reportsContainer.getChildren().clear();

        if (reports == null || reports.isEmpty()) {
            reportsContainer.getChildren().add(buildEmptyCard());
            return;
        }

        for (DiagnosticReport report : reports) {
            reportsContainer.getChildren().add(buildReportCard(report));
        }
    }

    private VBox buildReportCard(DiagnosticReport report) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-border-color: #D1FAE5; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label title = new Label(valueOrDefault(report.getReportTitle(), "Hospital Diagnostic Report"));
        title.setStyle("-fx-text-fill:#0F766E; -fx-font-size:17; -fx-font-weight:bold;");

        Label meta = new Label(
                "Patient: " + valueOrDefault(report.getPatientName(), "Unknown")
                        + "    |    Doctor: " + valueOrDefault(report.getDoctorName(), "Unknown")
                        + "    |    Hospital: " + valueOrDefault(report.getHospitalName(), "Unknown")
        );
        meta.setWrapText(true);
        meta.setStyle("-fx-text-fill:#475569; -fx-font-size:12;");

        Label type = new Label("Type: " + valueOrDefault(report.getReportType(), "Diagnostic Result"));
        type.setStyle("-fx-text-fill:#334155; -fx-font-size:12; -fx-font-weight:bold;");

        Label findingsTitle = new Label("Hospital Findings");
        findingsTitle.setStyle("-fx-text-fill:#0F766E; -fx-font-size:13; -fx-font-weight:bold;");

        Label findings = new Label(valueOrDefault(report.getHospitalFindings(), "No findings."));
        findings.setWrapText(true);
        findings.setStyle("-fx-text-fill:#334155; -fx-font-size:12;");

        Label resultTitle = new Label("Diagnostic Results");
        resultTitle.setStyle("-fx-text-fill:#0F766E; -fx-font-size:13; -fx-font-weight:bold;");

        Label results = new Label(valueOrDefault(report.getDiagnosticResults(), "No results."));
        results.setWrapText(true);
        results.setStyle("-fx-text-fill:#334155; -fx-font-size:12;");

        Label uploaded = new Label("Uploaded: " + formatDate(report.getUploadedAt()));
        uploaded.setStyle("-fx-text-fill:#64748B; -fx-font-size:11;");

        card.getChildren().addAll(title, meta, type, findingsTitle, findings, resultTitle, results, uploaded);
        return card;
    }

    private VBox buildEmptyCard() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color:#F8FAFC; -fx-border-color:#E2E8F0; -fx-border-radius:12; -fx-background-radius:12;");

        Label title = new Label("No medical reports available");
        title.setStyle("-fx-text-fill:#0F172A; -fx-font-size:15; -fx-font-weight:bold;");

        Label sub = new Label("Uploaded hospital diagnostic reports will appear here.");
        sub.setStyle("-fx-text-fill:#64748B; -fx-font-size:12;");

        box.getChildren().addAll(title, sub);
        return box;
    }

    private VBox buildLoadingCard() {
        VBox box = new VBox();
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color:#F8FAFC; -fx-border-color:#E2E8F0; -fx-border-radius:12; -fx-background-radius:12;");

        Label title = new Label("Loading medical reports...");
        title.setStyle("-fx-text-fill:#64748B; -fx-font-size:13;");

        box.getChildren().add(title);
        return box;
    }

    @FXML
    private void onBack() {
        if (userContext.isPatient()) {
            SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
        } else if (userContext.isDoctor()) {
            SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
        } else if (userContext.isHospital()) {
            SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDate(Long millis) {
        if (millis == null) return "Unknown";

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(REPORT_DATE_FORMAT);
    }
}