package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class HospitalDepartmentsController {

    @FXML private VBox departmentsListBox;

    private FirebaseService firebaseService;
    private UserContext userContext;

    private final String[] presetDepartments = {
            "Emergency",
            "Cardiology",
            "Dermatology",
            "Pediatrics",
            "Orthopedics",
            "Radiology",
            "Neurology",
            "Pharmacy",
            "Laboratory",
            "General Medicine",
            "Surgery"
    };

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        loadDepartments();
    }

    @FXML
    public void loadDepartments() {
        departmentsListBox.getChildren().clear();

        Label loading = new Label("Loading departments...");
        loading.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14;");
        departmentsListBox.getChildren().add(loading);

        String currentHospitalUid = userContext.getUid();

        if (currentHospitalUid == null || currentHospitalUid.isBlank()) {
            departmentsListBox.getChildren().clear();
            addMessage("Hospital account not found.");
            return;
        }

        firebaseService.getAllDoctors()
                .thenAccept(doctors -> Platform.runLater(() -> buildDepartmentCards(doctors, currentHospitalUid)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        departmentsListBox.getChildren().clear();
                        addMessage("Could not load departments.");
                    });
                    return null;
                });
    }

    private void buildDepartmentCards(List<Doctor> allDoctors, String currentHospitalUid) {
        departmentsListBox.getChildren().clear();

        for (String department : presetDepartments) {
            VBox card = new VBox(8);
            card.setStyle("""
                    -fx-background-color: white;
                    -fx-background-radius: 14;
                    -fx-border-color: #D1FAE5;
                    -fx-border-radius: 14;
                    -fx-padding: 16;
                    """);

            Label deptTitle = new Label(department);
            deptTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #0F766E;");

            List<Doctor> matchedDoctors = getDoctorsForDepartment(allDoctors, department, currentHospitalUid);

            Label countLabel = new Label(matchedDoctors.size() + " doctor(s) assigned");
            countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

            card.getChildren().addAll(deptTitle, countLabel);

            if (matchedDoctors.isEmpty()) {
                Label none = new Label("No doctors currently assigned to this department.");
                none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13;");
                card.getChildren().add(none);
            } else {
                for (Doctor doctor : matchedDoctors) {
                    card.getChildren().add(buildDoctorBox(doctor));
                }
            }

            departmentsListBox.getChildren().add(card);
        }
    }

    private VBox buildDoctorBox(Doctor doctor) {
        VBox doctorBox = new VBox(2);
        doctorBox.setStyle("""
                -fx-background-color: #F0FDFA;
                -fx-background-radius: 10;
                -fx-padding: 10;
                """);

        Label doctorName = new Label(valueOrDefault(doctor.getName(), "Unnamed Doctor"));
        doctorName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #134E4A;");

        Label doctorInfo = new Label(
                valueOrDefault(doctor.getClinicName(), "No Clinic")
                        + " • " +
                        valueOrDefault(doctor.getPhone(), "No Phone")
        );
        doctorInfo.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label doctorEmail = new Label(valueOrDefault(doctor.getEmail(), "No Email"));
        doctorEmail.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        doctorBox.getChildren().addAll(doctorName, doctorInfo, doctorEmail);
        return doctorBox;
    }

    private List<Doctor> getDoctorsForDepartment(List<Doctor> allDoctors, String department, String currentHospitalUid) {
        List<Doctor> matched = new ArrayList<>();

        if (allDoctors == null) {
            return matched;
        }

        for (Doctor doctor : allDoctors) {
            if (doctor == null) {
                continue;
            }

            String doctorHospitalUid = doctor.getHospitalUid();
            if (doctorHospitalUid == null || !doctorHospitalUid.equals(currentHospitalUid)) {
                continue;
            }

            String doctorDepartment = doctor.getDepartment();

            if (doctorDepartment != null && doctorDepartment.equalsIgnoreCase(department)) {
                matched.add(doctor);
                continue;
            }

            String specialty = doctor.getSpecialty();
            if (doctorDepartment == null && specialty != null) {
                String specialtyLower = specialty.trim().toLowerCase();
                String departmentLower = department.trim().toLowerCase();

                if (specialtyLower.contains(departmentLower)
                        || (departmentLower.equals("emergency") && specialtyLower.contains("emergency medicine"))) {
                    matched.add(doctor);
                }
            }
        }

        return matched;
    }

    private void addMessage(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14;");
        departmentsListBox.getChildren().add(label);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @FXML
    private void onBack() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }
}