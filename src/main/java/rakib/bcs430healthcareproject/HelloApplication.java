package rakib.bcs430healthcareproject;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        // Initialize Firebase Admin SDK
        try {
            FirebaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("Warning: Firebase initialization failed. Some features may not work.");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        SceneRouter.init(stage);
        SceneRouter.go("login-view.fxml", "Healthcare Project");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}