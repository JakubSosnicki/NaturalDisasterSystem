package views;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.*;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

public class NaturalDisasterWarningSystem extends Application {
    private String city;
    private List<String> selectedThreats = new ArrayList<>();
    private double threatRadius = 100;
    private JXMapViewer mapViewer;
    private GeoPosition selectedLocation;
    private Label coordinatesLabel;
    private Button resetButton;
    private TextField cityInput;



    private Map<GeoPosition, List<ThreatDetail>> threatCache = new HashMap<>();


    private static final String EONET_API_URL = "https://eonet.gsfc.nasa.gov/api/v3/events/geojson";





    @Override
    public void start(Stage primaryStage) {
        //Kontener główny
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(100));
        mainContainer.setStyle("-fx-background-color: #eceff1;");

        // Tytuł
        Label titleLabel = new Label("Natural Disaster Warning System");
        titleLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));

        // Tekst pod ikoną
        Label infoLabel = new Label("Enter city name: ");
        infoLabel.setFont(Font.font("Times New Roman", 16));

        // Tekst pod polem wprowadzania miasta
        Label orLabel = new Label(" or ");
        orLabel.setFont(Font.font("Times New Roman", 16));

        // Ikona
        ImageView warningIcon = new ImageView();
        Image warningImage = new Image("file:resources/alarm.png"); //ścieżka do pliku
        warningIcon.setImage(warningImage);
        warningIcon.setFitHeight(100); //wymiary
        warningIcon.setFitWidth(100);

        // Pole wprowadzenia miasta
        cityInput = new TextField();
        cityInput.setPromptText("Enter city name");
        cityInput.setMaxWidth(300);
        cityInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                city = newValue.substring(0, 1).toUpperCase() + newValue.substring(1).toLowerCase();
            } else {
                city = null;
            }
        });

        // Wyświetlanie współrzędnych
        coordinatesLabel = new Label("");
        coordinatesLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        coordinatesLabel.setStyle("-fx-text-fill: #555555;");

        // Przyciski
        Button settingsButton = createStyledButton("Search Settings");
        Button locationButton = createStyledButton("Select Location");
        Button searchButton = createStyledButtonTwo("Find threats");
        resetButton = createStyledResetButton("Reset");

        resetButton.setDisable(true); // ustawienie resetu na wyłączony na początku

        // Ułożenie przycisków w poziomie
        HBox buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(searchButton, resetButton);
        VBox.setMargin(buttonContainer, new Insets(20, 0, 0, 0));

        // Dodanie do kontenera głównego
        mainContainer.getChildren().addAll(
                titleLabel,
                warningIcon,
                infoLabel,
                cityInput,
                orLabel,
                locationButton,
                coordinatesLabel,
                settingsButton,
                buttonContainer
        );

        // Stworzenie sceny głównej
        Scene mainScene = new Scene(mainContainer, 800, 600);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Natural Disaster Warning System");

        // Ustawienie akcji dla przycisków
        settingsButton.setOnAction(e -> showSettingsWindow()); //otworzenie okna ustawień
        locationButton.setOnAction(e -> showMapWindow()); //otworzenie okna wyboru lokalizacji z mapy
        searchButton.setOnAction(e -> {
            if (selectedLocation == null && city != null) {
                searchThreatsByCity();
            } else {
                searchThreats();
            }
            resetButton.setDisable(false); // Włączenie przycisku resetu
        }); // ustawienie akcji dla 2 sposobów wyszukania lokalizacji
        // Dodanie akcji resetowania
        resetButton.setOnAction(e -> resetApplication());

        primaryStage.setMaximized(true); // Maksymalizacja okna
        primaryStage.show();
    }

    // Metoda resetowania
    private void resetApplication() {
        //ustawienie zmiennych początkowych
        city = null;
        selectedLocation = null;
        selectedThreats.clear();
        threatRadius = 100;
        threatCache.clear();
        cachedThreatDetails.clear();

        // Reset komponentów UI
        cityInput.clear(); //wyczyszczenie pola wprowaniania miasta
        coordinatesLabel.setText(""); //ustawienie tekstu koordynatów
        resetButton.setDisable(true); // wyłączenie przycisku resetu
    }

    //style dla przycisków - standardowy
    private Button createStyledButton(String text) {
        Button button = new Button(text);button.setStyle(
                "-fx-background-color: #607d8b;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 10;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;"
        );
        button.setPrefSize(300, 20);
        return button;
    }
    // styl przycisku reset
    private Button createStyledResetButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #cc0033;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5 10;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        );
        button.setPrefSize(100, 20);
        return button;
    }
    // styl kluczowych przycisków
    private Button createStyledButtonTwo(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #228B22;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;"
        );
        button.setPrefWidth(200);

        return button;
    }
    // okno ustawień
    private void showSettingsWindow() {
        //stworzenie sceny - ustawienia
        Stage settingsStage = new Stage();
        VBox settingsContainer = new VBox(20);
        settingsContainer.setAlignment(Pos.TOP_CENTER);
        settingsContainer.setPadding(new Insets(20));
        settingsContainer.setStyle("-fx-background-color: #eceff1;");

        // dodanie tekstu informacyjnego
        Label titleLabel = new Label("Select threat type and radius:");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // lista nazw zagrożeń zgodnych z API (title)
        String[] threatNames = {
                "Drought", "Dust and Haze", "Earthquakes", "Floods", "Landslides",
                "Manmade", "Sea and Lake Ice", "Severe Storms", "Snow",
                "Temperature Extremes", "Volcanoes", "Water Color", "Wildfires"
        };

        // ustawienie checkboxa
        VBox checkBoxContainer = new VBox(10);
        List<CheckBox> threatCheckBoxes = new ArrayList<>();
        for (String threat : threatNames) {
            CheckBox checkBox = new CheckBox(threat);
            threatCheckBoxes.add(checkBox);
            checkBoxContainer.getChildren().add(checkBox);
        }

        // dodanie opcji zaznaczenia wszystkich zagrożeń
        CheckBox allThreatsCheckBox = new CheckBox("All threats");
        allThreatsCheckBox.setOnAction(e -> {
            boolean isSelected = allThreatsCheckBox.isSelected();
            threatCheckBoxes.forEach(cb -> cb.setSelected(isSelected));
        });
        checkBoxContainer.getChildren().add(0, allThreatsCheckBox); // dodanie na górę listy

        // Slider do wyboru zasięgu
        Label radiusLabel = new Label("Threat radius (km):");
        Slider radiusSlider = new Slider(0, 100, 20);
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setShowTickMarks(true);
        radiusSlider.setMajorTickUnit(20);

        Button saveButton = createStyledButtonTwo("Save");
        saveButton.setOnAction(e -> {
            // akcja aktualizacji promień i wybranych zagrożeń
            threatRadius = radiusSlider.getValue();
            selectedThreats.clear();
            if (allThreatsCheckBox.isSelected()) {
                selectedThreats.addAll(List.of(threatNames)); // zaznaczenie wszystkich zagrożeń
            } else {
                for (CheckBox checkBox : threatCheckBoxes) {
                    if (checkBox.isSelected()) {
                        selectedThreats.add(checkBox.getText()); // dodawanie wybranych zagrożeń
                    }
                }
            }
            settingsStage.close(); // zamknięcie okna ustawień
        });

        // ustawienie komponentów
        settingsContainer.getChildren().addAll(
                titleLabel,
                checkBoxContainer,
                radiusLabel,
                radiusSlider,
                saveButton
        );

        //Stowrzenie sceny
        Scene settingsScene = new Scene(settingsContainer, 600, 700);
        settingsStage.setScene(settingsScene);
        settingsStage.setTitle("Threat Settings");
        settingsStage.show();
    }


// Animacja pobierania danych (nie działa poprawnie)
//    private void startAnalysisAnimation(Label analyzingLabel) {
//        String[] analyzingTexts = {"Analiza Danych.", "Analiza Danych..", "Analiza Danych..."};
//        Timeline timeline = new Timeline(
//                new KeyFrame(Duration.seconds(0), event -> analyzingLabel.setText(analyzingTexts[0])),
//                new KeyFrame(Duration.seconds(1), event -> analyzingLabel.setText(analyzingTexts[1])),
//                new KeyFrame(Duration.seconds(2), event -> analyzingLabel.setText(analyzingTexts[2])),
//                new KeyFrame(Duration.seconds(3), event -> analyzingLabel.setText(analyzingTexts[3]))
//        );
//        timeline.setCycleCount(Animation.INDEFINITE);
//        timeline.play();
//
//        // Dodaj zatrzymanie animacji po otwarciu nowego okna
//        searchButton.setOnAction(e -> {
//            timeline.stop();
//            analyzingLabel.setText("");
//        });
//    }

    // szukanie po nawie maista
    private GeoPosition geocodeCity(String cityName) {
        try {
            // Api do ściągania współrzędnych miasta system
            String encodedCityName = URLEncoder.encode(cityName, "UTF-8"); // utf dla polskich znaków
            String apiUrl = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1", encodedCityName);

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "NaturalDisasterWarningSystem/1.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parsowanie JSONA
            JSONArray jsonArray = new JSONArray(response.toString());
            if (jsonArray.length() > 0) {
                JSONObject location = jsonArray.getJSONObject(0);
                double lat = location.getDouble("lat");
                double lon = location.getDouble("lon");
                return new GeoPosition(lat, lon);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not find coordinates for city: " + cityName);
        }
        return null;
    }

    // szukanie zagrożeń po nazwie miasta
    private void searchThreatsByCity() {
        if (city == null || city.trim().isEmpty()) {
            showAlert("Error", "Please enter the name of the city.");
            return;
        }

        GeoPosition cityLocation = geocodeCity(city);
        if (cityLocation != null) {
            selectedLocation = cityLocation;
            coordinatesLabel.setText("Selected Location: lat=" + cityLocation.getLatitude() + ", lon=" + cityLocation.getLongitude());
            searchThreats();
        }
    }


    // okno wyboru lokalizacji
    private void showMapWindow() {
        Stage mapStage = new Stage();
        SwingNode swingNode = new SwingNode();

        // inicjalizacja mapy natychmiast
        initializeMap(swingNode);

        // kontener na mapę i przycisk
        VBox mapContainer = new VBox();
        mapContainer.setStyle("-fx-background-color: #eceff1;");
        mapContainer.setPadding(new Insets(10));

        // zajęcie dynamicznie 100% przestrzeni
        VBox.setVgrow(swingNode, Priority.ALWAYS);

        Button confirmButton = createStyledButtonTwo("Confirm location");
        confirmButton.setOnAction(e -> {
            if (selectedLocation != null) {
                // zapisanie współrzędnych geograficznych do zmiennych lon i lat
                double lat = selectedLocation.getLatitude();
                double lon = selectedLocation.getLongitude();

                city = String.format("%.4f, %.4f", lat, lon);
                coordinatesLabel.setText("Selected Location: lat=" + lat + ", lon=" + lon);

                // wyświetlenie potwierdzenia w konsoli
                System.out.println("Zaznaczone współrzędne: lat=" + lat + ", lon=" + lon);

                mapStage.close();
            } else {
                showAlert("Error", "Please select a location on the map");
            }
        });



        mapContainer.getChildren().addAll(swingNode, confirmButton);

        // ustawienie rozmiaru sceny
        Scene mapScene = new Scene(mapContainer, 800, 600);
        mapStage.setScene(mapScene);
        mapStage.setTitle("Map");
        mapStage.setMaximized(false);
        mapStage.show();
    }


    // klasa wewnętrzna (do napisów po najechaniu na znacznik nie działa)
    public static class InfoWaypoint extends DefaultWaypoint {
        private final String info; // informacje o zagrożeniu

        public InfoWaypoint(GeoPosition position, String info) {
            super(position);
            this.info = info;
        }

        public String getInfo() {
            return info;
        }
    }

    // metoda updateMapMarker obecnie nie działa poprawnie
    private void updateMapMarker(GeoPosition position) {
        Set<InfoWaypoint> waypoints = new HashSet<>();
        waypoints.add(new InfoWaypoint(position, "Zagrożenie: Powódź")); // Przykładowa informacja

        WaypointPainter<InfoWaypoint> painter = new WaypointPainter<>();
        painter.setWaypoints(waypoints);

        mapViewer.setOverlayPainter(painter);

        // obsługa zdarzenia najechania na znacznik (nie działa)
        mapViewer.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point mousePoint = e.getPoint();
                GeoPosition mouseGeo = mapViewer.convertPointToGeoPosition(mousePoint);

                for (InfoWaypoint waypoint : waypoints) {
                    Point2D wpPoint2D = mapViewer.getTileFactory().geoToPixel(waypoint.getPosition(), mapViewer.getZoom());
                    Point wpPoint = new Point((int) wpPoint2D.getX(), (int) wpPoint2D.getY());
                    if (wpPoint.distance(mousePoint) < 10) { // Sprawdź bliskość kursora
                        mapViewer.setToolTipText(waypoint.getInfo());
                        return;
                    }
                }
                mapViewer.setToolTipText(null); // Brak znacznika w pobliżu
            }
        });

        mapViewer.repaint();
    }



    // metoda inicjalizująca mapę z zagrożeniami
    private void initializeMap(SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            mapViewer = new JXMapViewer();
            TileFactoryInfo info = new OSMTileFactoryInfo();
            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            mapViewer.setTileFactory(tileFactory);

            GeoPosition poland = new GeoPosition(52.237049, 21.017532); // Warszawa
            mapViewer.setZoom(7);
            mapViewer.setAddressLocation(poland);

            // obsługa przesuwania i powiększania
            MouseInputListener mia = new PanMouseInputListener(mapViewer);
            mapViewer.addMouseListener(mia);
            mapViewer.addMouseMotionListener(mia);
            mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));

            // obsługa kliknięcia na mapie
            mapViewer.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point point = e.getPoint();
                    selectedLocation = mapViewer.convertPointToGeoPosition(point);
                    updateMapMarker(selectedLocation);
                }
            });

            swingNode.setContent(mapViewer);
        });
    }






    private void searchThreats() {
        if (selectedLocation == null) {
            showAlert("Error", "Please select your location first.");
            return;
        }

        try {
            // sprawdź, czy dane dla tej lokalizacji są już w pamięci podręcznej
            if (threatCache.containsKey(selectedLocation)) {
                List<ThreatDetail> cachedThreats = threatCache.get(selectedLocation);
                System.out.println("Pobrano dane z pamięci podręcznej.");
                showThreatsOnNewMap(convertToWaypoints(cachedThreats));
            } else {
                // jeśli brak danych, pobierz je z API
                Set<Waypoint> threatMarkers = fetchThreatsFromAPI(
                        selectedLocation.getLatitude(),
                        selectedLocation.getLongitude(),
                        threatRadius
                );

                if (threatMarkers.isEmpty()) {
                    showAlert("No threats", "No threats found within the selected radius.");
                } else {
                    // konwertuj dane i zapisz w pamięci podręcznej
                    List<ThreatDetail> threatDetails = fetchThreatDetailsFromWaypoints(threatMarkers);
                    threatCache.put(selectedLocation, threatDetails);
                    showThreatsOnNewMap(threatMarkers);
                }
            }
        } catch (Exception e) {
            showAlert("Error", "There was a problem downloading data: , Try Again" + e.getMessage());
            e.printStackTrace();
        }
    }

    // metody pomocnicze do zapisu
    private Set<Waypoint> convertToWaypoints(List<ThreatDetail> details) {
        Set<Waypoint> waypoints = new HashSet<>();
        for (ThreatDetail detail : details) {
            String[] coords = detail.getCoordinates().split(", ");
            double lat = Double.parseDouble(coords[0].split("=")[1]);
            double lon = Double.parseDouble(coords[1].split("=")[1]);
            waypoints.add(new DefaultWaypoint(new GeoPosition(lat, lon)));
        }
        return waypoints;
    }

    private List<ThreatDetail> fetchThreatDetailsFromWaypoints(Set<Waypoint> waypoints) {
        List<ThreatDetail> details = new ArrayList<>();
        // twórz szczegóły na podstawie markerów Waypoint
        for (Waypoint waypoint : waypoints) {
            GeoPosition position = waypoint.getPosition();
            details.add(new ThreatDetail("Title", "Type", "Date", "Source",
                    String.format("lat=%.4f, lon=%.4f", position.getLatitude(), position.getLongitude())));
        }
        return details;
    }

    // zapis danych do listy w celu optymalizacji i wywołanie w szczegóły zagrożeń
    private ObservableList<ThreatDetail> cachedThreatDetails = FXCollections.observableArrayList();





    private Set<Waypoint> fetchThreatsFromAPI(double lat, double lon, double radius) throws Exception {
        Set<Waypoint> waypoints = new HashSet<>();
        cachedThreatDetails.clear(); // opróżnij stare dane

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // jeśli użytkownik wybrał zagrożenia, dodaj je do zapytania jako parametr `categories`
            String categoriesQuery = selectedThreats.isEmpty() ? "" : String.join(",", selectedThreats);
            String apiUrl = String.format("%s?lat=%f&lon=%f&radius=%f",
                    EONET_API_URL, lat, lon, radius);

            // utwórz zapytanie HTTP
            HttpGet request = new HttpGet(apiUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(json);
                JSONArray features = jsonObject.getJSONArray("features");

                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.getJSONObject("properties");
                    JSONObject geometry = feature.getJSONObject("geometry");

                    // pobieranie szczegółów zagrożenia
                    String title = properties.getString("title");
                    String date = properties.optString("closed",
                            properties.optString("updated",
                                    properties.optString("created", "Unknown date")));
                    JSONArray categories = properties.optJSONArray("categories");
                    String threatType = "Unknown";
                    if (categories != null && categories.length() > 0) {
                        JSONObject category = categories.getJSONObject(0);
                        threatType = category.optString("title", "Unknown");
                    }

                    // filtrowanie po wybranych zagrożeniach (na podstawie tytułu kategorii)
                    if (!selectedThreats.isEmpty() && !selectedThreats.contains(threatType)) {
                        continue; // pomijaj zagrożenia, które nie zostały wybrane przez użytkownika
                    }

                    JSONArray sources = properties.optJSONArray("sources");
                    String source = "Unknown";
                    if (sources != null && sources.length() > 0) {
                        JSONObject sourceObj = sources.getJSONObject(0);
                        source = sourceObj.optString("id", "Unknown");
                    }

                    // pobieranie współrzędnych i filtrowanie według promienia
                    if (geometry.getString("type").equals("Point")) {
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        double eventLon = coordinates.getDouble(0);
                        double eventLat = coordinates.getDouble(1);

                        if (isWithinRadius(lat, lon, eventLat, eventLon, radius)) {
                            waypoints.add(new DefaultWaypoint(new GeoPosition(eventLat, eventLon)));

                            // dodaj szczegóły zagrożenia do pamięci lokalnej
                            cachedThreatDetails.add(new ThreatDetail(
                                    title,
                                    threatType,
                                    date,
                                    source,
                                    String.format("lat=%.4f, lon=%.4f", eventLat, eventLon)
                            ));
                        }
                    }
                }
            }
        }

        return waypoints;
    }



    // obliczanie promienia dla zasięgu pobierania
    private boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        final double EARTH_RADIUS = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        return distance <= radiusKm;
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }




    private void showThreatsOnNewMap(Set<Waypoint> threatMarkers) {
        Stage mapStage = new Stage();
        SwingNode swingNode = new SwingNode();

        SwingUtilities.invokeLater(() -> {
            JXMapViewer threatMapViewer = new JXMapViewer();
            TileFactoryInfo info = new OSMTileFactoryInfo();
            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            threatMapViewer.setTileFactory(tileFactory);

            GeoPosition centerPosition = selectedLocation != null
                    ? selectedLocation
                    : new GeoPosition(52.237049, 21.017532);
            threatMapViewer.setZoom(7);
            threatMapViewer.setAddressLocation(centerPosition);

            WaypointPainter<Waypoint> painter = new WaypointPainter<>();
            painter.setWaypoints(threatMarkers);
            threatMapViewer.setOverlayPainter(painter);

            MouseInputListener mia = new PanMouseInputListener(threatMapViewer);
            threatMapViewer.addMouseListener(mia);
            threatMapViewer.addMouseMotionListener(mia);
            threatMapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(threatMapViewer));

            swingNode.setContent(threatMapViewer);
        });

        VBox mapContainer = new VBox(10);
        mapContainer.setStyle("-fx-background-color: #eceff1;");
        mapContainer.setPadding(new Insets(10));
        VBox.setVgrow(swingNode, Priority.ALWAYS);

        Label threatCountLabel = new Label("Number of threats: " + threatMarkers.size());
        threatCountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Button propertiesButton = createStyledButton("Show details");
        Button closeButton = createStyledButtonTwo("Close map");
        closeButton.setOnAction(e -> mapStage.close());

        // Add action to show threat details
        propertiesButton.setOnAction(e -> showThreatDetails());

        HBox buttonContainer = new HBox(10, threatCountLabel, propertiesButton, closeButton);
        buttonContainer.setAlignment(Pos.CENTER);

        mapContainer.getChildren().addAll(swingNode, buttonContainer);

        Scene mapScene = new Scene(mapContainer, 800, 600);
        mapStage.setScene(mapScene);
        mapStage.setTitle("Natural Disaster Map");
        mapStage.show();
    }

    //szczegóły zagrożeń
    private void showThreatDetails() {
        Stage detailsStage = new Stage();
        VBox detailsContainer = new VBox(20);
        detailsContainer.setPadding(new Insets(20));
        detailsContainer.setStyle("-fx-background-color: #eceff1;");

        // nagłówek
        Label titleLabel = new Label("Threat Details");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // obszar tekstowy do wyświetlania szczegółów
        TextArea threatDetailsArea = new TextArea();
        threatDetailsArea.setEditable(false);
        threatDetailsArea.setWrapText(true);
        threatDetailsArea.setFont(Font.font("Arial", 14));
        threatDetailsArea.setPrefSize(800, 500);

        // pobierz dane z pamięci
        StringBuilder detailsText = new StringBuilder();
        for (ThreatDetail detail : cachedThreatDetails) {
            detailsText.append(String.format(
                    "Name: %s\nType: %s\nDate: %s\nSource: %s\nCoordinates: %s\n\n",
                    detail.getTitle(),
                    detail.getType(),
                    detail.getDate(),
                    detail.getSource(),
                    detail.getCoordinates()
            ));
            detailsText.append("-------------------------------------------------------------------------------------------------------\n\n");
        }
        threatDetailsArea.setText(detailsText.toString());

        // przyciski
        Button closeButton = createStyledButtonTwo("Close");
        closeButton.setOnAction(e -> detailsStage.close());

        detailsContainer.getChildren().addAll(titleLabel, threatDetailsArea, closeButton);

        // większe okno
        Scene detailsScene = new Scene(detailsContainer, 900, 650);
        detailsStage.setScene(detailsScene);
        detailsStage.setTitle("Threat Details");
        detailsStage.show();
    }





    public static class ThreatDetail {
        private String title;
        private String type;
        private String date;
        private String source;
        private String coordinates;

        public ThreatDetail(String title, String type, String date, String source, String coordinates) {
            this.title = title;
            this.type = type;
            this.date = date;
            this.source = source;
            this.coordinates = coordinates;
        }

        
        public String getTitle() { return title; }
        public String getType() { return type; }
        public String getDate() { return date; }
        public String getSource() { return source; }
        public String getCoordinates() { return coordinates; }
    }


    public static void main(String[] args) {
        launch(args);
    }
}