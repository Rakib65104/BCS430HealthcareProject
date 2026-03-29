package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Controller for doctor-to-patient messaging.
 */
public class DoctorMessageController {

    @FXML private Label doctorNameLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientInfoLabel;
    @FXML private Label statusLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesVBox;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile selectedPatient;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        selectedPatient = userContext.getSelectedPatientProfile();
        if (selectedPatient == null) {
            SceneRouter.go("doctor-patients-view.fxml", "My Patients");
            return;
        }

        String doctorName = userContext.getName() != null ? userContext.getName() : "Doctor";
        doctorNameLabel.setText("Dr. " + doctorName);

        patientNameLabel.setText(
                selectedPatient.getName() != null ? selectedPatient.getName() : "Patient"
        );

        String email = fallback(selectedPatient.getEmail());
        String phone = fallback(selectedPatient.getPhoneNumber());
        patientInfoLabel.setText("Email: " + email + "   |   Phone: " + phone);

        loadMessages();
    }

    @FXML
    private void onSendMessage() {
        String text = safeTrim(messageInputArea.getText());

        if (text.isBlank()) {
            showStatus("Please enter a message before sending.", true);
            return;
        }

        Message message = new Message();
        message.setDoctorUid(userContext.getUid());
        message.setDoctorName(userContext.getName());
        message.setPatientUid(selectedPatient.getUid());
        message.setPatientName(selectedPatient.getName());
        message.setSenderUid(userContext.getUid());
        message.setSenderName(userContext.getName());
        message.setSenderRole("DOCTOR");
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
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }

    private void loadMessages() {
        showStatus("Loading conversation...", false);

        firebaseService.getMessagesBetweenDoctorAndPatient(
                        userContext.getUid(),
                        selectedPatient.getUid()
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

        messages.sort(Comparator.comparingLong(Message::getCreatedAt));

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
        boolean isDoctorMessage = userContext.getUid() != null
                && userContext.getUid().equals(message.getSenderUid());

        VBox wrapper = new VBox(4);
        wrapper.setFillWidth(true);

        HBox row = new HBox();
        row.setAlignment(isDoctorMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(12));
        bubble.setStyle(isDoctorMessage
                ? "-fx-background-color: #CCFBF1; -fx-background-radius: 14;"
                : "-fx-background-color: white; -fx-border-color: #D1D5DB; -fx-border-radius: 14; -fx-background-radius: 14;");

        Label senderLabel = new Label(isDoctorMessage ? "You" : fallback(message.getSenderName()));
        senderLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12; -fx-font-weight: bold;");

        Label textLabel = new Label(fallback(message.getMessageText()));
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13;");

        Label timeLabel = new Label(formatTime(message.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11;");

        bubble.getChildren().addAll(senderLabel, textLabel, timeLabel);
        row.getChildren().add(bubble);

        wrapper.getChildren().add(row);
        VBox.setVgrow(wrapper, Priority.NEVER);

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