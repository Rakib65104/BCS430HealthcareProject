package rakib.bcs430healthcareproject;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class SceneRouter {
    private static Stage stage;

    private SceneRouter() {}

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setMinWidth(900);
        stage.setMinHeight(600);
    }

    public static void go(String fxmlFileName, String title) {
        try {
            // resources are in: src/main/resources/rakib.healthcareproject/
            String path = Objects.requireNonNull(SceneRouter.class.getResource(fxmlFileName)).toExternalForm();
            Parent root = FXMLLoader.load(Objects.requireNonNull(SceneRouter.class.getResource(fxmlFileName)));

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(SceneRouter.class.getResource("app.css")).toExternalForm());

            stage.setTitle(title);
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxmlFileName, e);
        }
    }
}
