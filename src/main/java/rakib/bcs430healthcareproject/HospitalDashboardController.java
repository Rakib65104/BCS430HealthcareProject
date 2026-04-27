package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HospitalDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label hospitalInfoLabel;

    @FXML private Label totalPatientsLabel;
    @FXML private Label appointmentsTodayLabel;
    @FXML private Label departmentsLabel;

    @FXML private VBox patientsListVBox;
    @FXML private VBox scheduleListVBox;

    private UserContext userContext;
    private FirebaseService firebaseService;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @FXML
    public void initialize() {
        userContext = UserContext.getInstance();
        firebaseService = new FirebaseService();

        HospitalProfile profile = userContext.getHospitalProfile();

        if (profile != null) {
            setLabelText(welcomeLabel, "Welcome, " + valueOrDefault(profile.getHospitalName(), "Hospital"));
            setLabelText(hospitalInfoLabel, valueOrDefault(profile.getFullAddress(), "Manage patients and appointments."));
            loadDashboardData(profile.getUid());
        } else {
            setLabelText(welcomeLabel, "Welcome, Hospital");
            setLabelText(hospitalInfoLabel, "Manage patients and appointments.");
            loadEmptyState();
        }
    }

    private void loadDashboardData(String hospitalUid) {
        firebaseService.getAppointmentsForHospital(hospitalUid)
                .thenCombine(firebaseService.getPatientsForHospital(hospitalUid), (appointments, patients) -> {
                    List<Doctor> hospitalDoctors;
                    try {
                        hospitalDoctors = getDoctorsForHospital(hospitalUid);
                    } catch (Exception e) {
                        hospitalDoctors = new ArrayList<>();
                    }
                    return new DashboardData(
                            appointments == null ? new ArrayList<>() : appointments,
                            patients == null ? new ArrayList<>() : patients,
                            hospitalDoctors
                    );
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    updateStats(data.patients, data.appointments, data.doctors);
                    loadPatientsPreview(data.patients);
                    loadSchedulePreview(data.appointments);
                }))
                .exceptionally(e -> {
                    Platform.runLater(this::loadEmptyState);
                    return null;
                });
    }

    private List<Doctor> getDoctorsForHospital(String hospitalUid) throws Exception {
        List<Doctor> allDoctors = firebaseService.getAllDoctors().get();
        List<Doctor> hospitalDoctors = new ArrayList<>();

        if (allDoctors == null) {
            return hospitalDoctors;
        }

        for (Doctor doctor : allDoctors) {
            if (doctor != null
                    && doctor.getHospitalUid() != null
                    && doctor.getHospitalUid().equals(hospitalUid)) {
                hospitalDoctors.add(doctor);
            }
        }

        return hospitalDoctors;
    }

    private void updateStats(List<PatientProfile> patients, List<Appointment> appointments, List<Doctor> hospitalDoctors) {
        setLabelText(totalPatientsLabel, String.valueOf(patients != null ? patients.size() : 0));

        int todayCount = 0;

        if (appointments != null) {
            for (Appointment appointment : appointments) {
                if (appointment != null && isToday(appointment) && !isCancelled(appointment)) {
                    todayCount++;
                }
            }
        }

        setLabelText(appointmentsTodayLabel, String.valueOf(todayCount));
        setLabelText(departmentsLabel, String.valueOf(countRealDepartments(hospitalDoctors)));
    }

    private int countRealDepartments(List<Doctor> hospitalDoctors) {
        Set<String> departments = new HashSet<>();

        if (hospitalDoctors == null) {
            return 0;
        }

        for (Doctor doctor : hospitalDoctors) {
            if (doctor == null) {
                continue;
            }

            String department = doctor.getDepartment();

            if (department != null && !department.isBlank()) {
                departments.add(department.trim().toLowerCase());
            }
        }

        return departments.size();
    }

    private void loadPatientsPreview(List<PatientProfile> patients) {
        if (patientsListVBox == null) return;

        patientsListVBox.getChildren().clear();

        if (patients == null || patients.isEmpty()) {
            patientsListVBox.getChildren().add(buildEmptyCard("No patients found yet."));
            return;
        }

        int limit = Math.min(5, patients.size());
        for (int i = 0; i < limit; i++) {
            patientsListVBox.getChildren().add(buildPatientCard(patients.get(i)));
        }
    }

    private void loadSchedulePreview(List<Appointment> appointments) {
        if (scheduleListVBox == null) return;

        scheduleListVBox.getChildren().clear();

        if (appointments == null || appointments.isEmpty()) {
            scheduleListVBox.getChildren().add(buildEmptyCard("No appointments scheduled."));
            return;
        }

        List<Appointment> upcomingAppointments = appointments.stream()
                .filter(Objects::nonNull)
                .filter(appointment -> !isCancelled(appointment))
                .sorted(Comparator.comparing(
                        Appointment::resolveAppointmentEpochMillis,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .collect(Collectors.toList());

        int shown = 0;
        for (Appointment appointment : upcomingAppointments) {
            scheduleListVBox.getChildren().add(buildAppointmentCard(appointment));
            shown++;

            if (shown >= 5) break;
        }

        if (shown == 0) {
            scheduleListVBox.getChildren().add(buildEmptyCard("No active appointments scheduled."));
        }
    }

    private VBox buildPatientCard(PatientProfile patient) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12; -fx-border-color: #D1FAE5; -fx-border-radius: 12;");

        Label nameLabel = new Label(valueOrDefault(patient.getName(), "Unnamed Patient"));
        nameLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 14; -fx-font-weight: bold;");

        Label emailLabel = new Label(valueOrDefault(patient.getEmail(), "No email"));
        emailLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label phoneLabel = new Label(valueOrDefault(patient.getPhoneNumber(), "No phone"));
        phoneLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(nameLabel, emailLabel, phoneLabel);
        return card;
    }

    private VBox buildAppointmentCard(Appointment appointment) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #ECFDF5; -fx-background-radius: 12; -fx-border-color: #A7F3D0; -fx-border-radius: 12;");

        Label patientLabel = new Label(valueOrDefault(appointment.getPatientName(), "Unknown Patient"));
        patientLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 14; -fx-font-weight: bold;");

        Label timeLabel = new Label(formatAppointmentTime(appointment));
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label statusLabel = new Label(valueOrDefault(appointment.getStatus(), "SCHEDULED"));
        statusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 11; -fx-font-weight: bold;");

        card.getChildren().addAll(patientLabel, timeLabel, statusLabel);
        return card;
    }

    private VBox buildEmptyCard(String text) {
        VBox box = new VBox();
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
        box.getChildren().add(label);

        return box;
    }

    private String formatAppointmentTime(Appointment appointment) {
        try {
            Long millis = appointment.resolveAppointmentEpochMillis();

            if (millis == null) {
                if (appointment.getAppointmentDate() != null && appointment.getAppointmentSlot() != null) {
                    return appointment.getAppointmentDate() + " • " + appointment.getAppointmentSlot();
                }
                return valueOrDefault(appointment.getAppointmentTime(), "Time not available");
            }

            return Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
        } catch (Exception e) {
            return valueOrDefault(appointment.getAppointmentTime(), "Time not available");
        }
    }

    private boolean isToday(Appointment appointment) {
        try {
            Long millis = appointment.resolveAppointmentEpochMillis();
            if (millis == null) return false;

            LocalDate appointmentDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            return appointmentDate.equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCancelled(Appointment appointment) {
        return appointment.getStatus() != null && appointment.getStatus().equalsIgnoreCase("CANCELLED");
    }

    private void loadEmptyState() {
        setLabelText(totalPatientsLabel, "0");
        setLabelText(appointmentsTodayLabel, "0");
        setLabelText(departmentsLabel, "0");

        if (patientsListVBox != null) {
            patientsListVBox.getChildren().clear();
            patientsListVBox.getChildren().add(buildEmptyCard("No patients found yet."));
        }

        if (scheduleListVBox != null) {
            scheduleListVBox.getChildren().clear();
            scheduleListVBox.getChildren().add(buildEmptyCard("No appointments scheduled."));
        }
    }

    @FXML
    private void onPatients() {
        SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients");
    }

    @FXML
    private void onSchedule() {
        SceneRouter.go("hospital-schedule-view.fxml", "Hospital Schedule");
    }

    @FXML
    private void onDepartments() {
        SceneRouter.go("hospital-departments-view.fxml", "Hospital Departments");
    }

    @FXML
    private void onSendPrescription() {
        SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients");
    }

    @FXML
    private void onProfile() {
        SceneRouter.go("hospital-profile-view.fxml", "Hospital Profile");
    }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    private void setLabelText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class DashboardData {
        private final List<Appointment> appointments;
        private final List<PatientProfile> patients;
        private final List<Doctor> doctors;

        private DashboardData(List<Appointment> appointments, List<PatientProfile> patients, List<Doctor> doctors) {
            this.appointments = appointments;
            this.patients = patients;
            this.doctors = doctors;
        }
    }
}
