package rakib.bcs430healthcareproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HospitalForgotUsernameController {

    @FXML
    private TextField emailField;

    @FXML
    private Label statusLabel;

    /**
     * Called when the user clicks the "Forgot password?" hyperlink.
     * Use this to navigate back to the Forgot Password screen.
     */
    @FXML
    protected void onForgotPassword(ActionEvent event) {
        System.out.println("Navigating to Forgot Password screen...");
        // TODO: Add your scene-switching logic here to load HospitalForgotPassword.fxml
    }

    /**
     * Called when the user clicks the "Send Username" button.
     * Use this to validate the email and interact with your database.
     */
    @FXML
    protected void onSendUsername(ActionEvent event) {
        String email = emailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showError("Please enter a valid email address.");
            return;
        }

        System.out.println("Attempting to send username to: " + email);
        // TODO: Add your database check and email-sending logic here

        // Example of showing success:
        // showSuccess("If the email is registered, your username has been sent.");
    }

    /**
     * Called when the user clicks the "Back to Login" button.
     * Use this to navigate back to the main Login screen.
     */
    @FXML
    protected void onBackToLogin(ActionEvent event) {
        System.out.println("Navigating back to Login screen...");
        // TODO: Add your scene-switching logic here to load the Login FXML
    }

    /**
     * Helper method to display error messages on the UI
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;"); // Ensures it looks like an error
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    /**
     * Helper method to display success messages on the UI
     */
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;"); // Success color
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }
}