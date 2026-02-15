package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;

public class SignupRoleController {

    @FXML
    private void onPatient() {
        System.out.println("Patient selected (next: patient signup screen)");
    }

    @FXML
    private void onDoctor() {
        System.out.println("Doctor selected (next: doctor signup screen)");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("login-view.fxml", "Healthcare Project");
    }
}
