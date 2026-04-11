package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DoctorSearchController {

    @FXML private TextField nameSearchField;
    @FXML private TextField specialtySearchField;
    @FXML private TextField zipSearchField;
    @FXML private Button searchButton;
    @FXML private Button clearFiltersButton;
    @FXML private VBox doctorListVBox;
    @FXML private Label statusLabel;
    @FXML private Button backButton;
    @FXML private WebView mapWebView;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private List<Doctor> allDoctors = new ArrayList<>();
    private List<Doctor> filteredDoctors = new ArrayList<>();

    private boolean mapLoaded = false;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        setupUI();
        loadMap();
        loadDoctors();
    }

    private void setupUI() {
        nameSearchField.setPromptText("Doctor name...");
        specialtySearchField.setPromptText("e.g., Cardiology, Family Medicine...");
        zipSearchField.setPromptText("e.g., 11735");

        nameSearchField.textProperty().addListener((obs, o, n) -> applyFilters());
        specialtySearchField.textProperty().addListener((obs, o, n) -> applyFilters());
        zipSearchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    /**
     * Load Leaflet map inside WebView.
     */
    private void loadMap() {
        try {
            WebEngine engine = mapWebView.getEngine();

            // Use a modern browser user agent so tile servers accept requests
            engine.setUserAgent(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"
            );

            engine.setOnError(e -> System.out.println("WebView error: " + e.getMessage()));
            engine.setOnAlert(e -> System.out.println("JS alert: " + e.getData()));

            String mapUrl = getClass()
                    .getResource("/rakib/bcs430healthcareproject/map.html")
                    .toExternalForm();

            engine.load(mapUrl);

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    mapLoaded = true;
                    System.out.println("Map loaded.");
                    // Ensure the WebView is properly laid out
                    Platform.runLater(() -> mapWebView.requestLayout());
                    // Expose the controller to JavaScript
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaController", this);
                    if (!filteredDoctors.isEmpty()) {
                        updateMap(filteredDoctors);
                    }
                } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                    System.out.println("Map failed to load.");
                }
            });
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load doctors from Firebase.
     */
    private void loadDoctors() {
        showStatus("Loading doctors...", false);

        firebaseService.getAllDoctors()
                .thenAccept(doctors -> Platform.runLater(() -> {
                    allDoctors = doctors;
                    filteredDoctors = new ArrayList<>(doctors);
                    displayDoctors(filteredDoctors);
                    showStatus("Found " + doctors.size() + " doctors", false);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Error loading doctors", true));
                    return null;
                });
    }

    @FXML
    private void onSearch() {
        applyFilters();
    }

    @FXML
    private void onClearFilters() {
        nameSearchField.clear();
        specialtySearchField.clear();
        zipSearchField.clear();
        filteredDoctors = new ArrayList<>(allDoctors);
        displayDoctors(filteredDoctors);
        showStatus("Filters cleared. Showing " + filteredDoctors.size() + " doctors", false);
    }

    /**
     * Filter doctors.
     */
    private void applyFilters() {
        String name = nameSearchField.getText() != null
                ? nameSearchField.getText().trim().toLowerCase()
                : "";
        String specialty = specialtySearchField.getText() != null
                ? specialtySearchField.getText().trim().toLowerCase()
                : "";
        String zip = zipSearchField.getText() != null
                ? zipSearchField.getText().trim()
                : "";

        filteredDoctors = new ArrayList<>();

        for (Doctor doctor : allDoctors) {
            boolean matchesName =
                    name.isEmpty() ||
                    (doctor.getName() != null && doctor.getName().toLowerCase().contains(name));

            boolean matchesSpecialty =
                    specialty.isEmpty() ||
                    (doctor.getSpecialty() != null && doctor.getSpecialty().toLowerCase().contains(specialty));

            boolean matchesZip =
                    zip.isEmpty() ||
                    (doctor.getZip() != null && zip.equals(doctor.getZip()));

            if (matchesName && matchesSpecialty && matchesZip) {
                filteredDoctors.add(doctor);
            }
        }

        displayDoctors(filteredDoctors);
        showStatus("Found " + filteredDoctors.size() + " doctors", false);
    }

    /**
     * Display doctor cards.
     */
    private void displayDoctors(List<Doctor> doctors) {
        doctorListVBox.getChildren().clear();

        if (doctors.isEmpty()) {
            Label label = new Label("No doctors found.");
            doctorListVBox.getChildren().add(label);
            updateMap(allDoctors); // Show all doctors on map even when no filtered results
            return;
        }

        for (Doctor doctor : doctors) {
            VBox card = new VBox(8);
            card.setStyle(
                    "-fx-border-color: #E8E8E8; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-background-color: white; " +
                    "-fx-padding: 15; " +
                    "-fx-spacing: 6;"
            );

            // Doctor name and specialty (top of card)
            Label nameLabel = new Label(
                    Optional.ofNullable(doctor.getName()).orElse("doctor"));
            nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            Label specialtyLabel = new Label(
                    Optional.ofNullable(doctor.getSpecialty()).orElse("Cardiology"));
            specialtyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7F8C8D;");

            // Clinic and location section
            String clinicText = "Clinic: " +
                    Optional.ofNullable(doctor.getClinicName()).orElse("Not specified");
            Label clinicLabel = new Label(clinicText);
            clinicLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495E;");

            String city = Optional.ofNullable(doctor.getCity()).orElse("Unknown");
            String state = Optional.ofNullable(doctor.getState()).orElse("Unknown");
            String zip = Optional.ofNullable(doctor.getZip()).orElse("");
            String location = "Location: " + city + ", " + state +
                    (zip.isBlank() ? "" : " " + zip);
            Label locationLabel = new Label(location);
            locationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495E;");

            Label insuranceLabel = new Label(buildInsuranceLabel(doctor));
            insuranceLabel.setWrapText(true);
            insuranceLabel.setStyle(getInsuranceLabelStyle(doctor));

            // Accepting new patients status
            boolean accepting = Boolean.TRUE.equals(doctor.getAcceptingNewPatients());
            Label acceptingLabel = new Label(
                    accepting ? "✓ Accepting new patients" : "Not accepting new patients");
            acceptingLabel.setStyle(
                    accepting
                            ? "-fx-font-size: 11; -fx-text-fill: #27AE60; -fx-font-weight: bold;"
                            : "-fx-font-size: 11; -fx-text-fill: #E74C3C;");

            // Action buttons
            Button viewProfileButton = new Button("View Profile");
            viewProfileButton.setStyle(
                    "-fx-padding: 8 18; " +
                    "-fx-background-radius: 20; " +
                    "-fx-font-size: 12; " +
                    "-fx-background-color: #3498DB; " +
                    "-fx-text-fill: white; " +
                    "-fx-cursor: hand;");
            viewProfileButton.setOnAction(e -> viewDoctorProfile(doctor));

            Button bookAppointmentButton = new Button("Book Appointment");
            bookAppointmentButton.setStyle(
                    "-fx-padding: 8 18; " +
                    "-fx-background-radius: 20; " +
                    "-fx-font-size: 12; " +
                    "-fx-background-color: #27AE60; " +
                    "-fx-text-fill: white; " +
                    "-fx-cursor: hand;");
            bookAppointmentButton.setOnAction(e -> bookAppointment(doctor));

            HBox buttonRow = new HBox(10, viewProfileButton, bookAppointmentButton);
            buttonRow.setStyle("-fx-alignment: center-left; -fx-padding: 5 0 0 0;");

            card.getChildren().addAll(
                    nameLabel,
                    specialtyLabel,
                    clinicLabel,
                    locationLabel,
                    insuranceLabel,
                    acceptingLabel,
                    buttonRow
            );

            doctorListVBox.getChildren().add(card);
        }

        updateMap(allDoctors); // Always show all doctors on the map
    }

    /**
     * Update Leaflet markers by calling functions defined in map.html.
     */
    private void updateMap(List<Doctor> doctors) {
        if (!mapLoaded) {
            System.out.println("Map not loaded yet, skipping updateMap");
            return;
        }

        System.out.println("Updating map with " + doctors.size() + " doctors");

        Platform.runLater(() -> {
            try {
                WebEngine engine = mapWebView.getEngine();

                // clear existing markers
                engine.executeScript("if (typeof clearMarkers === 'function') clearMarkers();");

                for (Doctor doctor : doctors) {
                    String uid = doctor.getUid();
                    String name = Optional.ofNullable(doctor.getName()).orElse("Doctor");
                    String infoHtml = buildInfoHtml(doctor);
                    String address = buildAddress(doctor);

                    Double lat = doctor.getLatitude();
                    Double lon = doctor.getLongitude();

                    System.out.println("Adding marker for doctor: " + name + ", address: '" + address + "', lat: " + lat + ", lon: " + lon);

                    String latStr = (lat != null) ? lat.toString() : "null";
                    String lonStr = (lon != null) ? lon.toString() : "null";

                    String script = String.format(
                            "if (typeof addMarker === 'function') addMarker('%s','%s','%s','%s',%s,%s);",
                            escapeForJs(address),
                            escapeForJs(name),
                            escapeForJs(infoHtml),
                            escapeForJs(uid),
                            latStr,
                            lonStr
                    );

                    engine.executeScript(script);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String buildAddress(Doctor doctor) {
        String line1 = Optional.ofNullable(doctor.getAddress()).orElse("");
        String city = Optional.ofNullable(doctor.getCity()).orElse("");
        String state = Optional.ofNullable(doctor.getState()).orElse("");
        String zip = Optional.ofNullable(doctor.getZip()).orElse("");

        StringBuilder sb = new StringBuilder();
        if (!line1.isBlank()) sb.append(line1);
        if (!city.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (!state.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (!zip.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(zip);
        }

        // If still empty, try to use just city and state
        if (sb.length() == 0 && !city.isBlank() && !state.isBlank()) {
            sb.append(city).append(", ").append(state);
        }

        return sb.toString();
    }

    private String buildInfoHtml(Doctor doctor) {
        StringBuilder sb = new StringBuilder();
        String specialty = Optional.ofNullable(doctor.getSpecialty()).orElse("");
        String zip = Optional.ofNullable(doctor.getZip()).orElse("");
        String clinic = Optional.ofNullable(doctor.getClinicName()).orElse("");
        String phone = Optional.ofNullable(doctor.getPhone()).orElse("");

        if (!specialty.isBlank()) {
            sb.append("<div>Specialty: ").append(specialty).append("</div>");
        }
        if (!zip.isBlank()) {
            sb.append("<div>Zip: ").append(zip).append("</div>");
        }
        if (!clinic.isBlank()) {
            sb.append("<div>Clinic: ").append(clinic).append("</div>");
        }
        if (!phone.isBlank()) {
            sb.append("<div>Phone: ").append(phone).append("</div>");
        }
        String insuranceText = buildInsuranceLabel(doctor);
        if (!insuranceText.isBlank()) {
            sb.append("<div>").append(insuranceText).append("</div>");
        }

        return sb.toString();
    }

    private String escapeForJs(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private void viewDoctorProfile(Doctor doctor) {
        UserContext.getInstance().setSelectedDoctor(doctor);
        SceneRouter.go("doctor-profile-view.fxml", "Doctor Profile");
    }

    private void bookAppointment(Doctor doctor) {
        UserContext.getInstance().setSelectedDoctor(doctor);
        SceneRouter.go("book-appointment-view.fxml", "Book Appointment");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        if (error) {
            statusLabel.setStyle("-fx-text-fill:red");
        } else {
            statusLabel.setStyle("-fx-text-fill:green");
        }
    }

    /**
     * Called from JavaScript to open doctor profile.
     */
    public void openDoctorProfile(String uid) {
        Platform.runLater(() -> {
            Doctor doctor = allDoctors.stream()
                    .filter(d -> uid != null && uid.equals(d.getUid()))
                    .findFirst()
                    .orElse(null);
            if (doctor != null) {
                viewDoctorProfile(doctor);
            } else {
                showStatus("Doctor not found", true);
            }
        });
    }

    private String buildInsuranceLabel(Doctor doctor) {
        List<String> acceptedInsurance = parseInsuranceInfo(doctor != null ? doctor.getInsuranceInfo() : null);
        if (acceptedInsurance.isEmpty()) {
            return "Insurance: Not listed";
        }

        String patientInsurance = getPatientInsurance();
        if (patientInsurance == null || patientInsurance.isBlank()) {
            return "Insurance accepted: " + String.join(", ", acceptedInsurance);
        }

        boolean acceptsPatientInsurance = acceptedInsurance.stream()
                .anyMatch(insurance -> insurance.equalsIgnoreCase(patientInsurance));

        if (acceptsPatientInsurance) {
            return "Accepts your insurance: " + patientInsurance;
        }

        return "Does not list your insurance (" + patientInsurance + "). Accepts: " + String.join(", ", acceptedInsurance);
    }

    private String getInsuranceLabelStyle(Doctor doctor) {
        String patientInsurance = getPatientInsurance();
        if (patientInsurance == null || patientInsurance.isBlank()) {
            return "-fx-font-size: 11; -fx-text-fill: #34495E;";
        }

        boolean acceptsPatientInsurance = parseInsuranceInfo(doctor != null ? doctor.getInsuranceInfo() : null)
                .stream()
                .anyMatch(insurance -> insurance.equalsIgnoreCase(patientInsurance));

        return acceptsPatientInsurance
                ? "-fx-font-size: 11; -fx-text-fill: #166534; -fx-font-weight: bold;"
                : "-fx-font-size: 11; -fx-text-fill: #B45309; -fx-font-weight: bold;";
    }

    private String getPatientInsurance() {
        if (userContext == null || !userContext.isPatient() || userContext.getProfile() == null) {
            return null;
        }

        String insuranceCompany = userContext.getProfile().getInsuranceCompany();
        return insuranceCompany == null || insuranceCompany.isBlank() ? null : insuranceCompany.trim();
    }

    private List<String> parseInsuranceInfo(String insuranceInfo) {
        if (insuranceInfo == null || insuranceInfo.isBlank()) {
            return List.of();
        }

        return Arrays.stream(insuranceInfo.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
