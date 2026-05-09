package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for sending a prescription to a patient's pharmacy from the hospital side.
 */
public class HospitalPrescriptionController {

    @FXML private Label patientNameLabel;
    @FXML private Label hospitalNameLabel;
    @FXML private ComboBox<PharmacyOption> pharmacySelectionComboBox;
    @FXML private TextField pharmacyNameField;
    @FXML private TextField pharmacyStreetAddressField;
    @FXML private TextField pharmacyCityField;
    @FXML private TextField pharmacyStateField;
    @FXML private TextField pharmacyZipField;
    @FXML private TextField pharmacyPhoneField;
    @FXML private TextField medicationSearchField;
    @FXML private ComboBox<RxNormMedicationService.MedicationOption> medicationResultsComboBox;
    @FXML private Button searchMedicationButton;
    @FXML private TextField medicationNameField;
    @FXML private TextField dosageField;
    @FXML private TextField quantityField;
    @FXML private TextField refillDetailsField;
    @FXML private TextField refillIntervalDaysField;
    @FXML private TextArea instructionsArea;
    @FXML private VBox pendingPrescriptionsVBox;
    @FXML private Label statusLabel;
    @FXML private Button addMedicationButton;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private RxNormMedicationService rxNormMedicationService;
    private UserContext userContext;
    private PatientProfile selectedPatient;
    private List<PharmacyProfile> availablePharmacies = new ArrayList<>();
    private final List<PrescriptionDraft> pendingPrescriptionDrafts = new ArrayList<>();

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        rxNormMedicationService = new RxNormMedicationService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isHospital()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        HospitalProfile hospital = userContext.getHospitalProfile();
        if (hospital != null) {
            hospitalNameLabel.setText(
                    hospital.getHospitalName() != null ? hospital.getHospitalName() : "Hospital"
            );
        }

        selectedPatient = userContext.getSelectedPatientProfile();
        if (selectedPatient == null) {
            showStatus("No patient selected. Please select a patient from the hospital schedule.", true);
            patientNameLabel.setText("No patient selected");
        } else {
            patientNameLabel.setText(
                    selectedPatient.getName() != null ? selectedPatient.getName() : "Patient"
            );
            loadPharmacies();
            populatePreferredPharmacy();
        }

        medicationResultsComboBox.setDisable(true);
        medicationResultsComboBox.setOnAction(event -> onMedicationSelected());
    }

    private void loadPharmacies() {
        firebaseService.getAllPharmacies()
                .thenAccept(pharmacies -> Platform.runLater(() -> {
                    availablePharmacies = pharmacies != null ? pharmacies : new ArrayList<>();
                    pharmacySelectionComboBox.getItems().clear();
                    
                    // Add a "Select a pharmacy" placeholder
                    pharmacySelectionComboBox.getItems().add(new PharmacyOption(null, "Select a pharmacy", "", ""));
                    
                    // Add all available pharmacies
                    for (PharmacyProfile pharmacy : availablePharmacies) {
                        PharmacyOption option = new PharmacyOption(
                                pharmacy.getUid(),
                                pharmacy.getPharmacyName(),
                                pharmacy.getFullAddress(),
                                pharmacy.getPhoneNumber()
                        );
                        pharmacySelectionComboBox.getItems().add(option);
                    }
                    
                    pharmacySelectionComboBox.getSelectionModel().selectFirst();
                    pharmacySelectionComboBox.setOnAction(event -> onPharmacySelected());
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load pharmacies: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void onPharmacySelected() {
        PharmacyOption selectedOption = pharmacySelectionComboBox.getValue();
        
        if (selectedOption == null || selectedOption.uid == null || selectedOption.uid.isBlank()) {
            // Clear the fields if placeholder is selected
            pharmacyNameField.clear();
            pharmacyStreetAddressField.clear();
            pharmacyCityField.clear();
            pharmacyStateField.clear();
            pharmacyZipField.clear();
            pharmacyPhoneField.clear();
            return;
        }
        
        // Find the selected pharmacy and populate the fields
        PharmacyProfile selectedPharmacy = availablePharmacies.stream()
                .filter(p -> selectedOption.uid.equals(p.getUid()))
                .findFirst()
                .orElse(null);
        
        if (selectedPharmacy != null) {
            pharmacyNameField.setText(selectedPharmacy.getPharmacyName() != null ? selectedPharmacy.getPharmacyName() : "");
            pharmacyStreetAddressField.setText(selectedPharmacy.getAddressLine() != null ? selectedPharmacy.getAddressLine() : "");
            pharmacyCityField.setText(selectedPharmacy.getCity() != null ? selectedPharmacy.getCity() : "");
            pharmacyStateField.setText(selectedPharmacy.getState() != null ? selectedPharmacy.getState() : "");
            pharmacyZipField.setText(selectedPharmacy.getZip() != null ? selectedPharmacy.getZip() : "");
            pharmacyPhoneField.setText(selectedPharmacy.getPhoneNumber() != null ? selectedPharmacy.getPhoneNumber() : "");
        }
    }

    @FXML
    private void onSendPrescription() {
        if (selectedPatient == null) {
            showStatus("No patient selected. Please select a patient from the hospital schedule.", true);
            return;
        }

        String pharmacyValidation = validatePharmacyFields();
        if (pharmacyValidation != null) {
            showStatus(pharmacyValidation, true);
            return;
        }

        PrescriptionDraft currentDraft = collectCurrentDraft();
        boolean hasCurrentDraft = currentDraft.hasAnyMedicationData();
        if (hasCurrentDraft) {
            String currentValidation = currentDraft.validate();
            if (currentValidation != null) {
                showStatus(currentValidation, true);
                return;
            }
        }

        List<PrescriptionDraft> draftsToSend = new ArrayList<>(pendingPrescriptionDrafts);
        if (hasCurrentDraft) {
            draftsToSend.add(currentDraft);
        }

        if (draftsToSend.isEmpty()) {
            showStatus("Add at least one medication before sending.", true);
            return;
        }

        String pharmacyName = safeTrim(pharmacyNameField.getText());
        String pharmacyStreetAddress = safeTrim(pharmacyStreetAddressField.getText());
        String pharmacyCity = safeTrim(pharmacyCityField.getText());
        String pharmacyState = safeTrim(pharmacyStateField.getText()).toUpperCase();
        String pharmacyZip = safeTrim(pharmacyZipField.getText());
        String pharmacyAddress = PharmacyProfile.buildFullAddress(
                pharmacyStreetAddress,
                pharmacyCity,
                pharmacyState,
                pharmacyZip
        );
        String pharmacyPhone = safeTrim(pharmacyPhoneField.getText());

        HospitalProfile hospital = userContext.getHospitalProfile();
        String hospitalUid = hospital != null ? hospital.getUid() : null;
        String hospitalName = hospital != null ? hospital.getHospitalName() : "Hospital";

        sendButton.setDisable(true);
        addMedicationButton.setDisable(true);
        showStatus("Sending " + draftsToSend.size() + " prescription(s)...", false);

        List<CompletableFuture<String>> saves = draftsToSend.stream()
                .map(draft -> firebaseService.savePrescription(buildPrescription(
                        draft,
                        hospitalUid,
                        hospitalName,
                        pharmacyName,
                        pharmacyAddress,
                        pharmacyPhone
                )))
                .toList();

        CompletableFuture.allOf(saves.toArray(new CompletableFuture[0]))
                .thenRun(() -> Platform.runLater(() -> {
                    showStatus(draftsToSend.size() + " prescription(s) sent successfully.", false);
                    sendButton.setDisable(false);
                    addMedicationButton.setDisable(false);
                    pendingPrescriptionDrafts.clear();
                    renderPendingPrescriptions();
                    clearForm();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showStatus("Failed to send prescription: " + cleanErrorMessage(e), true);
                        sendButton.setDisable(false);
                        addMedicationButton.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void onAddMedicationToBatch() {
        PrescriptionDraft draft = collectCurrentDraft();
        String validation = draft.validate();
        if (validation != null) {
            showStatus(validation, true);
            return;
        }

        pendingPrescriptionDrafts.add(draft);
        renderPendingPrescriptions();
        clearMedicationFields();
        showStatus("Added " + draft.medicationName + ". Add another medication or send the list.", false);
    }

    @FXML
    private void onSearchMedications() {
        String query = safeTrim(medicationSearchField.getText());
        if (query.length() < 2) {
            showStatus("Enter at least 2 characters to search medications.", true);
            return;
        }

        searchMedicationButton.setDisable(true);
        medicationResultsComboBox.setDisable(true);
        medicationResultsComboBox.getItems().clear();
        showStatus("Searching RxNorm medications...", false);

        rxNormMedicationService.searchMedications(query)
                .thenAccept(results -> Platform.runLater(() -> showMedicationResults(results)))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        searchMedicationButton.setDisable(false);
                        medicationResultsComboBox.setDisable(false);
                        showStatus("Medication search failed: " + cleanErrorMessage(e), true);
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("hospital-schedule-view.fxml", "Hospital Schedule");
    }

    private void clearForm() {
        pharmacySelectionComboBox.getSelectionModel().selectFirst();
        pharmacyNameField.clear();
        pharmacyStreetAddressField.clear();
        pharmacyCityField.clear();
        pharmacyStateField.clear();
        pharmacyZipField.clear();
        pharmacyPhoneField.clear();
        medicationSearchField.clear();
        medicationResultsComboBox.getItems().clear();
        medicationResultsComboBox.setDisable(true);
        pendingPrescriptionDrafts.clear();
        renderPendingPrescriptions();
        clearMedicationFields();
        populatePreferredPharmacy();
    }

    private void clearMedicationFields() {
        medicationSearchField.clear();
        medicationResultsComboBox.getItems().clear();
        medicationResultsComboBox.setDisable(true);
        medicationNameField.clear();
        dosageField.clear();
        quantityField.clear();
        refillDetailsField.clear();
        refillIntervalDaysField.clear();
        instructionsArea.clear();
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

    private void populatePreferredPharmacy() {
        if (selectedPatient == null) {
            return;
        }

        if (!safeTrim(selectedPatient.getPreferredPharmacyName()).isBlank()) {
            pharmacyNameField.setText(selectedPatient.getPreferredPharmacyName());
        }

        String fullAddress = safeTrim(selectedPatient.getPreferredPharmacyAddress());
        if (!fullAddress.isBlank()) {
            String[] parts = fullAddress.split(",");

            if (parts.length >= 1) {
                pharmacyStreetAddressField.setText(parts[0].trim());
            }

            if (parts.length >= 2) {
                pharmacyCityField.setText(parts[1].trim());
            }

            if (parts.length >= 3) {
                String stateZip = parts[2].trim();

                String[] stateZipParts = stateZip.split("\\s+");
                if (stateZipParts.length >= 1) {
                    pharmacyStateField.setText(stateZipParts[0].trim());
                }
                if (stateZipParts.length >= 2) {
                    pharmacyZipField.setText(stateZipParts[1].trim());
                }
            }
        }

        if (!safeTrim(selectedPatient.getPreferredPharmacyPhoneNumber()).isBlank()) {
            pharmacyPhoneField.setText(selectedPatient.getPreferredPharmacyPhoneNumber());
        }
    }

    private String buildMedicationInformation(String medicationName, String dosage, String quantity, String refillDetails) {
        return "Medication Name: " + medicationName
                + " | Dosage: " + dosage
                + " | Quantity: " + quantity
                + " | Refills: " + refillDetails;
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }

    private void showMedicationResults(List<RxNormMedicationService.MedicationOption> results) {
        searchMedicationButton.setDisable(false);

        if (results == null || results.isEmpty()) {
            medicationResultsComboBox.getItems().clear();
            medicationResultsComboBox.setDisable(true);
            showStatus("No RxNorm medications matched that search.", true);
            return;
        }

        medicationResultsComboBox.getItems().setAll(results);
        medicationResultsComboBox.setDisable(false);
        medicationResultsComboBox.getSelectionModel().selectFirst();
        onMedicationSelected();
        showStatus("Loaded " + results.size() + " medication option(s) from RxNorm.", false);
    }

    private void onMedicationSelected() {
        RxNormMedicationService.MedicationOption selectedMedication = medicationResultsComboBox.getValue();
        if (selectedMedication != null) {
            medicationNameField.setText(selectedMedication.getDisplayName());
        }
    }

    private Integer parsePositiveInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String validatePharmacyFields() {
        String pharmacyName = safeTrim(pharmacyNameField.getText());
        String pharmacyStreetAddress = safeTrim(pharmacyStreetAddressField.getText());
        String pharmacyCity = safeTrim(pharmacyCityField.getText());
        String pharmacyState = safeTrim(pharmacyStateField.getText()).toUpperCase();
        String pharmacyZip = safeTrim(pharmacyZipField.getText());
        String pharmacyAddress = PharmacyProfile.buildFullAddress(pharmacyStreetAddress, pharmacyCity, pharmacyState, pharmacyZip);
        String pharmacyPhone = safeTrim(pharmacyPhoneField.getText());

        if (pharmacyName.isBlank()) return "Pharmacy name is required.";
        if (pharmacyStreetAddress.isBlank() || pharmacyCity.isBlank() || pharmacyState.isBlank() || pharmacyZip.isBlank()) {
            return "Street address, city, state, and ZIP are required.";
        }
        if (pharmacyState.length() != 2) return "State must be a 2-letter abbreviation.";
        if (!pharmacyZip.matches("\\d{5}")) return "ZIP code must be 5 digits.";
        if (pharmacyAddress.isBlank()) return "Pharmacy address is required.";
        if (pharmacyPhone.isBlank()) return "Pharmacy phone number is required.";
        return null;
    }

    private PrescriptionDraft collectCurrentDraft() {
        return new PrescriptionDraft(
                safeTrim(medicationNameField.getText()),
                safeTrim(dosageField.getText()),
                safeTrim(quantityField.getText()),
                safeTrim(refillDetailsField.getText()),
                safeTrim(refillIntervalDaysField.getText()),
                safeTrim(instructionsArea.getText())
        );
    }

    private Prescription buildPrescription(PrescriptionDraft draft,
                                           String hospitalUid,
                                           String hospitalName,
                                           String pharmacyName,
                                           String pharmacyAddress,
                                           String pharmacyPhone) {
        Prescription prescription = new Prescription();
        prescription.setHospitalUid(hospitalUid);
        prescription.setHospitalName(hospitalName);
        prescription.setPatientUid(selectedPatient.getUid());
        prescription.setPatientName(selectedPatient.getName());
        prescription.setPharmacyName(pharmacyName);
        prescription.setPharmacyAddress(pharmacyAddress);
        prescription.setPharmacyAddressNormalized(AddressNormalizer.normalize(pharmacyAddress));
        prescription.setPharmacyPhoneNumber(pharmacyPhone);
        prescription.setMedicationName(draft.medicationName);
        prescription.setDosage(draft.dosage);
        prescription.setQuantity(draft.quantity);
        prescription.setRefillDetails(PrescriptionRefillSupport.formatRemainingRefills(draft.remainingRefills()));
        prescription.setRemainingRefills(draft.remainingRefills());
        prescription.setRefillIntervalDays(draft.refillIntervalDays());
        prescription.setMedicationInformation(buildMedicationInformation(draft.medicationName, draft.dosage, draft.quantity, draft.refillDetails));
        prescription.setInstructions(draft.instructions);
        return prescription;
    }

    private void renderPendingPrescriptions() {
        pendingPrescriptionsVBox.getChildren().clear();
        for (int i = 0; i < pendingPrescriptionDrafts.size(); i++) {
            PrescriptionDraft draft = pendingPrescriptionDrafts.get(i);
            int index = i;
            Label label = new Label((i + 1) + ". " + draft.medicationName + " | " + draft.dosage + " | Qty: " + draft.quantity);
            label.setWrapText(true);
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(event -> {
                pendingPrescriptionDrafts.remove(index);
                renderPendingPrescriptions();
            });
            HBox row = new HBox(10, label, removeButton);
            row.setStyle("-fx-alignment: center-left; -fx-padding: 8; -fx-background-color: #F8FAFC; -fx-background-radius: 8;");
            pendingPrescriptionsVBox.getChildren().add(row);
        }
    }

    private class PrescriptionDraft {
        private final String medicationName;
        private final String dosage;
        private final String quantity;
        private final String refillDetails;
        private final String refillIntervalText;
        private final String instructions;

        private PrescriptionDraft(String medicationName, String dosage, String quantity, String refillDetails, String refillIntervalText, String instructions) {
            this.medicationName = medicationName;
            this.dosage = dosage;
            this.quantity = quantity;
            this.refillDetails = refillDetails;
            this.refillIntervalText = refillIntervalText;
            this.instructions = instructions;
        }

        private boolean hasAnyMedicationData() {
            return !medicationName.isBlank() || !dosage.isBlank() || !quantity.isBlank()
                    || !refillDetails.isBlank() || !refillIntervalText.isBlank() || !instructions.isBlank();
        }

        private String validate() {
            if (medicationName.isBlank()) return "Medication name is required.";
            if (dosage.isBlank()) return "Dosage is required.";
            if (quantity.isBlank()) return "Quantity is required.";
            if (refillDetails.isBlank()) return "Refill details are required.";
            if (remainingRefills() == null) return "Refill details must include a count, like '2 refills remaining' or 'No refills remaining'.";
            if (refillIntervalText.isBlank()) return "Refill interval is required.";
            if (refillIntervalDays() == null) return "Refill interval must be a whole number of days, like 30.";
            if (instructions.isBlank()) return "Instructions are required.";
            return null;
        }

        private Integer remainingRefills() {
            return PrescriptionRefillSupport.parseRemainingRefills(refillDetails);
        }

        private Integer refillIntervalDays() {
            return parsePositiveInteger(refillIntervalText);
        }
    }

    private static class PharmacyOption {
        private final String uid;
        private final String name;
        private final String address;
        private final String phoneNumber;

        private PharmacyOption(String uid, String name, String address, String phoneNumber) {
            this.uid = uid;
            this.name = name;
            this.address = address;
            this.phoneNumber = phoneNumber;
        }

        @Override
        public String toString() {
            if (uid == null || uid.isBlank()) {
                return name;
            }
            return name + " | " + address + " | " + phoneNumber;
        }
    }
}
