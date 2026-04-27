package rakib.bcs430healthcareproject;

import javafx.application.Application;

public class Launcher {
    static {
        JavaFxRuntimeSupport.configure();
    }

    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}
