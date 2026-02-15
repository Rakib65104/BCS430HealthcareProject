package rakib.bcs430healthcareproject;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        SceneRouter.init(stage);
        SceneRouter.go("login-view.fxml", "Healthcare Project");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}