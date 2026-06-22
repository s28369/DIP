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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.fleetmanagement.model.*;
import org.example.fleetmanagement.repository.TripAttachmentRepository;
import org.example.fleetmanagement.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controller for the trips (active routes) management view: trip lifecycle, notes,
 * customers and PDF attachments.
 */
@Component
public class TripManagementController {
    
    private final TripService tripService;
    private final DriverService driverService;
    private final TruckService truckService;
    private final TrailerService trailerService;
    private final CustomerService customerService;
    private final TripAttachmentRepository attachmentRepository;
    private final ObservableList<Trip> tripList = FXCollections.observableArrayList();
    private FilteredList<Trip> filteredList;
    private VBox view;
    private TableView<Trip> tableView;
    private CheckBox showAllCheckbox;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // Constructor injection of all services and the attachment repository used by trips; builds the view.
    @Autowired
    public TripManagementController(TripService tripService, DriverService driverService,
                                    TruckService truckService, TrailerService trailerService,
                                    CustomerService customerService,
                                    TripAttachmentRepository attachmentRepository) {
        this.tripService = tripService;
        this.driverService = driverService;
        this.truckService = truckService;
        this.trailerService = trailerService;
        this.customerService = customerService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }
    
    // Builds the table, status legend, filters and action buttons for the trips screen.
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        
        Label titleLabel = new Label("Aktywne trasy");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        showAllCheckbox = new CheckBox("Pokaż wszystkie trasy (w tym zakończone)");
        showAllCheckbox.setOnAction(e -> refreshData());

        Button addButton = new Button("Nowa trasa");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddTripDialog());
        
        Button editButton = new Button("Edytuj");
        editButton.setOnAction(e -> showEditTripDialog());
        
        Button startButton = new Button("Rozpocznij trasę");
        startButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        startButton.setOnAction(e -> handleStartTrip());
        
        Button completeButton = new Button("Zakończ trasę");
        completeButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        completeButton.setOnAction(e -> handleCompleteTrip());
        
        Button cancelButton = new Button("Anuluj trasę");
        cancelButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> handleCancelTrip());
        
        Button notesButton = new Button("Uwagi");
        notesButton.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        notesButton.setOnAction(e -> showNotesDialog());
        
        Button attachmentsButton = new Button("Dokumenty PDF");
        attachmentsButton.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());
        
        Button customersButton = new Button("Klienci");
        customersButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        customersButton.setOnAction(e -> showCustomerManagementDialog());
        
        Button deleteButton = new Button("Usuń trasę");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteTrip());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });
        
        HBox buttonBox1 = new HBox(10, addButton, editButton, startButton, completeButton, cancelButton);
        HBox buttonBox2 = new HBox(10, notesButton, attachmentsButton, customersButton, deleteButton, refreshButton);
        VBox buttonContainer = new VBox(5, buttonBox1, buttonBox2);

        TextField searchField = new TextField();
        searchField.setPromptText("Wprowadź tekst do wyszukania...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Wszystko", "Ciągnik", "Naczepa", "Kierowca", "Miejsce wyjazdu", "Miejsce docelowe", "Ładunek", "Klient", "Status");
        searchParam.setValue("Wszystko");

        filteredList = new FilteredList<>(tripList, p -> true);

        Runnable applyFilter = () -> {
            String text = searchField.getText();
            String param = searchParam.getValue();
            if (text == null || text.trim().isEmpty()) {
                filteredList.setPredicate(p -> true);
                return;
            }
            String lower = text.trim().toLowerCase();
            filteredList.setPredicate(trip -> switch (param) {
                case "Ciągnik" -> containsTruck(trip.getTruck(), lower);
                case "Naczepa" -> containsTrailer(trip.getTrailer(), lower);
                case "Kierowca" -> trip.getDriver() != null && contains(trip.getDriver().getFullName(), lower);
                case "Miejsce wyjazdu" -> contains(trip.getOrigin(), lower);
                case "Miejsce docelowe" -> contains(trip.getDestination(), lower);
                case "Ładunek" -> contains(trip.getCargoDescription(), lower);
                case "Klient" -> trip.getCustomer() != null && contains(trip.getCustomer().getName(), lower);
                case "Status" -> contains(statusLabel(trip.getStatus()), lower);
                default -> containsTruck(trip.getTruck(), lower)
                        || containsTrailer(trip.getTrailer(), lower)
                        || (trip.getDriver() != null && contains(trip.getDriver().getFullName(), lower))
                        || contains(trip.getOrigin(), lower)
                        || contains(trip.getDestination(), lower)
                        || contains(trip.getCargoDescription(), lower)
                        || (trip.getCustomer() != null && contains(trip.getCustomer().getName(), lower))
                        || contains(statusLabel(trip.getStatus()), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Szukaj:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);
        
        TableColumn<Trip, String> truckCol = new TableColumn<>("Ciągnik");
        truckCol.setCellValueFactory(cellData -> {
            Truck truck = cellData.getValue().getTruck();
            return new SimpleStringProperty(truck != null ?
                truck.getBrand() + " (" + truck.getRegistrationNumber() + ")" : "-");
        });
        truckCol.setPrefWidth(170);
        
        TableColumn<Trip, String> trailerCol = new TableColumn<>("Naczepa");
        trailerCol.setCellValueFactory(cellData -> {
            Trailer trailer = cellData.getValue().getTrailer();
            return new SimpleStringProperty(trailer != null ?
                trailer.getBrand() + " (" + trailer.getRegistrationNumber() + ")" : "???");
        });
        trailerCol.setPrefWidth(170);
        
        TableColumn<Trip, String> driverCol = new TableColumn<>("Kierowca");
        driverCol.setCellValueFactory(cellData -> {
            Driver driver = cellData.getValue().getDriver();
            return new SimpleStringProperty(driver != null ? driver.getFullName() : "???");
        });
        driverCol.setPrefWidth(130);
        
        TableColumn<Trip, String> originCol = new TableColumn<>("Miejsce wyjazdu");
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));
        originCol.setPrefWidth(110);
        
        TableColumn<Trip, String> destCol = new TableColumn<>("Miejsce docelowe");
        destCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        destCol.setPrefWidth(110);
        
        TableColumn<Trip, String> cargoCol = new TableColumn<>("Ładunek");
        cargoCol.setCellValueFactory(new PropertyValueFactory<>("cargoDescription"));
        cargoCol.setPrefWidth(110);
        
        TableColumn<Trip, String> customerCol = new TableColumn<>("Klient");
        customerCol.setCellValueFactory(cellData -> {
            Customer cust = cellData.getValue().getCustomer();
            return new SimpleStringProperty(cust != null ? cust.getName() : "-");
        });
        customerCol.setPrefWidth(120);
        
        TableColumn<Trip, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            Trip.TripStatus st = cellData.getValue().getStatus();
            String label = switch (st) {
                case PLANNED -> "Zaplanowana";
                case IN_PROGRESS -> "W trakcie";
                case COMPLETED -> "Zakończona";
                case CANCELLED -> "Anulowana";
            };
            return new SimpleStringProperty(label);
        });
        statusCol.setPrefWidth(100);
        
        TableColumn<Trip, String> notesCol = new TableColumn<>("Uwagi");
        notesCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getNoteCount();
            return new SimpleStringProperty(count > 0 ? count + " szt." : "—");
        });
        notesCol.setPrefWidth(70);
        
        tableView.getColumns().addAll(truckCol, trailerCol, driverCol, originCol, destCol,
                cargoCol, customerCol, statusCol, notesCol);

        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Trip trip, boolean empty) {
                super.updateItem(trip, empty);
                if (empty || trip == null) {
                    setStyle("");
                } else {
                    String base;
                    String expStyle = getAttachmentExpirationStyle(trip.getAttachments());
                    if (!expStyle.isEmpty()) {
                        base = expStyle;
                    } else {
                        base = switch (trip.getStatus()) {
                            case PLANNED -> "-fx-background-color: #fff9c4;";
                            case IN_PROGRESS -> "-fx-background-color: #c8e6c9;";
                            case COMPLETED -> "-fx-background-color: #e0e0e0;";
                            case CANCELLED -> "-fx-background-color: #ffcdd2;";
                        };
                    }
                    if (isSelected()) {
                        setStyle(base + " -fx-text-fill: black; -fx-border-color: #2980b9; -fx-border-width: 2;");
                    } else {
                        setStyle(base);
                    }
                }
            }

            {
                selectedProperty().addListener((obs, wasSelected, isNowSelected) -> updateItem(getItem(), isEmpty()));
            }
        });

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                showAttachmentsDialog();
            }
        });

        HBox legend = new HBox(15);
        legend.getChildren().addAll(
            createLegendItem("Zaplanowana", "#fff9c4"),
            createLegendItem("W trakcie", "#c8e6c9"),
            createLegendItem("Zakończona", "#e0e0e0"),
            createLegendItem("Anulowana", "#ffcdd2")
        );
        legend.setPadding(new Insets(5, 0, 0, 0));
        
        view.getChildren().addAll(titleLabel, showAllCheckbox, buttonContainer, searchBox, tableView, legend);
        VBox.setVgrow(tableView, Priority.ALWAYS);
    }
    
    // Creates a colored legend label used to explain trip status colors.
    private Label createLegendItem(String text, String color) {
        Label label = new Label("■ " + text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        return label;
    }
    
    // Returns the root node of this view.
    public Parent getView() {
        return view;
    }
    
    // Reloads trips into the table (all trips or only active ones, depending on the checkbox).
    public void refreshData() {
        boolean showAll = showAllCheckbox.isSelected();
        var data = showAll ? tripService.getAllTrips() : tripService.getActiveTrips();
        if (javafx.application.Platform.isFxApplicationThread()) {
            tripList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> tripList.setAll(data));
        }
    }

    // ---- Add Trip ----
    
    // Shows the dialog for creating a new trip (selecting truck, trailer, driver and customer).
    private void showAddTripDialog() {
        Dialog<Trip> dialog = new Dialog<>();
        dialog.setTitle("Nowa trasa");
        dialog.setHeaderText("Utwórz nową trasę");
        
        ButtonType addButtonType = new ButtonType("Utwórz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        ComboBox<Truck> truckCombo = new ComboBox<>();
        truckCombo.getItems().addAll(truckService.getTrucksByStatus(Truck.STATUS_AVAILABLE));
        truckCombo.setConverter(truckConverter());
        truckCombo.setPrefWidth(300);

        ComboBox<Trailer> trailerCombo = new ComboBox<>();
        trailerCombo.getItems().addAll(trailerService.getTrailersByStatus(Trailer.STATUS_AVAILABLE));
        trailerCombo.setConverter(trailerConverter());
        trailerCombo.setPrefWidth(300);

        ComboBox<Driver> driverCombo = new ComboBox<>();
        driverCombo.getItems().addAll(driverService.getAvailableDrivers());
        driverCombo.setConverter(driverConverter());
        driverCombo.setPrefWidth(300);
        
        TextField originField = new TextField();
        originField.setPromptText("Miejsce wyjazdu");
        
        TextField destinationField = new TextField();
        destinationField.setPromptText("Miejsce docelowe");
        
        TextField cargoField = new TextField();
        cargoField.setPromptText("Opis ładunku");

        ComboBox<Customer> customerCombo = new ComboBox<>();
        customerCombo.getItems().addAll(customerService.getAllCustomers());
        customerCombo.setConverter(customerConverter());
        customerCombo.setPrefWidth(300);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Ciągnik:"), truckCombo,
            new Label("Naczepa (opcjonalnie):"), trailerCombo,
            new Label("Kierowca (opcjonalnie):"), driverCombo,
            new Label("Miejsce wyjazdu:"), originField,
            new Label("Miejsce docelowe:"), destinationField,
            new Label("Ładunek:"), cargoField,
            new Label("Klient:"), customerCombo
        );
        content.setPadding(new Insets(10));
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(450);
        
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefWidth(450);

        final Button createBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        createBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (truckCombo.getValue() == null) {
                showAlert("Błąd", "Wybierz ciągnik", Alert.AlertType.ERROR);
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
                trip.setTrailer(trailerCombo.getValue());
                trip.setDriver(driverCombo.getValue());
                trip.setCustomer(customerCombo.getValue());
                trip.setOrigin(originField.getText().trim());
                trip.setDestination(destinationField.getText().trim());
                trip.setCargoDescription(cargoField.getText().trim());
                trip.setStatus(Trip.TripStatus.PLANNED);
                return trip;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(trip -> runAsync(() -> {
            tripService.createTrip(trip);
            refreshData();
        }, "Trasa utworzona", "Nie udało się utworzyć trasy"));
    }

    // ---- Edit Trip ----

    // Shows the dialog for editing the selected trip.
    private void showEditTripDialog() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz trasę do edycji", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Trip> dialog = new Dialog<>();
        dialog.setTitle("Edytuj trasę");
        dialog.setHeaderText("Trasa: " + selected.getRouteDescription());

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField originField = new TextField(selected.getOrigin());
        TextField destinationField = new TextField(selected.getDestination());
        TextField cargoField = new TextField(selected.getCargoDescription() != null ? selected.getCargoDescription() : "");

        ComboBox<Customer> customerCombo = new ComboBox<>();
        customerCombo.getItems().addAll(customerService.getAllCustomers());
        customerCombo.setConverter(customerConverter());
        customerCombo.setPrefWidth(300);
        if (selected.getCustomer() != null) {
            customerCombo.setValue(selected.getCustomer());
        }

        VBox content = new VBox(10,
            new Label("Miejsce wyjazdu:"), originField,
            new Label("Miejsce docelowe:"), destinationField,
            new Label("Ładunek:"), cargoField,
            new Label("Klient:"), customerCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                selected.setOrigin(originField.getText().trim());
                selected.setDestination(destinationField.getText().trim());
                selected.setCargoDescription(cargoField.getText().trim());
                selected.setCustomer(customerCombo.getValue());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(trip -> runAsync(() -> {
            tripService.updateTrip(trip);
            refreshData();
        }, "Trasa zaktualizowana", "Nie udało się zaktualizować trasy"));
    }
    
    // ---- Trip lifecycle ----
    
    // Starts the selected planned trip.
    private void handleStartTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz trasę do rozpoczęcia", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() != Trip.TripStatus.PLANNED) {
            showAlert("Błąd", "Można rozpocząć tylko zaplanowaną trasę", Alert.AlertType.WARNING);
            return;
        }
        try {
            tripService.startTrip(selected.getId());
            refreshData();
            showAlert("Sukces", "Trasa rozpoczęta", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Błąd", "Nie udało się rozpocząć trasy: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    // Completes the selected in-progress trip and frees its resources.
    private void handleCompleteTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz trasę do zakończenia", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() != Trip.TripStatus.IN_PROGRESS) {
            showAlert("Błąd", "Można zakończyć tylko trasę w trakcie", Alert.AlertType.WARNING);
            return;
        }
        runAsync(() -> {
            tripService.completeTrip(selected.getId());
            refreshData();
        }, "Trasa zakończona", "Nie udało się zakończyć trasy");
    }
    
    // Cancels the selected trip after confirmation and frees its resources.
    private void handleCancelTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz trasę do anulowania", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() == Trip.TripStatus.COMPLETED ||
            selected.getStatus() == Trip.TripStatus.CANCELLED) {
            showAlert("Błąd", "Tej trasy nie można anulować", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz anulować tę trasę?");
        confirm.setContentText(selected.getRouteDescription());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runAsync(() -> {
                    tripService.cancelTrip(selected.getId());
                    refreshData();
                }, "Trasa anulowana", "Nie udało się anulować trasy");
            }
        });
    }
    
    // Deletes the selected trip after confirmation.
    private void handleDeleteTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz trasę do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć tę trasę?");
        confirm.setContentText(selected.getRouteDescription());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runAsync(() -> {
                    tripService.deleteTrip(selected.getId());
                    refreshData();
                }, "Trasa usunięta", "Nie udało się usunąć trasy");
            }
        });
    }

    // ---- Notes dialog (like TrailerManagementController) ----

    // Opens a dialog listing the trip's notes with add/edit/delete actions.
    private void showNotesDialog() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz trasę, aby zobaczyć uwagi", Alert.AlertType.WARNING);
            return;
        }

        ObservableList<TripNote> noteList = FXCollections.observableArrayList();
        noteList.addAll(tripService.getNotesByTrip(selected.getId()));

        TableView<TripNote> noteTable = new TableView<>();
        noteTable.setItems(noteList);

        TableColumn<TripNote, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TripNote, String> contentCol = new TableColumn<>("Treść uwagi");
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        contentCol.setPrefWidth(350);

        TableColumn<TripNote, String> dateCol = new TableColumn<>("Data utworzenia");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("—");
        });
        dateCol.setPrefWidth(140);

        noteTable.getColumns().addAll(idCol, contentCol, dateCol);

        Button addNoteBtn = new Button("Dodaj uwagę");
        addNoteBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addNoteBtn.setOnAction(e -> addNote(selected, noteList));

        Button editNoteBtn = new Button("Edytuj");
        editNoteBtn.setOnAction(e -> {
            TripNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editNote(sel, noteList, selected);
            } else {
                showAlert("Błąd", "Wybierz uwagę do edycji", Alert.AlertType.WARNING);
            }
        });

        Button deleteNoteBtn = new Button("Usuń");
        deleteNoteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteNoteBtn.setOnAction(e -> {
            TripNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteNote(sel, noteList, selected);
            } else {
                showAlert("Błąd", "Wybierz uwagę do usunięcia", Alert.AlertType.WARNING);
            }
        });

        HBox noteBtnBox = new HBox(10, addNoteBtn, editNoteBtn, deleteNoteBtn);
        noteBtnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Uwagi trasy: " + selected.getRouteDescription()),
            noteTable,
            noteBtnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(noteTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Uwagi trasy");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();

        refreshData();
    }

    // Adds a new note to the trip.
    private void addNote(Trip trip, ObservableList<TripNote> noteList) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nowa uwaga");
        dlg.setHeaderText("Wprowadź treść uwagi");
        dlg.setContentText("Uwaga:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    TripNote note = new TripNote(text.trim(), trip);
                    tripService.addNote(note);
                    noteList.setAll(tripService.getNotesByTrip(trip.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się dodać uwagi: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // Edits an existing trip note.
    private void editNote(TripNote note, ObservableList<TripNote> noteList, Trip trip) {
        TextInputDialog dlg = new TextInputDialog(note.getContent());
        dlg.setTitle("Edytuj uwagę");
        dlg.setHeaderText("Zmień treść uwagi");
        dlg.setContentText("Uwaga:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    note.setContent(text.trim());
                    tripService.updateNote(note);
                    noteList.setAll(tripService.getNotesByTrip(trip.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się zaktualizować uwagi: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // Deletes a trip note.
    private void deleteNote(TripNote note, ObservableList<TripNote> noteList, Trip trip) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Usunąć tę uwagę?");
        confirm.setContentText(note.getContent());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tripService.deleteNote(note.getId());
                    noteList.setAll(tripService.getNotesByTrip(trip.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się usunąć uwagi: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Customer management dialog ----

    // Opens a dialog to manage the list of customers (add/edit/delete).
    private void showCustomerManagementDialog() {
        ObservableList<Customer> customerList = FXCollections.observableArrayList();
        customerList.addAll(customerService.getAllCustomers());

        TableView<Customer> customerTable = new TableView<>();
        customerTable.setItems(customerList);

        TableColumn<Customer, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Nazwa klienta");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(350);

        customerTable.getColumns().addAll(idCol, nameCol);

        Button addBtn = new Button("Dodaj");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Nowy klient");
            dlg.setHeaderText("Wprowadź nazwę klienta");
            dlg.setContentText("Nazwa:");
            dlg.getEditor().setPrefWidth(300);
            dlg.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        customerService.addCustomer(new Customer(name.trim()));
                        customerList.setAll(customerService.getAllCustomers());
                    } catch (Exception ex) {
                        showAlert("Błąd", "Nie udało się dodać klienta: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        Button editBtn = new Button("Edytuj");
        editBtn.setOnAction(e -> {
            Customer sel = customerTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Błąd", "Wybierz klienta", Alert.AlertType.WARNING);
                return;
            }
            TextInputDialog dlg = new TextInputDialog(sel.getName());
            dlg.setTitle("Edytuj klienta");
            dlg.setHeaderText("Zmień nazwę klienta");
            dlg.setContentText("Nazwa:");
            dlg.getEditor().setPrefWidth(300);
            dlg.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        sel.setName(name.trim());
                        customerService.updateCustomer(sel);
                        customerList.setAll(customerService.getAllCustomers());
                    } catch (Exception ex) {
                        showAlert("Błąd", "Nie udało się zaktualizować klienta: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        Button deleteBtn = new Button("Usuń");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            Customer sel = customerTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Błąd", "Wybierz klienta do usunięcia", Alert.AlertType.WARNING);
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Potwierdzenie");
            confirm.setHeaderText("Usunąć klienta?");
            confirm.setContentText(sel.getName());
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        customerService.deleteCustomer(sel.getId());
                        customerList.setAll(customerService.getAllCustomers());
                    } catch (Exception ex) {
                        showAlert("Błąd", "Nie udało się usunąć klienta: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        HBox btnBox = new HBox(10, addBtn, editBtn, deleteBtn);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Zarządzanie klientami"),
            customerTable,
            btnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(customerTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Klienci");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();
    }

    // ---- Attachments dialog (kept from original) ----
    
    // Opens a dialog listing the trip's PDF attachments with add/edit/download/delete actions.
    private void showAttachmentsDialog() {
        Trip selectedTrip = tableView.getSelectionModel().getSelectedItem();
        if (selectedTrip == null) {
            showAlert("Błąd", "Wybierz trasę", Alert.AlertType.WARNING);
            return;
        }

        selectedTrip = tripService.getTripById(selectedTrip.getId()).orElse(selectedTrip);
        final Trip trip = selectedTrip;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dokumenty PDF trasy");
        dialog.setHeaderText("Trasa: " + trip.getRouteDescription() +
            "\nKierowca: " + trip.getDriver().getFullName() +
            "\nCiągnik: " + trip.getTruck().getRegistrationNumber());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TripAttachment> attachmentTable = new TableView<>();
        ObservableList<TripAttachment> attachmentList = FXCollections.observableArrayList(trip.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);
        
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
                return new SimpleStringProperty(cellData.getValue().getUploadedAt().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);
        
        TableColumn<TripAttachment, String> expCol = new TableColumn<>("Data ważności");
        expCol.setCellValueFactory(cellData -> {
            LocalDate exp = cellData.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? exp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "—");
        });
        expCol.setPrefWidth(120);

        attachmentTable.getColumns().addAll(nameCol, descCol, sizeCol, dateCol, expCol);

        attachmentTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TripAttachment item, boolean empty) {
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
            addAttachmentToTrip(trip, attachmentList);
            refreshData();
        });
        
        Button downloadBtn = new Button("Pobierz PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TripAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                downloadAttachment(sel);
            } else {
                showAlert("Błąd", "Wybierz załącznik do pobrania", Alert.AlertType.WARNING);
            }
        });
        
        Button editDescBtn = new Button("Zmień opis");
        editDescBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        editDescBtn.setOnAction(e -> {
            TripAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editAttachmentDescription(sel, attachmentList, trip);
            } else {
                showAlert("Błąd", "Wybierz załącznik", Alert.AlertType.WARNING);
            }
        });
        
        Button expDateBtn = new Button("Data ważności");
        expDateBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        expDateBtn.setOnAction(e -> {
            TripAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editTripExpirationDate(sel, attachmentList, trip);
            } else {
                showAlert("Błąd", "Wybierz załącznik", Alert.AlertType.WARNING);
            }
        });
        
        Button delBtn = new Button("Usuń załącznik");
        delBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        delBtn.setOnAction(e -> {
            TripAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteAttachment(trip, sel, attachmentList);
                refreshData();
            } else {
                showAlert("Błąd", "Wybierz załącznik do usunięcia", Alert.AlertType.WARNING);
            }
        });
        
        HBox buttonBox = new HBox(10, addBtn, downloadBtn, editDescBtn, expDateBtn, delBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox content = new VBox(10,
            new Label("Lista dokumentów PDF trasy:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();
    }
    
    // Lets the user pick a file and attaches it (with description/expiry) to the trip.
    private void addAttachmentToTrip(Trip trip, ObservableList<TripAttachment> attachmentList) {
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
                    TripAttachment attachment = new TripAttachment(file.getName(), "", fileData, trip);
                    attachmentRepository.save(attachment);
                    attachmentList.add(attachment);
                    added++;
                } catch (IOException e) {
                    showAlert("Błąd", "Nie udało się odczytać pliku: " + file.getName() + "\n" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            if (added > 0) {
                showAlert("Sukces", "Dodano dokumentów: " + added, Alert.AlertType.INFORMATION);
            }
        }
    }
    
    // Edits the description of a trip attachment.
    private void editAttachmentDescription(TripAttachment attachment, ObservableList<TripAttachment> attachmentList, Trip trip) {
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
                    new java.util.ArrayList<>(tripService.getTripById(trip.getId()).map(Trip::getAttachments).orElse(java.util.Set.of())));
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać opisu: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    // Edits the expiration date of a trip attachment.
    private void editTripExpirationDate(TripAttachment attachment, ObservableList<TripAttachment> attachmentList, Trip trip) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Data ważności dokumentu");
        dlg.setHeaderText("Plik: " + attachment.getFilename());

        ButtonType saveType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("Usuń datę", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, clearType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(attachment.getExpirationDate());
        VBox dpContent = new VBox(10, new Label("Data ważności:"), datePicker);
        dpContent.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(dpContent);

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
                    new java.util.ArrayList<>(tripService.getTripById(trip.getId()).map(Trip::getAttachments).orElse(java.util.Set.of())));
                refreshData();
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać daty: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Saves the selected attachment's file to disk.
    private void downloadAttachment(TripAttachment attachment) {
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
                showAlert("Sukces", "Dokument zapisano jako:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    // Deletes the selected attachment from the trip.
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
                    showAlert("Sukces", "Dokument usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się usunąć dokumentu: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Converters ----

    // Combo-box converter rendering a Truck as a human-readable label.
    private StringConverter<Truck> truckConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Truck t) {
                return t != null ? t.getBrand() + " (" + t.getRegistrationNumber() + ")" : "";
            }
            @Override
            public Truck fromString(String s) { return null; }
        };
    }

    // Combo-box converter rendering a Trailer as a human-readable label.
    private StringConverter<Trailer> trailerConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Trailer t) {
                return t != null ? t.getBrand() + " (" + t.getRegistrationNumber() + ")" : "";
            }
            @Override
            public Trailer fromString(String s) { return null; }
        };
    }

    // Combo-box converter rendering a Driver as a human-readable label.
    private StringConverter<Driver> driverConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Driver d) {
                return d != null ? d.getFullName() : "";
            }
            @Override
            public Driver fromString(String s) { return null; }
        };
    }

    // Combo-box converter rendering a Customer as a human-readable label.
    private StringConverter<Customer> customerConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                return c != null ? c.getName() : "";
            }
            @Override
            public Customer fromString(String s) { return null; }
        };
    }
    
    // Returns a style highlighting trips whose attachments are expired or expiring soon.
    private static String getAttachmentExpirationStyle(java.util.Collection<TripAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";
        long minDays = Long.MAX_VALUE;
        for (TripAttachment a : attachments) {
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

    // Case-insensitive substring check used by the search filter.
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    // Returns true if the truck matches the search text (by registration or brand).
    private static boolean containsTruck(Truck truck, String search) {
        return truck != null && (contains(truck.getBrand(), search) || contains(truck.getRegistrationNumber(), search));
    }

    // Returns true if the trailer matches the search text.
    private static boolean containsTrailer(Trailer trailer, String search) {
        return trailer != null && (contains(trailer.getBrand(), search) || contains(trailer.getRegistrationNumber(), search));
    }

    // Maps a trip status enum to its Polish display label.
    private static String statusLabel(Trip.TripStatus st) {
        return switch (st) {
            case PLANNED -> "Zaplanowana";
            case IN_PROGRESS -> "W trakcie";
            case COMPLETED -> "Zakończona";
            case CANCELLED -> "Anulowana";
        };
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
