package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class HospitalDepartmentsController {

    @FXML private VBox departmentsListBox;

    private FirebaseService firebaseService;

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
        loadDepartments();
    }

    @FXML
    public void loadDepartments() {
        departmentsListBox.getChildren().clear();

        firebaseService.getAllDoctors()
                .thenAccept(doctors -> Platform.runLater(() -> buildDepartmentCards(doctors)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Label error = new Label("Could not load departments.");
                        departmentsListBox.getChildren().add(error);
                    });
                    return null;
                });
    }

    private void buildDepartmentCards(List<Doctor> allDoctors) {
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

            List<Doctor> matchedDoctors = getDoctorsForDepartment(allDoctors, department);

            Label countLabel = new Label(matchedDoctors.size() + " doctor(s) assigned");
            countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

            card.getChildren().addAll(deptTitle, countLabel);

            if (matchedDoctors.isEmpty()) {
                Label none = new Label("No doctors currently assigned.");
                none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13;");
                card.getChildren().add(none);
            } else {
                for (Doctor doctor : matchedDoctors) {
                    VBox doctorBox = new VBox(2);
                    doctorBox.setStyle("""
                            -fx-background-color: #F0FDFA;
                            -fx-background-radius: 10;
                            -fx-padding: 10;
                            """);

                    Label doctorName = new Label(doctor.getName());
                    doctorName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #134E4A;");

                    Label doctorClinic = new Label(
                            (doctor.getClinicName() != null ? doctor.getClinicName() : "No Clinic")
                                    + " • " +
                                    (doctor.getPhone() != null ? doctor.getPhone() : "No Phone")
                    );
                    doctorClinic.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

                    doctorBox.getChildren().addAll(doctorName, doctorClinic);
                    card.getChildren().add(doctorBox);
                }
            }

            departmentsListBox.getChildren().add(card);
        }
    }

    private List<Doctor> getDoctorsForDepartment(List<Doctor> allDoctors, String department) {
        List<Doctor> matched = new ArrayList<>();

        if (allDoctors == null) {
            return matched;
        }

        for (Doctor doctor : allDoctors) {
            if (doctor == null || doctor.getSpecialty() == null) {
                continue;
            }

            String specialty = doctor.getSpecialty().trim().toLowerCase();
            String dept = department.trim().toLowerCase();

            if (specialty.contains(dept)) {
                matched.add(doctor);
            } else if (dept.equals("emergency") && specialty.contains("emergency medicine")) {
                matched.add(doctor);
            }
        }

        return matched;
    }

    @FXML
    private void onBack() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }
}