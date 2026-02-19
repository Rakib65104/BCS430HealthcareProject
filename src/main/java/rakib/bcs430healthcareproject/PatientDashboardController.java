package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
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
        // TODO: Implement appointments view
        // SceneRouter.go("patient-appointments-view.fxml", "My Appointments");
    }

    @FXML
    private void onPrescriptions() {
        System.out.println("Navigating to prescriptions...");
        // TODO: Implement prescriptions view
        // SceneRouter.go("patient-prescriptions-view.fxml", "My Prescriptions");
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
