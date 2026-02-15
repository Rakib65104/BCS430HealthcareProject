module rakib.bcs430healthcareproject {
    requires javafx.controls;
    requires javafx.fxml;

    opens rakib.bcs430healthcareproject to javafx.fxml;
    exports rakib.bcs430healthcareproject;
}