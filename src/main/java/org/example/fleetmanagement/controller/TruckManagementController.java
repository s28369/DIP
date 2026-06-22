package org.example.fleetmanagement.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Controller for the truck (tractor unit) fleet management view, including PDF attachments.
 */
@Component
public class TruckManagementController {
    
    private final TruckService truckService;
    private final TruckAttachmentRepository attachmentRepository;
    private final ObservableList<Truck> truckList = FXCollections.observableArrayList();
    private FilteredList<Truck> filteredList;
    private VBox view;
    private TableView<Truck> tableView;
    
    // Constructor injection of the truck service and attachment repository; builds the view.
    @Autowired
    public TruckManagementController(TruckService truckService, TruckAttachmentRepository attachmentRepository) {
        this.truckService = truckService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }
    
    // Builds the table, search box and action buttons for the trucks screen.
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Ciągniki");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Dodaj ciągnik");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddTruckDialog());

        Button editButton = new Button("Edytuj");
        editButton.setOnAction(e -> showEditTruckDialog());

        Button attachmentsButton = new Button("Załączniki PDF");
        attachmentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());

        Button deleteButton = new Button("Usuń ciągnik");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteTruck());

        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });

        HBox buttonBox = new HBox(10, addButton, editButton, attachmentsButton, deleteButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Wpisz tekst do wyszukania...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Wszystko", "Nr rej.", "Marka", "Firma", "Status", "Lokalizacja");
        searchParam.setValue("Wszystko");

        filteredList = new FilteredList<>(truckList, p -> true);

        Runnable applyFilter = () -> {
            String text = searchField.getText();
            String param = searchParam.getValue();
            if (text == null || text.trim().isEmpty()) {
                filteredList.setPredicate(p -> true);
                return;
            }
            String lower = text.trim().toLowerCase();
            filteredList.setPredicate(truck -> switch (param) {
                case "Nr rej." -> contains(truck.getRegistrationNumber(), lower);
                case "Marka" -> contains(truck.getBrand(), lower);
                case "Firma" -> contains(truck.getCompany(), lower);
                case "Status" -> contains(truck.getStatus(), lower);
                case "Lokalizacja" -> contains(truck.getCurrentLocation(), lower);
                default -> contains(truck.getRegistrationNumber(), lower)
                        || contains(truck.getBrand(), lower)
                        || contains(truck.getCompany(), lower)
                        || contains(truck.getStatus(), lower)
                        || contains(truck.getCurrentLocation(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Szukaj:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);

        TableColumn<Truck, String> registrationColumn = new TableColumn<>("Numer rejestracyjny");
        registrationColumn.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        registrationColumn.setPrefWidth(170);

        TableColumn<Truck, String> brandColumn = new TableColumn<>("Marka");
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        brandColumn.setPrefWidth(180);

        TableColumn<Truck, String> companyColumn = new TableColumn<>("Firma");
        companyColumn.setCellValueFactory(cellData -> {
            String c = cellData.getValue().getCompany();
            return new SimpleStringProperty(c != null ? c : "—");
        });
        companyColumn.setPrefWidth(120);

        TableColumn<Truck, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(120);

        TableColumn<Truck, String> locationColumn = new TableColumn<>("Lokalizacja");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("currentLocation"));
        locationColumn.setPrefWidth(180);

        tableView.getColumns().addAll(registrationColumn, brandColumn, companyColumn, statusColumn, locationColumn);

        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Truck item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getExpirationStyle(item.getAttachments()));
                }
            }
        });

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                showAttachmentsDialog();
            }
        });

        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Zwraca widok kontrolera
     */
    // Returns the root node of this view.
    public Parent getView() {
        return view;
    }
    
    /**
     * Odświeża dane w tabeli
     */
    // Reloads all trucks into the table (on the FX thread).
    public void refreshData() {
        var data = truckService.getAllTrucks();
        if (javafx.application.Platform.isFxApplicationThread()) {
            truckList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> truckList.setAll(data));
        }
    }
    
    /**
     * Wyświetla okno dialogowe dodawania nowego ciągnika
     */
    // Creates an editable combo box pre-filled with the given items.
    private ComboBox<String> createEditableComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setEditable(true);
        combo.setPrefWidth(300);
        return combo;
    }

    // Shows the dialog for adding a new truck.
    private void showAddTruckDialog() {
        Dialog<Truck> dialog = new Dialog<>();
        dialog.setTitle("Dodaj ciągnik");
        dialog.setHeaderText("Wprowadź dane nowego ciągnika");

        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField registrationField = new TextField();
        registrationField.setPromptText("Nr rej. (np. WW12345)");

        TextField brandField = new TextField();
        brandField.setPromptText("Marka (np. Volvo FH16)");

        ComboBox<String> companyCombo = createEditableComboBox(
                Truck.COMPANY_MTG, Truck.COMPANY_APA, Truck.COMPANY_ABSOLUT);

        ComboBox<String> statusCombo = createEditableComboBox(
                Truck.STATUS_AVAILABLE, Truck.STATUS_ON_TRIP, Truck.STATUS_MAINTENANCE);
        statusCombo.setValue(Truck.STATUS_AVAILABLE);

        TextField locationField = new TextField();
        locationField.setPromptText("np. Warszawa, ul. Przemysłowa 15");

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Numer rejestracyjny:"), registrationField,
            new Label("Marka:"), brandField,
            new Label("Firma:"), companyCombo,
            new Label("Status:"), statusCombo,
            new Label("Lokalizacja:"), locationField
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (registrationField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Numer rejestracyjny nie może być pusty", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (brandField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Marka nie może być pusta", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Truck truck = new Truck();
                truck.setRegistrationNumber(registrationField.getText().trim());
                truck.setBrand(brandField.getText().trim());
                String company = companyCombo.getEditor().getText();
                truck.setCompany(company != null && !company.trim().isEmpty() ? company.trim() : null);
                String status = statusCombo.getEditor().getText();
                truck.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Truck.STATUS_AVAILABLE);
                truck.setCurrentLocation(locationField.getText().trim());
                return truck;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(truck -> runAsync(() -> {
            truckService.addTruck(truck);
            refreshData();
        }, "Ciągnik dodany", "Nie udało się dodać ciągnika"));
    }
    
    // Shows the dialog for editing the selected truck.
    private void showEditTruckDialog() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();

        if (selectedTruck == null) {
            showAlert("Błąd", "Wybierz ciągnik do edycji", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Truck> dialog = new Dialog<>();
        dialog.setTitle("Edytuj ciągnik");
        dialog.setHeaderText("Ciągnik: " + selectedTruck.getBrand() + " (" + selectedTruck.getRegistrationNumber() + ")");

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField registrationField = new TextField(selectedTruck.getRegistrationNumber());
        TextField brandField = new TextField(selectedTruck.getBrand());

        ComboBox<String> companyCombo = createEditableComboBox(
                Truck.COMPANY_MTG, Truck.COMPANY_APA, Truck.COMPANY_ABSOLUT);
        if (selectedTruck.getCompany() != null) {
            companyCombo.setValue(selectedTruck.getCompany());
        }

        ComboBox<String> statusCombo = createEditableComboBox(
                Truck.STATUS_AVAILABLE, Truck.STATUS_ON_TRIP, Truck.STATUS_MAINTENANCE);
        statusCombo.setValue(selectedTruck.getStatus());

        TextField locationField = new TextField(
                selectedTruck.getCurrentLocation() != null ? selectedTruck.getCurrentLocation() : "");

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Numer rejestracyjny:"), registrationField,
            new Label("Marka:"), brandField,
            new Label("Firma:"), companyCombo,
            new Label("Status:"), statusCombo,
            new Label("Lokalizacja:"), locationField
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selectedTruck.setRegistrationNumber(registrationField.getText().trim());
                selectedTruck.setBrand(brandField.getText().trim());
                String company = companyCombo.getEditor().getText();
                selectedTruck.setCompany(company != null && !company.trim().isEmpty() ? company.trim() : null);
                String status = statusCombo.getEditor().getText();
                selectedTruck.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Truck.STATUS_AVAILABLE);
                selectedTruck.setCurrentLocation(locationField.getText().trim());
                return selectedTruck;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(truck -> runAsync(() -> {
            truckService.updateTruck(truck);
            refreshData();
        }, "Dane ciągnika zaktualizowane", "Nie udało się zaktualizować ciągnika"));
    }
    
    /**
     * Obsługuje usuwanie ciągnika
     */
    // Deletes the selected truck after confirmation.
    private void handleDeleteTruck() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Błąd", "Wybierz ciągnik do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz usunąć ten ciągnik?");
        confirmAlert.setContentText("Marka: " + selectedTruck.getBrand() + 
            "\nNr rej.: " + selectedTruck.getRegistrationNumber());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runAsync(() -> {
                    truckService.deleteTruck(selectedTruck.getId());
                    refreshData();
                }, "Ciągnik usunięty", "Nie udało się usunąć ciągnika");
            }
        });
    }
    
    /**
     * Wyświetla okno dialogowe zarządzania załącznikami PDF dla ciągnika
     */
    // Opens a dialog listing the truck's PDF attachments with add/edit/download/delete actions.
    private void showAttachmentsDialog() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Błąd", "Wybierz ciągnik", Alert.AlertType.WARNING);
            return;
        }

        selectedTruck = truckService.getTruckById(selectedTruck.getId()).orElse(selectedTruck);
        final Truck truck = selectedTruck;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Załączniki PDF");
        dialog.setHeaderText("Ciągnik: " + truck.getBrand() + " (" + truck.getRegistrationNumber() + ")");
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
        
        TableColumn<TruckAttachment, String> expCol = new TableColumn<>("Data ważności");
        expCol.setCellValueFactory(cellData -> {
            LocalDate exp = cellData.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? exp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "—");
        });
        expCol.setPrefWidth(120);

        attachmentTable.getColumns().addAll(idCol, nameCol, descCol, dateCol, expCol, sizeCol);

        attachmentTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TruckAttachment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getExpirationDate() == null) {
                    setStyle("");
                } else {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), item.getExpirationDate());
                    if (days <= 7) {
                        setStyle("-fx-background-color: #ffcdd2;");
                    } else if (days <= 30) {
                        setStyle("-fx-background-color: #fff9c4;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

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
        
        Button editDescBtn = new Button("Edytuj opis");
        editDescBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        editDescBtn.setOnAction(e -> {
            TruckAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                editAttachmentDescription(selected, attachmentList, truck);
            } else {
                showAlert("Błąd", "Wybierz załącznik", Alert.AlertType.WARNING);
            }
        });

        Button expDateBtn = new Button("Data ważności");
        expDateBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        expDateBtn.setOnAction(e -> {
            TruckAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                editExpirationDate(selected, attachmentList, truck);
            } else {
                showAlert("Błąd", "Wybierz załącznik", Alert.AlertType.WARNING);
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
        
        HBox buttonBox = new HBox(10, addBtn, downloadBtn, editDescBtn, expDateBtn, deleteBtn);
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
     * Dodaje nowy załącznik PDF do ciągnika
     */
    // Lets the user pick a file and attaches it (with description/expiry) to the truck.
    private void addAttachmentToTruck(Truck truck, ObservableList<TruckAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz pliki");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Wszystkie obsługiwane", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Obrazy", "*.jpg", "*.jpeg", "*.png")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(stage);
        
        if (files != null && !files.isEmpty()) {
            int added = 0;
            for (File file : files) {
                try {
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    TruckAttachment attachment = new TruckAttachment(file.getName(), "", fileData, truck);
                    attachmentRepository.save(attachment);
                    attachmentList.add(attachment);
                    added++;
                } catch (IOException e) {
                    showAlert("Błąd", "Nie udało się odczytać pliku: " + file.getName() + "\n" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            if (added > 0) {
                showAlert("Sukces", "Dodano plików: " + added, Alert.AlertType.INFORMATION);
            }
        }
    }
    
    // Edits the description of a truck attachment.
    private void editAttachmentDescription(TruckAttachment attachment, ObservableList<TruckAttachment> attachmentList, Truck truck) {
        TextInputDialog dlg = new TextInputDialog(attachment.getDescription() != null ? attachment.getDescription() : "");
        dlg.setTitle("Opis dokumentu");
        dlg.setHeaderText("Plik: " + attachment.getFilename());
        dlg.setContentText("Opis:");
        dlg.getEditor().setPrefWidth(350);
        
        dlg.showAndWait().ifPresent(text -> {
            try {
                attachment.setDescription(text.trim());
                attachmentRepository.save(attachment);
                attachmentList.setAll(
                    new java.util.ArrayList<>(truckService.getTruckById(truck.getId()).map(Truck::getAttachments).orElse(java.util.Set.of())));
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać opisu: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    // Edits the expiration date of a truck attachment.
    private void editExpirationDate(TruckAttachment attachment, ObservableList<TruckAttachment> attachmentList, Truck truck) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Data ważności dokumentu");
        dlg.setHeaderText("Plik: " + attachment.getFilename());

        ButtonType saveType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("Usuń datę", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, clearType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(attachment.getExpirationDate());
        VBox content = new VBox(10, new Label("Data ważności:"), datePicker);
        content.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(content);

        dlg.setResultConverter(btn -> {
            if (btn == saveType) return datePicker.getValue();
            if (btn == clearType) return LocalDate.MIN;
            return null;
        });

        dlg.showAndWait().ifPresent(date -> {
            try {
                attachment.setExpirationDate(date == LocalDate.MIN ? null : date);
                attachmentRepository.save(attachment);
                attachmentList.setAll(
                    new java.util.ArrayList<>(truckService.getTruckById(truck.getId()).map(Truck::getAttachments).orElse(java.util.Set.of())));
                refreshData();
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać daty: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Saves the selected attachment's file to disk.
    private void downloadAttachment(TruckAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik");
        fileChooser.setInitialFileName(attachment.getFilename());
        String ext = getExtension(attachment.getFilename());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(ext.toUpperCase() + " pliki", "*." + ext),
            new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*")
        );
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));

        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            file = ensureExtension(file, ext);
            try {
                byte[] data = attachmentRepository.findFileDataById(attachment.getId());
                Files.write(file.toPath(), data);
                showAlert("Sukces", "Plik został zapisany jako:\n" + file.getAbsolutePath(), 
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Usuwa załącznik ciągnika
     */
    // Deletes the selected attachment from the truck.
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
                    showAlert("Sukces", "Załącznik usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się usunąć załącznika: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Wyświetla okno dialogowe z komunikatem
     */
    // Returns a row style highlighting trucks whose documents are expired or expiring soon.
    private static String getExpirationStyle(java.util.Collection<TruckAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";
        long minDays = Long.MAX_VALUE;
        for (TruckAttachment a : attachments) {
            if (a.getExpirationDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), a.getExpirationDate());
                if (days < minDays) minDays = days;
            }
        }
        if (minDays == Long.MAX_VALUE) return "";
        if (minDays <= 7) return "-fx-background-color: #ffcdd2;";
        if (minDays <= 30) return "-fx-background-color: #fff9c4;";
        return "";
    }

    // Runs a blocking task on a background thread and shows a success/error alert on the FX thread.
    private void runAsync(Runnable task, String successMsg, String errorPrefix) {
        Thread.ofVirtual().start(() -> {
            try {
                task.run();
                javafx.application.Platform.runLater(() ->
                    showAlert("Sukces", successMsg, Alert.AlertType.INFORMATION));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    showAlert("Błąd", errorPrefix + ": " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    // Case-insensitive substring check used by the search filter.
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    // Returns the lowercase file extension, defaulting to "pdf" when none is present.
    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "pdf";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    // Ensures the saved file ends with the expected extension.
    private static java.io.File ensureExtension(java.io.File file, String ext) {
        String name = file.getName();
        if (name.contains(".") && name.toLowerCase().endsWith("." + ext.toLowerCase())) return file;
        if (!name.contains(".")) return new java.io.File(file.getParent(), name + "." + ext);
        return file;
    }

    // Shows a simple modal alert dialog with the given title and message.
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
