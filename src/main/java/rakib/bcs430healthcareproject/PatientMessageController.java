package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Controller for patient-to-doctor messaging.
 */
public class PatientMessageController {

    @FXML private Label patientNameLabel;
    @FXML private Label doctorInfoLabel;
    @FXML private Label statusLabel;
    @FXML private ComboBox<Doctor> doctorComboBox;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesVBox;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile patientProfile;
    private Doctor selectedDoctor;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        patientProfile = userContext.getProfile();
        if (patientProfile == null) {
            SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
            return;
        }

        String patientName = patientProfile.getName() != null ? patientProfile.getName() : "Patient";
        patientNameLabel.setText(patientName);

        doctorComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText(null);
                } else {
                    String specialty = doctor.getSpecialty() != null ? doctor.getSpecialty() : "Doctor";
                    setText(doctor.getName() + " - " + specialty);
                }
            }
        });

        doctorComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText("Choose a doctor conversation...");
                } else {
                    String specialty = doctor.getSpecialty() != null ? doctor.getSpecialty() : "Doctor";
                    setText(doctor.getName() + " - " + specialty);
                }
            }
        });

        loadDoctors();
    }

    private void loadDoctors() {
        showStatus("Loading your doctors...", false);

        firebaseService.getDoctorsForPatient(patientProfile.getUid())
                .thenAccept(doctors -> Platform.runLater(() -> renderDoctors(doctors)))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Failed to load doctors: " + cleanErrorMessage(e), true)
                    );
                    return null;
                });
    }

    private void renderDoctors(List<Doctor> doctors) {
        doctorComboBox.getItems().clear();

        if (doctors == null) {
            doctors = new ArrayList<>();
        }

        if (doctors.isEmpty()) {
            doctorInfoLabel.setText("No doctors found. Book an appointment first to start messaging.");
            showStatus("No doctor conversations available.", false);
            return;
        }

        doctorComboBox.getItems().addAll(doctors);
        doctorComboBox.getSelectionModel().selectFirst();
        selectedDoctor = doctorComboBox.getSelectionModel().getSelectedItem();
        updateDoctorInfo();
        loadMessages();

        showStatus("Loaded " + doctors.size() + " doctor conversation(s).", false);
    }

    @FXML
    private void onDoctorSelected() {
        selectedDoctor = doctorComboBox.getSelectionModel().getSelectedItem();
        updateDoctorInfo();
        loadMessages();
    }

    @FXML
    private void onSendMessage() {
        if (selectedDoctor == null) {
            showStatus("Please select a doctor first.", true);
            return;
        }

        String text = safeTrim(messageInputArea.getText());
        if (text.isBlank()) {
            showStatus("Please enter a message before sending.", true);
            return;
        }

        Message message = new Message();
        message.setDoctorUid(selectedDoctor.getUid());
        message.setDoctorName(selectedDoctor.getName());
        message.setPatientUid(patientProfile.getUid());
        message.setPatientName(patientProfile.getName());
        message.setSenderUid(patientProfile.getUid());
        message.setSenderName(patientProfile.getName());
        message.setSenderRole("PATIENT");
        message.setMessageText(text);
        message.setCreatedAt(System.currentTimeMillis());

        sendButton.setDisable(true);
        showStatus("Sending message...", false);

        firebaseService.saveMessage(message)
                .thenAccept(messageId -> Platform.runLater(() -> {
                    messageInputArea.clear();
                    sendButton.setDisable(false);
                    showStatus("Message sent successfully.", false);
                    loadMessages();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        showStatus("Failed to send message: " + cleanErrorMessage(e), true);
                    });
                    return null;
                });
    }

    @FXML
    private void onClear() {
        messageInputArea.clear();
        showStatus("Message cleared.", false);
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    private void updateDoctorInfo() {
        if (selectedDoctor == null) {
            doctorInfoLabel.setText("No doctor selected.");
            return;
        }

        String specialty = fallback(selectedDoctor.getSpecialty());
        String clinic = fallback(selectedDoctor.getClinicName());
        doctorInfoLabel.setText("Specialty: " + specialty + "   |   Clinic: " + clinic);
    }

    private void loadMessages() {
        messagesVBox.getChildren().clear();

        if (selectedDoctor == null) {
            showStatus("Please select a doctor conversation.", false);
            return;
        }

        showStatus("Loading conversation...", false);

        firebaseService.getMessagesBetweenDoctorAndPatient(
                        selectedDoctor.getUid(),
                        patientProfile.getUid()
                )
                .thenAccept(messages -> Platform.runLater(() -> renderMessages(messages)))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Failed to load messages: " + cleanErrorMessage(e), true)
                    );
                    return null;
                });
    }

    private void renderMessages(List<Message> messages) {
        messagesVBox.getChildren().clear();

        if (messages == null) {
            messages = new ArrayList<>();
        }

        messages.sort(Comparator.comparingLong(m ->
                m.getCreatedAt() != null ? m.getCreatedAt() : 0L
        ));

        if (messages.isEmpty()) {
            Label emptyLabel = new Label("No messages yet. Start the conversation below.");
            emptyLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13;");
            messagesVBox.getChildren().add(emptyLabel);
            showStatus("No messages found.", false);
            return;
        }

        for (Message message : messages) {
            messagesVBox.getChildren().add(createMessageBubble(message));
        }

        showStatus("Loaded " + messages.size() + " message(s).", false);
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    private VBox createMessageBubble(Message message) {
        boolean isPatientMessage = patientProfile.getUid() != null
                && patientProfile.getUid().equals(message.getSenderUid());

        VBox wrapper = new VBox(4);

        HBox row = new HBox();
        row.setAlignment(isPatientMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(12));
        bubble.setStyle(isPatientMessage
                ? "-fx-background-color: #DBEAFE; -fx-background-radius: 14;"
                : "-fx-background-color: white; -fx-border-color: #D1D5DB; -fx-border-radius: 14; -fx-background-radius: 14;");

        Label senderLabel = new Label(isPatientMessage ? "You" : fallback(message.getSenderName()));
        senderLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12; -fx-font-weight: bold;");

        Label textLabel = new Label(fallback(message.getMessageText()));
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13;");

        Label timeLabel = new Label(formatTime(message.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11;");

        bubble.getChildren().addAll(senderLabel, textLabel, timeLabel);
        row.getChildren().add(bubble);
        wrapper.getChildren().add(row);

        return wrapper;
    }

    private String formatTime(Long millis) {
        if (millis == null) {
            return "";
        }
        return timeFormat.format(new Date(millis));
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}