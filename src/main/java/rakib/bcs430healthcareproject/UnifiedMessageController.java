package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UnifiedMessageController {

    @FXML private Label currentUserNameLabel;
    @FXML private Label conversationTitleLabel;
    @FXML private Label conversationSubtitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label contactHelpLabel;
    @FXML private ListView<ContactOption> contactListView;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesVBox;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private ContactOption selectedContact;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        currentUserNameLabel.setText(formatCurrentUserName());
        contactHelpLabel.setText(contactHelpText());
        setupContactList();
        loadContacts();
    }

    private void setupContactList() {
        contactListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ContactOption contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setText(null);
                } else {
                    setText(contact.displayName + " | " + contact.roleLabel());
                }
            }
        });

        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedContact = newVal;
            updateHeader();
            loadMessages();
        });
    }

    private void loadContacts() {
        showStatus("Loading conversations...", false);
        CompletableFuture<List<ContactOption>> future;

        if (userContext.isPatient()) {
            future = loadPatientContacts();
        } else if (userContext.isDoctor()) {
            future = loadDoctorContacts();
        } else if (userContext.isPharmacy()) {
            future = loadPharmacyContacts();
        } else {
            showStatus("Messages are available for patients, doctors, and pharmacies.", true);
            return;
        }

        future.thenAccept(contacts -> Platform.runLater(() -> {
                    contactListView.getItems().setAll(contacts);
                    if (contacts.isEmpty()) {
                        showStatus("No message contacts are available yet.", false);
                        updateHeader();
                    } else {
                        hideStatus();
                        contactListView.getSelectionModel().selectFirst();
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus(cleanErrorMessage(e), true));
                    return null;
                });
    }

    private CompletableFuture<List<ContactOption>> loadPatientContacts() {
        CompletableFuture<List<Doctor>> doctorsFuture = firebaseService.getDoctorsForPatient(userContext.getUid());
        CompletableFuture<List<PharmacyProfile>> pharmaciesFuture = firebaseService.getAllPharmacies();

        return doctorsFuture.thenCombine(pharmaciesFuture, (doctors, pharmacies) -> {
            Map<String, ContactOption> contacts = new LinkedHashMap<>();
            if (doctors != null) {
                for (Doctor doctor : doctors) {
                    if (doctor != null && hasText(doctor.getUid())) {
                        contacts.put("DOCTOR:" + doctor.getUid(), new ContactOption(
                                "DOCTOR",
                                doctor.getUid(),
                                valueOrDefault(doctor.getName(), "Doctor"),
                                "Specialty: " + valueOrDefault(doctor.getSpecialty(), "Not specified")
                        ));
                    }
                }
            }
            addPharmacyContacts(contacts, pharmacies);
            return new ArrayList<>(contacts.values());
        });
    }

    private CompletableFuture<List<ContactOption>> loadDoctorContacts() {
        CompletableFuture<List<PatientProfile>> patientsFuture = firebaseService.getDoctorPatients(userContext.getUid());
        CompletableFuture<List<PharmacyProfile>> pharmaciesFuture = firebaseService.getAllPharmacies();

        return patientsFuture.thenCombine(pharmaciesFuture, (patients, pharmacies) -> {
            Map<String, ContactOption> contacts = new LinkedHashMap<>();
            if (patients != null) {
                for (PatientProfile patient : patients) {
                    if (patient != null && hasText(patient.getUid())) {
                        contacts.put("PATIENT:" + patient.getUid(), new ContactOption(
                                "PATIENT",
                                patient.getUid(),
                                valueOrDefault(patient.getName(), "Patient"),
                                "Email: " + valueOrDefault(patient.getEmail(), "Not provided")
                        ));
                    }
                }
            }
            addPharmacyContacts(contacts, pharmacies);
            return new ArrayList<>(contacts.values());
        });
    }

    private CompletableFuture<List<ContactOption>> loadPharmacyContacts() {
        String pharmacyAddress = userContext.getPharmacyProfile() == null
                ? null
                : userContext.getPharmacyProfile().getAddressNormalized();

        CompletableFuture<List<Prescription>> prescriptionsFuture = hasText(pharmacyAddress)
                ? firebaseService.getPrescriptionsForPharmacy(pharmacyAddress)
                : CompletableFuture.completedFuture(new ArrayList<>());
        CompletableFuture<List<Doctor>> doctorsFuture = firebaseService.getAllDoctors();

        return prescriptionsFuture.thenCombine(doctorsFuture, (prescriptions, doctors) -> {
            Map<String, ContactOption> contacts = new LinkedHashMap<>();
            if (prescriptions != null) {
                for (Prescription prescription : prescriptions) {
                    if (prescription != null && hasText(prescription.getPatientUid())) {
                        contacts.put("PATIENT:" + prescription.getPatientUid(), new ContactOption(
                                "PATIENT",
                                prescription.getPatientUid(),
                                valueOrDefault(prescription.getPatientName(), "Patient"),
                                "Prescription patient"
                        ));
                    }
                }
            }
            if (doctors != null) {
                for (Doctor doctor : doctors) {
                    if (doctor != null && hasText(doctor.getUid())) {
                        contacts.put("DOCTOR:" + doctor.getUid(), new ContactOption(
                                "DOCTOR",
                                doctor.getUid(),
                                valueOrDefault(doctor.getName(), "Doctor"),
                                "Specialty: " + valueOrDefault(doctor.getSpecialty(), "Not specified")
                        ));
                    }
                }
            }
            return new ArrayList<>(contacts.values());
        });
    }

    private void addPharmacyContacts(Map<String, ContactOption> contacts, List<PharmacyProfile> pharmacies) {
        if (pharmacies == null) {
            return;
        }
        for (PharmacyProfile pharmacy : pharmacies) {
            if (pharmacy != null && hasText(pharmacy.getUid())) {
                contacts.put("PHARMACY:" + pharmacy.getUid(), new ContactOption(
                        "PHARMACY",
                        pharmacy.getUid(),
                        valueOrDefault(pharmacy.getPharmacyName(), "Pharmacy"),
                        valueOrDefault(pharmacy.getFullAddress(), "Address not provided")
                ));
            }
        }
    }

    private void updateHeader() {
        if (selectedContact == null) {
            conversationTitleLabel.setText("Select a conversation");
            conversationSubtitleLabel.setText("Messages will appear here.");
            sendButton.setDisable(true);
            return;
        }

        conversationTitleLabel.setText(selectedContact.displayName);
        conversationSubtitleLabel.setText(selectedContact.roleLabel() + " | " + valueOrDefault(selectedContact.detail, ""));
        sendButton.setDisable(false);
    }

    private void loadMessages() {
        messagesVBox.getChildren().clear();
        if (selectedContact == null) {
            return;
        }

        firebaseService.getMessagesBetweenParticipants(
                        userContext.getRole(),
                        userContext.getUid(),
                        selectedContact.role,
                        selectedContact.uid
                )
                .thenAccept(messages -> Platform.runLater(() -> {
                    renderMessages(messages);
                    markAsRead();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load messages: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    @FXML
    private void onSendMessage() {
        if (selectedContact == null) {
            return;
        }

        String text = messageInputArea.getText() == null ? "" : messageInputArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        Message message = new Message();
        setParticipant(message, userContext.getRole(), userContext.getUid(), userContext.getName());
        setParticipant(message, selectedContact.role, selectedContact.uid, selectedContact.displayName);
        message.setSenderUid(userContext.getUid());
        message.setSenderName(userContext.getName());
        message.setSenderRole(userContext.getRole());
        message.setMessageText(text);
        message.setCreatedAt(System.currentTimeMillis());
        message.setRead(false);

        sendButton.setDisable(true);
        firebaseService.saveMessage(message)
                .thenRun(() -> Platform.runLater(() -> {
                    messageInputArea.clear();
                    sendButton.setDisable(false);
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

    private void markAsRead() {
        firebaseService.markMessagesAsReadBetweenParticipants(
                userContext.getRole(),
                userContext.getUid(),
                selectedContact.role,
                selectedContact.uid,
                userContext.getRole()
        );
    }

    private void renderMessages(List<Message> messages) {
        messagesVBox.getChildren().clear();
        if (messages == null) {
            messages = new ArrayList<>();
        }

        messages.sort(Comparator.comparingLong(m -> m.getCreatedAt() != null ? m.getCreatedAt() : 0));
        for (Message message : messages) {
            messagesVBox.getChildren().add(createBubble(message));
        }

        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    private VBox createBubble(Message message) {
        boolean isMe = userContext.getUid().equals(message.getSenderUid());

        VBox wrapper = new VBox();
        HBox row = new HBox();
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10));
        bubble.setMaxWidth(420);
        bubble.setStyle(isMe
                ? "-fx-background-color: #CCFBF1; -fx-background-radius: 12;"
                : "-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-background-radius: 12;");

        Label sender = new Label(valueOrDefault(message.getSenderName(), message.getSenderRole()));
        sender.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        Label text = new Label(message.getMessageText());
        text.setWrapText(true);

        Label time = new Label(formatTime(message.getCreatedAt()));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");

        bubble.getChildren().addAll(sender, text, time);
        row.getChildren().add(bubble);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private void setParticipant(Message message, String role, String uid, String name) {
        if ("PATIENT".equalsIgnoreCase(role)) {
            message.setPatientUid(uid);
            message.setPatientName(name);
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            message.setDoctorUid(uid);
            message.setDoctorName(name);
        } else if ("PHARMACY".equalsIgnoreCase(role)) {
            message.setPharmacyUid(uid);
            message.setPharmacyName(name);
        }
    }

    @FXML
    private void onClear() {
        messageInputArea.clear();
    }

    @FXML
    private void onBack() {
        if (userContext.isDoctor()) {
            SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
        } else if (userContext.isPharmacy()) {
            SceneRouter.go("pharmacy-prescriptions-view.fxml", "Pharmacy Portal");
        } else {
            SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
        }
    }

    private String formatCurrentUserName() {
        if (userContext.isDoctor()) {
            return "Dr. " + valueOrDefault(userContext.getName(), "Doctor");
        }
        return valueOrDefault(userContext.getName(), userContext.getRole());
    }

    private String contactHelpText() {
        if (userContext.isDoctor()) {
            return "Patients and pharmacies";
        }
        if (userContext.isPharmacy()) {
            return "Patients and doctors";
        }
        return "Doctors and pharmacies";
    }

    private String formatTime(Long timestamp) {
        return timestamp == null ? "" : timeFormat.format(new Date(timestamp));
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private String cleanErrorMessage(Throwable throwable) {
        Throwable cause = throwable != null && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String valueOrDefault(String value) {
        return value == null ? "" : value;
    }

    private static class ContactOption {
        private final String role;
        private final String uid;
        private final String displayName;
        private final String detail;

        private ContactOption(String role, String uid, String displayName, String detail) {
            this.role = role;
            this.uid = uid;
            this.displayName = displayName;
            this.detail = detail;
        }

        private String roleLabel() {
            if ("DOCTOR".equalsIgnoreCase(role)) {
                return "Doctor";
            }
            if ("PHARMACY".equalsIgnoreCase(role)) {
                return "Pharmacy";
            }
            return "Patient";
        }
    }
}
