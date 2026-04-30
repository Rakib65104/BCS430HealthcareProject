package rakib.bcs430healthcareproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ForgotUsernameController {

    @FXML
    private TextField emailField;

    @FXML
    private Label statusLabel;

    /**
     * Called when the user clicks the "Forgot password?" hyperlink.
     */
    @FXML
    protected void onForgotPassword(ActionEvent event) {
        System.out.println("Navigating to Forgot Password screen...");
        // TODO: Switch scene to ForgotPassword.fxml
    }

    /**
     * Called when the user clicks the "Send Username" button.
     */
    @FXML
    protected void onSendUsername(ActionEvent event) {
        String email = emailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showError("Please enter a valid email address.");
            return;
        }

        System.out.println("Attempting to send username to: " + email);
        // TODO: Implement database lookup and email sending logic here

        // Example of success state:
        // showSuccess("If this email is registered, your username has been sent.");
    }

    /**
     * Called when the user clicks the "Back to Login" hyperlink.
     */
    @FXML
    protected void onBackToLogin(ActionEvent event) {
        System.out.println("Navigating back to Login screen...");
        // TODO: Switch scene to Login.fxml
    }

    /**
     * Helper method to display error messages.
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-wrap-text: true;"); // Red text
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    /**
     * Helper method to display success messages.
     */
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 12; -fx-wrap-text: true;"); // Green text
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }
}