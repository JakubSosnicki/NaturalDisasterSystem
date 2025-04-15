module GUI.Projekt.Java {
    requires com.esri.arcgisruntime;
    requires javafx.web;
    requires jdk.jsobject;
    requires javafx.swing;
    requires jxmapviewer2;
    requires org.json;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires java.net.http; // Dodajemy dostęp do netscape.javascript
    exports views;
    opens views to javafx.fxml;
}