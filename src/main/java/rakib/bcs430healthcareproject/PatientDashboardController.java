package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for the Patient Dashboard.
 * Displays patient information and provides navigation to other features.
 */
public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientEmailLabel;
    @FXML private Button appointmentsButton;
    @FXML private Button findDoctorButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;

    private FirebaseService firebaseService;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        
        // Load current user data from session
        UserContext userContext = UserContext.getInstance();
        
        if (userContext.isLoggedIn()) {
            PatientProfile profile = userContext.getProfile();
            String uid = userContext.getUid();
            
            System.out.println("Loading dashboard for user: " + uid);
            
            if (profile != null) {
                String displayName = profile.getName() != null ? profile.getName() : "Patient";
                welcomeLabel.setText("Welcome Back, " + displayName);
                patientNameLabel.setText("Patient Name: " + displayName);
                patientEmailLabel.setText("Email: " + profile.getEmail());
            } else {
                welcomeLabel.setText("Welcome to Your Patient Dashboard");
                patientNameLabel.setText("Patient Name: [Not loaded]");
                patientEmailLabel.setText("Email: [Not loaded]");
            }
        } else {
            // User not logged in, show defaults
            welcomeLabel.setText("Welcome to Your Patient Dashboard");
            patientNameLabel.setText("Patient Name: [Not loaded]");
            patientEmailLabel.setText("Email: [Not loaded]");
        }
    }

    @FXML
    private void onAppointments() {
        System.out.println("Navigating to appointments...");
        SceneRouter.go("patient-appointments-view.fxml", "My Appointments");
    }

    @FXML
    private void onPrescriptions() {
        System.out.println("Navigating to prescriptions...");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feature Coming Soon");
        alert.setHeaderText("Prescriptions");
        alert.setContentText("The prescriptions feature will be available soon!");
        alert.showAndWait();
    }

    @FXML
    private void onFindDoctor() {
        System.out.println("Navigating to find doctor...");
        // debug resource lookup
        java.net.URL res = SceneRouter.class.getResource("doctor-search-view.fxml");
        System.out.println("doctor-search-view.fxml resource: " + res);
        try {
            SceneRouter.go("doctor-search-view.fxml", "Find a Doctor");
        } catch (Exception ex) {
            // log and show error to user rather than crashing
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open Find a Doctor");
            alert.setContentText("An error occurred while loading the doctor search. " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onProfile() {
        System.out.println("Navigating to profile...");
        SceneRouter.go("patient-profile-view.fxml", "My Profile");
    }

    @FXML
    private void onLogout() {
        System.out.println("Logging out...");
        UserContext userContext = UserContext.getInstance();
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }
}
