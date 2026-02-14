package org.example.fleetmanagement.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.Trip;
import org.example.fleetmanagement.model.TripAttachment;
import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.TripAttachmentRepository;
import org.example.fleetmanagement.service.DriverService;
import org.example.fleetmanagement.service.TripService;
import org.example.fleetmanagement.service.TruckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/**
 * Kontroler zarządzający widokiem rejsów/tras
 */
@Component
public class TripManagementController {
    
    private final TripService tripService;
    private final DriverService driverService;
    private final TruckService truckService;
    private final TripAttachmentRepository attachmentRepository;
    private final ObservableList<Trip> tripList = FXCollections.observableArrayList();
    private VBox view;
    private TableView<Trip> tableView;
    private CheckBox showAllCheckbox;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Autowired
    public TripManagementController(TripService tripService, DriverService driverService, 
                                    TruckService truckService, TripAttachmentRepository attachmentRepository) {
        this.tripService = tripService;
        this.driverService = driverService;
        this.truckService = truckService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        
        Label titleLabel = new Label("Aktywne Rejsy");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        showAllCheckbox = new CheckBox("Pokaż wszystkie rejsy (w tym zakończone)");
        showAllCheckbox.setOnAction(e -> refreshData());

        Button addButton = new Button("Nowy rejs");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddTripDialog());
        
        Button startButton = new Button("Rozpocznij rejs");
        startButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        startButton.setOnAction(e -> handleStartTrip());
        
        Button completeButton = new Button("Zakończ rejs");
        completeButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        completeButton.setOnAction(e -> handleCompleteTrip());
        
        Button cancelButton = new Button("Anuluj rejs");
        cancelButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> handleCancelTrip());
        
        Button attachmentsButton = new Button("Dokumenty PDF");
        attachmentsButton.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());
        
        Button deleteButton = new Button("Usuń rejs");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteTrip());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> refreshData());
        
        HBox buttonBox1 = new HBox(10, addButton, startButton, completeButton, cancelButton);
        HBox buttonBox2 = new HBox(10, attachmentsButton, deleteButton, refreshButton);
        VBox buttonContainer = new VBox(5, buttonBox1, buttonBox2);

        tableView = new TableView<>();
        tableView.setItems(tripList);
        
        TableColumn<Trip, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Trip, String> truckCol = new TableColumn<>("Ciężarówka");
        truckCol.setCellValueFactory(cellData -> {
            Truck truck = cellData.getValue().getTruck();
            return new SimpleStringProperty(truck != null ? 
                truck.getBrand() + " (" + truck.getRegistrationNumber() + ")" : "-");
        });
        truckCol.setPrefWidth(180);
        
        TableColumn<Trip, String> driverCol = new TableColumn<>("Kierowca");
        driverCol.setCellValueFactory(cellData -> {
            Driver driver = cellData.getValue().getDriver();
            return new SimpleStringProperty(driver != null ? driver.getFullName() : "-");
        });
        driverCol.setPrefWidth(150);
        
        TableColumn<Trip, String> routeCol = new TableColumn<>("Trasa");
        routeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRouteDescription()));
        routeCol.setPrefWidth(200);
        
        TableColumn<Trip, String> cargoCol = new TableColumn<>("Ładunek");
        cargoCol.setCellValueFactory(new PropertyValueFactory<>("cargoDescription"));
        cargoCol.setPrefWidth(150);
        
        TableColumn<Trip, Trip.TripStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        TableColumn<Trip, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStartTime() != null) {
                return new SimpleStringProperty(cellData.getValue().getStartTime().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("-");
        });
        startCol.setPrefWidth(120);
        
        TableColumn<Trip, String> attachCol = new TableColumn<>("Dokumenty");
        attachCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getAttachmentCount();
            return new SimpleStringProperty(count > 0 ? count + " plik(ów)" : "brak");
        });
        attachCol.setPrefWidth(80);
        
        tableView.getColumns().addAll(idCol, truckCol, driverCol, routeCol, cargoCol, statusCol, startCol, attachCol);

        tableView.setRowFactory(tv -> new TableRow<Trip>() {
            @Override
            protected void updateItem(Trip trip, boolean empty) {
                super.updateItem(trip, empty);
                if (empty || trip == null) {
                    setStyle("");
                } else {
                    switch (trip.getStatus()) {
                        case PLANNED -> setStyle("-fx-background-color: #fff9c4;");
                        case IN_PROGRESS -> setStyle("-fx-background-color: #c8e6c9;");
                        case COMPLETED -> setStyle("-fx-background-color: #e0e0e0;");
                        case CANCELLED -> setStyle("-fx-background-color: #ffcdd2;");
                        default -> setStyle("");
                    }
                }
            }
        });

        HBox legend = new HBox(15);
        legend.getChildren().addAll(
            createLegendItem("Zaplanowany", "#fff9c4"),
            createLegendItem("W trakcie", "#c8e6c9"),
            createLegendItem("Zakończony", "#e0e0e0"),
            createLegendItem("Anulowany", "#ffcdd2")
        );
        legend.setPadding(new Insets(5, 0, 0, 0));
        
        view.getChildren().addAll(titleLabel, showAllCheckbox, buttonContainer, tableView, legend);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    private Label createLegendItem(String text, String color) {
        Label label = new Label("■ " + text);
        label.setStyle("-fx-text-fill: " + color.replace("#", "#") + "; -fx-font-size: 11px;");
        return label;
    }
    
    public Parent getView() {
        return view;
    }
    
    public void refreshData() {
        tripList.clear();
        if (showAllCheckbox.isSelected()) {
            tripList.addAll(tripService.getAllTrips());
        } else {
            tripList.addAll(tripService.getActiveTrips());
        }
    }
    
    private void showAddTripDialog() {
        Dialog<Trip> dialog = new Dialog<>();
        dialog.setTitle("Nowy rejs");
        dialog.setHeaderText("Utwórz nowy rejs");
        
        ButtonType addButtonType = new ButtonType("Utwórz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        ComboBox<Truck> truckCombo = new ComboBox<>();
        truckCombo.getItems().addAll(truckService.getTrucksByStatus(Truck.TruckStatus.ACTIVE));
        truckCombo.setConverter(new StringConverter<Truck>() {
            @Override
            public String toString(Truck truck) {
                return truck != null ? truck.getBrand() + " (" + truck.getRegistrationNumber() + ")" : "";
            }
            @Override
            public Truck fromString(String string) { return null; }
        });
        truckCombo.setPrefWidth(300);

        ComboBox<Driver> driverCombo = new ComboBox<>();
        driverCombo.getItems().addAll(driverService.getAvailableDrivers());
        driverCombo.setConverter(new StringConverter<Driver>() {
            @Override
            public String toString(Driver driver) {
                return driver != null ? driver.getFullName() + " (" + driver.getLicenseNumber() + ")" : "";
            }
            @Override
            public Driver fromString(String string) { return null; }
        });
        driverCombo.setPrefWidth(300);
        
        TextField originField = new TextField();
        originField.setPromptText("Miejsce wyjazdu (np. Warszawa, Magazyn Główny)");
        
        TextField destinationField = new TextField();
        destinationField.setPromptText("Miejsce docelowe (np. Kraków, ul. Logistyczna 10)");
        
        TextField cargoField = new TextField();
        cargoField.setPromptText("Opis ładunku (np. Elektronika - 500 paczek)");
        
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Dodatkowe uwagi...");
        notesArea.setPrefRowCount(3);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Ciężarówka:"), truckCombo,
            new Label("Kierowca:"), driverCombo,
            new Label("Miejsce wyjazdu:"), originField,
            new Label("Miejsce docelowe:"), destinationField,
            new Label("Opis ładunku:"), cargoField,
            new Label("Uwagi:"), notesArea
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(450);

        final Button createBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        createBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (truckCombo.getValue() == null) {
                showAlert("Błąd", "Wybierz ciężarówkę", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (driverCombo.getValue() == null) {
                showAlert("Błąd", "Wybierz kierowcę", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (originField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Podaj miejsce wyjazdu", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (destinationField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Podaj miejsce docelowe", Alert.AlertType.ERROR);
                event.consume();
            }
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Trip trip = new Trip();
                trip.setTruck(truckCombo.getValue());
                trip.setDriver(driverCombo.getValue());
                trip.setOrigin(originField.getText().trim());
                trip.setDestination(destinationField.getText().trim());
                trip.setCargoDescription(cargoField.getText().trim());
                trip.setNotes(notesArea.getText().trim());
                trip.setStatus(Trip.TripStatus.PLANNED);
                return trip;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(trip -> {
            try {
                tripService.createTrip(trip);
                refreshData();
                showAlert("Sukces", "Rejs został utworzony", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można utworzyć rejsu: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    private void handleStartTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz rejs do rozpoczęcia", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() != Trip.TripStatus.PLANNED) {
            showAlert("Błąd", "Można rozpocząć tylko zaplanowany rejs", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            tripService.startTrip(selected.getId());
            refreshData();
            showAlert("Sukces", "Rejs został rozpoczęty", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Błąd", "Nie można rozpocząć rejsu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void handleCompleteTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz rejs do zakończenia", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() != Trip.TripStatus.IN_PROGRESS) {
            showAlert("Błąd", "Można zakończyć tylko rejs w trakcie", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            tripService.completeTrip(selected.getId());
            refreshData();
            showAlert("Sukces", "Rejs został zakończony", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Błąd", "Nie można zakończyć rejsu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void handleCancelTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz rejs do anulowania", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() == Trip.TripStatus.COMPLETED || 
            selected.getStatus() == Trip.TripStatus.CANCELLED) {
            showAlert("Błąd", "Ten rejs nie może być anulowany", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz anulować ten rejs?");
        confirm.setContentText(selected.getRouteDescription());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tripService.cancelTrip(selected.getId());
                    refreshData();
                    showAlert("Sukces", "Rejs został anulowany", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można anulować rejsu: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void handleDeleteTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz rejs do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć ten rejs?");
        confirm.setContentText(selected.getRouteDescription());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tripService.deleteTrip(selected.getId());
                    refreshData();
                    showAlert("Sukces", "Rejs został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć rejsu: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Wyświetla dialog zarządzania załącznikami PDF dla rejsu
     */
    private void showAttachmentsDialog() {
        Trip selectedTrip = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTrip == null) {
            showAlert("Błąd", "Wybierz rejs", Alert.AlertType.WARNING);
            return;
        }

        selectedTrip = tripService.getTripById(selectedTrip.getId()).orElse(selectedTrip);
        final Trip trip = selectedTrip;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dokumenty PDF rejsu");
        dialog.setHeaderText("Rejs: " + trip.getRouteDescription() + 
            "\nKierowca: " + trip.getDriver().getFullName() +
            "\nCiężarówka: " + trip.getTruck().getRegistrationNumber());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TripAttachment> attachmentTable = new TableView<>();
        ObservableList<TripAttachment> attachmentList = FXCollections.observableArrayList(trip.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);
        
        TableColumn<TripAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<TripAttachment, String> nameCol = new TableColumn<>("Nazwa pliku");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);
        
        TableColumn<TripAttachment, String> descCol = new TableColumn<>("Opis");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);
        
        TableColumn<TripAttachment, String> sizeCol = new TableColumn<>("Rozmiar");
        sizeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);
        
        TableColumn<TripAttachment, String> dateCol = new TableColumn<>("Data dodania");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getUploadedAt().format(DATE_FORMAT)
                );
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);
        
        attachmentTable.getColumns().addAll(idCol, nameCol, descCol, sizeCol, dateCol);

        Button addBtn = new Button("Dodaj PDF");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            addAttachmentToTrip(trip, attachmentList);
            refreshData();
        });
        
        Button downloadBtn = new Button("Pobierz PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TripAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                downloadAttachment(selected);
            } else {
                showAlert("Błąd", "Wybierz załącznik do pobrania", Alert.AlertType.WARNING);
            }
        });
        
        Button deleteBtn = new Button("Usuń załącznik");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            TripAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteAttachment(trip, selected, attachmentList);
                refreshData();
            } else {
                showAlert("Błąd", "Wybierz załącznik do usunięcia", Alert.AlertType.WARNING);
            }
        });
        
        HBox buttonBox = new HBox(10, addBtn, downloadBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Lista dokumentów PDF rejsu:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(450);
        
        dialog.showAndWait();
    }
    
    /**
     * Dodaje nowy załącznik PDF do rejsu
     */
    private void addAttachmentToTrip(Trip trip, ObservableList<TripAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            // Dialog na opis pliku
            TextInputDialog descDialog = new TextInputDialog();
            descDialog.setTitle("Opis dokumentu");
            descDialog.setHeaderText("Dodaj opis dokumentu (opcjonalnie)");
            descDialog.setContentText("Opis:");
            
            String description = descDialog.showAndWait().orElse("");
            
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                
                TripAttachment attachment = new TripAttachment(
                    file.getName(),
                    description,
                    fileData,
                    trip
                );
                
                attachmentRepository.save(attachment);
                attachmentList.add(attachment);
                
                showAlert("Sukces", "Dokument '" + file.getName() + "' został dodany", Alert.AlertType.INFORMATION);
                
            } catch (IOException e) {
                showAlert("Błąd", "Nie można odczytać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Pobiera załącznik i zapisuje na dysku
     */
    private void downloadAttachment(TripAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz dokument PDF");
        fileChooser.setInitialFileName(attachment.getFilename());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Files.write(file.toPath(), attachment.getFileData());
                showAlert("Sukces", "Dokument został zapisany jako:\n" + file.getAbsolutePath(), 
                    Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie można zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Usuwa załącznik z rejsu
     */
    private void deleteAttachment(Trip trip, TripAttachment attachment, ObservableList<TripAttachment> attachmentList) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz usunąć ten dokument?");
        confirmAlert.setContentText("Plik: " + attachment.getFilename());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    attachmentRepository.delete(attachment);
                    attachmentList.remove(attachment);
                    showAlert("Sukces", "Dokument został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć dokumentu: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
