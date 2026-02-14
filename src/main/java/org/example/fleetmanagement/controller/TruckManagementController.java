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
import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.model.TruckAttachment;
import org.example.fleetmanagement.repository.TruckAttachmentRepository;
import org.example.fleetmanagement.service.TruckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/**
 * Kontroler zarządzający widokiem flotą ciężarówek
 */
@Component
public class TruckManagementController {
    
    private final TruckService truckService;
    private final TruckAttachmentRepository attachmentRepository;
    private final ObservableList<Truck> truckList = FXCollections.observableArrayList();
    private VBox view;
    private TableView<Truck> tableView;
    
    @Autowired
    public TruckManagementController(TruckService truckService, TruckAttachmentRepository attachmentRepository) {
        this.truckService = truckService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }
    
    /**
     * Inicjalizuje widok zarządzania ciężarówkami
     */
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Zarządzanie Flotą");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Dodaj ciężarówkę");
        addButton.setOnAction(e -> showAddTruckDialog());
        
        Button editButton = new Button("Edytuj lokalizację/towar");
        editButton.setOnAction(e -> showEditLocationCargoDialog());
        
        Button attachmentsButton = new Button("Załączniki PDF");
        attachmentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());
        
        Button deleteButton = new Button("Usuń ciężarówkę");
        deleteButton.setOnAction(e -> handleDeleteTruck());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> refreshData());
        
        HBox buttonBox = new HBox(10, addButton, editButton, attachmentsButton, deleteButton, refreshButton);

        tableView = new TableView<>();
        tableView.setItems(truckList);
        
        TableColumn<Truck, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        
        TableColumn<Truck, String> brandColumn = new TableColumn<>("Marka");
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        brandColumn.setPrefWidth(200);
        
        TableColumn<Truck, String> registrationColumn = new TableColumn<>("Nr rejestracyjny");
        registrationColumn.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        registrationColumn.setPrefWidth(150);
        
        TableColumn<Truck, Truck.TruckStatus> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);
        
        TableColumn<Truck, String> locationColumn = new TableColumn<>("Lokalizacja");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("currentLocation"));
        locationColumn.setPrefWidth(150);
        
        TableColumn<Truck, String> cargoColumn = new TableColumn<>("Towar");
        cargoColumn.setCellValueFactory(new PropertyValueFactory<>("cargoDescription"));
        cargoColumn.setPrefWidth(150);
        
        TableColumn<Truck, String> attachmentsColumn = new TableColumn<>("Załączniki");
        attachmentsColumn.setCellValueFactory(cellData -> {
            Truck truck = cellData.getValue();
            int count = truck.getAttachmentCount();
            return new SimpleStringProperty(count > 0 ? count + " plik(ów)" : "brak");
        });
        attachmentsColumn.setPrefWidth(100);
        
        tableView.getColumns().addAll(idColumn, brandColumn, registrationColumn, statusColumn, locationColumn, cargoColumn, attachmentsColumn);
        
        view.getChildren().addAll(titleLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Zwraca widok kontrolera
     */
    public Parent getView() {
        return view;
    }
    
    /**
     * Odświeża dane w tabeli
     */
    public void refreshData() {
        truckList.clear();
        truckList.addAll(truckService.getAllTrucks());
    }
    
    /**
     * Wyświetla dialog dodawania nowej ciężarówki
     */
    private void showAddTruckDialog() {
        Dialog<Truck> dialog = new Dialog<>();
        dialog.setTitle("Dodaj ciężarówkę");
        dialog.setHeaderText("Wprowadź dane nowej ciężarówki");
        
        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField brandField = new TextField();
        brandField.setPromptText("Marka (np. Volvo FH16)");
        
        TextField registrationField = new TextField();
        registrationField.setPromptText("Nr rejestracyjny (np. WW12345)");
        
        ComboBox<Truck.TruckStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(Truck.TruckStatus.values());
        statusComboBox.setValue(Truck.TruckStatus.ACTIVE);

        Label locationLabel = new Label("Aktualna lokalizacja:");
        TextField locationField = new TextField();
        locationField.setPromptText("np. Warszawa, ul. Przemysłowa 15");
        
        Label cargoLabel = new Label("Opis towaru:");
        TextField cargoField = new TextField();
        cargoField.setPromptText("np. Elektronika - 500 paczek");
        
        VBox activeFieldsBox = new VBox(5, locationLabel, locationField, cargoLabel, cargoField);
        activeFieldsBox.setVisible(true);
        activeFieldsBox.setManaged(true);

        statusComboBox.setOnAction(e -> {
            boolean isActive = statusComboBox.getValue() == Truck.TruckStatus.ACTIVE;
            activeFieldsBox.setVisible(isActive);
            activeFieldsBox.setManaged(isActive);
            if (!isActive) {
                locationField.clear();
                cargoField.clear();
            }
        });
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Marka:"), brandField,
            new Label("Numer rejestracyjny:"), registrationField,
            new Label("Status:"), statusComboBox,
            activeFieldsBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Truck truck = new Truck();
                truck.setBrand(brandField.getText());
                truck.setRegistrationNumber(registrationField.getText());
                truck.setStatus(statusComboBox.getValue());
                if (statusComboBox.getValue() == Truck.TruckStatus.ACTIVE) {
                    truck.setCurrentLocation(locationField.getText());
                    truck.setCargoDescription(cargoField.getText());
                }
                return truck;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(truck -> {
            try {
                truckService.addTruck(truck);
                refreshData();
                showAlert("Sukces", "Ciężarówka została dodana", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można dodać ciężarówki: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Wyświetla dialog edycji lokalizacji i towaru dla aktywnej ciężarówki
     */
    private void showEditLocationCargoDialog() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Błąd", "Proszę wybrać ciężarówkę do edycji", Alert.AlertType.WARNING);
            return;
        }
        
        if (selectedTruck.getStatus() != Truck.TruckStatus.ACTIVE) {
            showAlert("Informacja", "Lokalizacja i towar są dostępne tylko dla ciężarówek o statusie ACTIVE", 
                Alert.AlertType.INFORMATION);
            return;
        }
        
        Dialog<Truck> dialog = new Dialog<>();
        dialog.setTitle("Edytuj lokalizację i towar");
        dialog.setHeaderText("Ciężarówka: " + selectedTruck.getBrand() + " (" + selectedTruck.getRegistrationNumber() + ")");
        
        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        TextField locationField = new TextField();
        locationField.setText(selectedTruck.getCurrentLocation() != null ? selectedTruck.getCurrentLocation() : "");
        locationField.setPromptText("np. Kraków, Magazyn Centralny");
        
        TextField cargoField = new TextField();
        cargoField.setText(selectedTruck.getCargoDescription() != null ? selectedTruck.getCargoDescription() : "");
        cargoField.setPromptText("np. Artykuły spożywcze - 2 tony");
        
        ComboBox<Truck.TruckStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(Truck.TruckStatus.values());
        statusComboBox.setValue(selectedTruck.getStatus());
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Status:"), statusComboBox,
            new Label("Aktualna lokalizacja:"), locationField,
            new Label("Opis towaru:"), cargoField
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selectedTruck.setStatus(statusComboBox.getValue());
                if (statusComboBox.getValue() == Truck.TruckStatus.ACTIVE) {
                    selectedTruck.setCurrentLocation(locationField.getText());
                    selectedTruck.setCargoDescription(cargoField.getText());
                } else {
                    selectedTruck.setCurrentLocation(null);
                    selectedTruck.setCargoDescription(null);
                }
                return selectedTruck;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(truck -> {
            try {
                truckService.updateTruck(truck);
                refreshData();
                showAlert("Sukces", "Dane ciężarówki zostały zaktualizowane", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można zaktualizować ciężarówki: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Obsługuje usuwanie ciężarówki
     */
    private void handleDeleteTruck() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Błąd", "Proszę wybrać ciężarówkę do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz usunąć tę ciężarówkę?");
        confirmAlert.setContentText("Marka: " + selectedTruck.getBrand() + 
            "\nNumer rejestracyjny: " + selectedTruck.getRegistrationNumber());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    truckService.deleteTruck(selectedTruck.getId());
                    refreshData();
                    showAlert("Sukces", "Ciężarówka została usunięta", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć ciężarówki: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Wyświetla dialog zarządzania załącznikami PDF dla ciężarówki
     */
    private void showAttachmentsDialog() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Błąd", "Proszę wybrać ciężarówkę", Alert.AlertType.WARNING);
            return;
        }

        selectedTruck = truckService.getTruckById(selectedTruck.getId()).orElse(selectedTruck);
        final Truck truck = selectedTruck;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Załączniki PDF");
        dialog.setHeaderText("Ciężarówka: " + truck.getBrand() + " (" + truck.getRegistrationNumber() + ")");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TruckAttachment> attachmentTable = new TableView<>();
        ObservableList<TruckAttachment> attachmentList = FXCollections.observableArrayList(truck.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);
        
        TableColumn<TruckAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<TruckAttachment, String> nameCol = new TableColumn<>("Nazwa pliku");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);
        
        TableColumn<TruckAttachment, String> descCol = new TableColumn<>("Opis");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);
        
        TableColumn<TruckAttachment, String> sizeCol = new TableColumn<>("Rozmiar");
        sizeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);
        
        TableColumn<TruckAttachment, String> dateCol = new TableColumn<>("Data dodania");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                );
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);
        
        attachmentTable.getColumns().addAll(idCol, nameCol, descCol, sizeCol, dateCol);

        Button addBtn = new Button("Dodaj PDF");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            addAttachmentToTruck(truck, attachmentList);
            refreshData();
        });
        
        Button downloadBtn = new Button("Pobierz PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TruckAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                downloadAttachment(selected);
            } else {
                showAlert("Błąd", "Wybierz załącznik do pobrania", Alert.AlertType.WARNING);
            }
        });
        
        Button deleteBtn = new Button("Usuń załącznik");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            TruckAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteAttachment(truck, selected, attachmentList);
                refreshData();
            } else {
                showAlert("Błąd", "Wybierz załącznik do usunięcia", Alert.AlertType.WARNING);
            }
        });
        
        HBox buttonBox = new HBox(10, addBtn, downloadBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Lista załączników PDF:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(400);
        
        dialog.showAndWait();
    }
    
    /**
     * Dodaje nowy załącznik PDF do ciężarówki
     */
    private void addAttachmentToTruck(Truck truck, ObservableList<TruckAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            TextInputDialog descDialog = new TextInputDialog();
            descDialog.setTitle("Opis załącznika");
            descDialog.setHeaderText("Dodaj opis pliku (opcjonalnie)");
            descDialog.setContentText("Opis:");
            
            String description = descDialog.showAndWait().orElse("");
            
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                
                TruckAttachment attachment = new TruckAttachment(
                    file.getName(),
                    description,
                    fileData,
                    truck
                );
                
                attachmentRepository.save(attachment);
                attachmentList.add(attachment);
                
                showAlert("Sukces", "Plik '" + file.getName() + "' został dodany", Alert.AlertType.INFORMATION);
                
            } catch (IOException e) {
                showAlert("Błąd", "Nie można odczytać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Pobiera załącznik i zapisuje na dysku
     */
    private void downloadAttachment(TruckAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik PDF");
        fileChooser.setInitialFileName(attachment.getFilename());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Files.write(file.toPath(), attachment.getFileData());
                showAlert("Sukces", "Plik został zapisany jako:\n" + file.getAbsolutePath(), 
                    Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie można zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Usuwa załącznik z ciężarówki
     */
    private void deleteAttachment(Truck truck, TruckAttachment attachment, ObservableList<TruckAttachment> attachmentList) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz usunąć ten załącznik?");
        confirmAlert.setContentText("Plik: " + attachment.getFilename());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    attachmentRepository.delete(attachment);
                    attachmentList.remove(attachment);
                    showAlert("Sukces", "Załącznik został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć załącznika: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Wyświetla okno dialogowe z komunikatem
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
