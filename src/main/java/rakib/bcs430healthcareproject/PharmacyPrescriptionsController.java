package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for a simple pharmacy portal that can fill prescriptions.
 */
public class PharmacyPrescriptionsController {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML private VBox prescriptionsListVBox;
    @FXML private Label statusLabel;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        loadPrescriptions();
    }

    private void loadPrescriptions() {
        showStatus("Loading prescriptions...", false);
        firebaseService.getAllPrescriptions()
                .thenAccept(prescriptions -> Platform.runLater(() -> renderPrescriptions(prescriptions)))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load prescriptions: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void renderPrescriptions(List<Prescription> prescriptions) {
        prescriptionsListVBox.getChildren().clear();

        if (prescriptions == null || prescriptions.isEmpty()) {
            showStatus("No prescriptions available.", false);
            return;
        }

        showStatus("Loaded " + prescriptions.size() + " prescription(s).", false);
        for (Prescription prescription : prescriptions) {
            prescriptionsListVBox.getChildren().add(createPrescriptionCard(prescription));
        }
    }

    private VBox createPrescriptionCard(Prescription prescription) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #D1FAE5; -fx-border-radius: 12;");

        HBox topRow = new HBox(10);
        Label patientLabel = new Label(valueOrDefault(prescription.getPatientName(), "Unknown Patient"));
        patientLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 16; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusBadge = new Label(valueOrDefault(prescription.getStatus(), Prescription.STATUS_SENT));
        statusBadge.setStyle(getStatusStyle(prescription.getStatus()));
        topRow.getChildren().addAll(patientLabel, spacer, statusBadge);

        Label medicationLabel = new Label("Medication: " + valueOrDefault(prescription.getMedicationInformation(), "Not provided"));
        medicationLabel.setWrapText(true);
        medicationLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label pharmacyLabel = new Label("Pharmacy: " + formatPharmacy(prescription));
        pharmacyLabel.setWrapText(true);
        pharmacyLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label instructionsLabel = new Label("Instructions: " + valueOrDefault(prescription.getInstructions(), "Not provided"));
        instructionsLabel.setWrapText(true);
        instructionsLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label sentLabel = new Label("Sent by Dr. " + valueOrDefault(prescription.getDoctorName(), "Unknown")
                + " on " + formatTimestamp(prescription.getCreatedAt()));
        sentLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(topRow, medicationLabel, pharmacyLabel, instructionsLabel, sentLabel);

        if (Prescription.STATUS_FILLED.equalsIgnoreCase(prescription.getStatus())) {
            Label filledLabel = new Label("Filled: " + formatTimestamp(prescription.getFilledAt())
                    + " by " + valueOrDefault(prescription.getFilledBy(), valueOrDefault(prescription.getPharmacyName(), "Pharmacy")));
            filledLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(filledLabel);
        } else {
            Button fillButton = new Button("Mark Filled");
            fillButton.setStyle("-fx-background-color: #14B8A6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
            fillButton.setOnAction(event -> markPrescriptionFilled(prescription, fillButton));
            card.getChildren().add(fillButton);
        }

        return card;
    }

    private void markPrescriptionFilled(Prescription prescription, Button fillButton) {
        fillButton.setDisable(true);
        showStatus("Updating prescription status...", false);

        prescription.setStatus(Prescription.STATUS_FILLED);
        prescription.setFilledAt(System.currentTimeMillis());
        prescription.setFilledBy(valueOrDefault(prescription.getPharmacyName(), "Pharmacy"));

        firebaseService.updatePrescription(prescription)
                .thenRun(() -> Platform.runLater(this::loadPrescriptions))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        fillButton.setDisable(false);
                        showStatus("Failed to update prescription: " + cleanErrorMessage(e), true);
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("login-view.fxml", "Login");
    }

    private String getStatusStyle(String status) {
        if (Prescription.STATUS_FILLED.equalsIgnoreCase(status)) {
            return "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12;";
        }
        return "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12;";
    }

    private String formatPharmacy(Prescription prescription) {
        String name = valueOrDefault(prescription.getPharmacyName(), "Pharmacy");
        String address = valueOrDefault(prescription.getPharmacyAddress(), "Address not provided");
        String phone = valueOrDefault(prescription.getPharmacyPhoneNumber(), "Phone not provided");
        return name + " | " + address + " | " + phone;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMAT);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
}
