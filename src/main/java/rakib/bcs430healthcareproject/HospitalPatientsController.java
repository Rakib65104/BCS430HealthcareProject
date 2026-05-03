package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class HospitalPatientsController {

    @FXML private TextField searchField;
    @FXML private Label resultsLabel;
    @FXML private VBox patientsListVBox;

    // add this in FXML after searchField later
    @FXML private ComboBox<String> departmentFilterComboBox;

    private final FirebaseService firebaseService = new FirebaseService();
    private final UserContext userContext = UserContext.getInstance();

    private List<Appointment> allHospitalAppointments = new ArrayList<>();

    @FXML
    public void initialize() {
        loadPatients();
    }

    private void loadPatients() {
        HospitalProfile hospital = userContext.getHospitalProfile();

        if (hospital == null) {
            showEmpty("No hospital is currently loaded.");
            return;
        }

        firebaseService.getAppointmentsForHospital(hospital.getUid())
                .thenAccept(appointments -> Platform.runLater(() -> {
                    allHospitalAppointments = appointments == null ? new ArrayList<>() : appointments;
                    populateDepartmentFilter();
                    renderAppointments(allHospitalAppointments);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showEmpty("Unable to load patients."));
                    return null;
                });
    }

    private void populateDepartmentFilter() {
        if (departmentFilterComboBox == null) return;

        Set<String> departments = new TreeSet<>();
        departments.add("All Departments");

        for (Appointment appointment : allHospitalAppointments) {
            if (appointment == null) continue;

            String dept = appointment.getDepartmentName();
            if (dept != null && !dept.isBlank()) {
                departments.add(dept);
            }
        }

        departmentFilterComboBox.getItems().setAll(departments);
        departmentFilterComboBox.setValue("All Departments");

        departmentFilterComboBox.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedDepartment = departmentFilterComboBox == null ? "All Departments" : departmentFilterComboBox.getValue();

        List<Appointment> filtered = new ArrayList<>();

        for (Appointment appointment : allHospitalAppointments) {
            if (appointment == null) continue;

            boolean departmentMatch = selectedDepartment == null
                    || selectedDepartment.equals("All Departments")
                    || selectedDepartment.equalsIgnoreCase(valueOrDefault(appointment.getDepartmentName(), ""));

            String patientName = valueOrDefault(appointment.getPatientName(), "").toLowerCase(Locale.ROOT);

            boolean searchMatch = query.isBlank() || patientName.contains(query);

            if (departmentMatch && searchMatch) {
                filtered.add(appointment);
            }
        }

        renderAppointments(filtered);
    }

    private void renderAppointments(List<Appointment> appointments) {
        patientsListVBox.getChildren().clear();

        if (appointments == null || appointments.isEmpty()) {
            showEmpty("No patients booked with this hospital yet.");
            return;
        }

        Map<String, Appointment> uniquePatients = new LinkedHashMap<>();

        List<Appointment> sorted = appointments.stream()
                .filter(Objects::nonNull)
                .filter(a -> !isCancelled(a))
                .sorted(Comparator.comparing(
                        Appointment::resolveAppointmentEpochMillis,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .collect(Collectors.toList());

        for (Appointment appointment : sorted) {
            if (appointment.getPatientUid() == null || appointment.getPatientUid().isBlank()) {
                continue;
            }

            uniquePatients.putIfAbsent(appointment.getPatientUid(), appointment);
        }

        resultsLabel.setText("Patient Results (" + uniquePatients.size() + ")");

        for (Appointment appointment : uniquePatients.values()) {
            patientsListVBox.getChildren().add(buildPatientCard(appointment));
        }
    }

    private VBox buildPatientCard(Appointment appointment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #D1FAE5;" +
                        "-fx-border-radius: 14;"
        );

        Label nameLabel = new Label(valueOrDefault(appointment.getPatientName(), "Unnamed Patient"));
        nameLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 16; -fx-font-weight: bold;");

        Label deptLabel = new Label("Department: " + valueOrDefault(appointment.getDepartmentName(), "General"));
        deptLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12; -fx-font-weight: bold;");

        Label doctorLabel = new Label("Authorized by Dr. " + valueOrDefault(appointment.getDoctorName(), "Unknown"));
        doctorLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label visitLabel = new Label("Visit: " + valueOrDefault(appointment.getAppointmentDate(), "") + "  " + valueOrDefault(appointment.getAppointmentSlot(), ""));
        visitLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        HBox actions = new HBox(10);

        Button viewButton = new Button("View Profile");
        viewButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8;");
        viewButton.setOnAction(event -> openPatientProfile(appointment));

        Button prescriptionButton = new Button("Send Prescription");
        prescriptionButton.setStyle("-fx-background-color: #14B8A6; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8;");
        prescriptionButton.setOnAction(event -> openPrescriptionForm(appointment));

        actions.getChildren().addAll(viewButton, prescriptionButton);

        card.getChildren().addAll(nameLabel, deptLabel, doctorLabel, visitLabel, actions);
        return card;
    }

    private void showEmpty(String text) {
        patientsListVBox.getChildren().clear();
        resultsLabel.setText("Patient Results");

        VBox card = new VBox();
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
        card.getChildren().add(label);

        patientsListVBox.getChildren().add(card);
    }

    @FXML
    private void onSearch() {
        applyFilters();
    }

    @FXML
    private void onClear() {
        searchField.clear();
        if (departmentFilterComboBox != null) {
            departmentFilterComboBox.setValue("All Departments");
        }
        renderAppointments(allHospitalAppointments);
    }

    @FXML private void onDashboard() { SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard"); }
    @FXML private void onPatients() {}
    @FXML private void onSchedule() { SceneRouter.go("hospital-schedule-view.fxml", "Hospital Schedule"); }
    @FXML private void onProfile() { SceneRouter.go("hospital-profile-view.fxml", "Hospital Profile"); }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    private boolean isCancelled(Appointment appointment) {
        return appointment.getStatus() != null && appointment.getStatus().equalsIgnoreCase("CANCELLED");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void openPatientProfile(Appointment appointment) {
        firebaseService.getPatientProfile(appointment.getPatientUid())
                .thenAccept(patient -> Platform.runLater(() -> {
                    userContext.setSelectedPatientUid(patient.getUid());
                    userContext.setSelectedPatientProfile(patient);
                    SceneRouter.go("patient-profile-view.fxml", "Patient Profile");
                }));
    }

    private void openPrescriptionForm(Appointment appointment) {
        firebaseService.getPatientProfile(appointment.getPatientUid())
                .thenAccept(patient -> Platform.runLater(() -> {
                    userContext.setSelectedPatientUid(patient.getUid());
                    userContext.setSelectedPatientProfile(patient);
                    SceneRouter.go("hospital-prescription-view.fxml", "Send Prescription");
                }));
    }
}