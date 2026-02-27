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
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        
        Label titleLabel = new Label("Активные рейсы");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        showAllCheckbox = new CheckBox("Показать все рейсы (включая завершённые)");
        showAllCheckbox.setOnAction(e -> refreshData());

        Button addButton = new Button("Новый рейс");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddTripDialog());
        
        Button editButton = new Button("Редактировать");
        editButton.setOnAction(e -> showEditTripDialog());
        
        Button startButton = new Button("Начать рейс");
        startButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        startButton.setOnAction(e -> handleStartTrip());
        
        Button completeButton = new Button("Завершить рейс");
        completeButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        completeButton.setOnAction(e -> handleCompleteTrip());
        
        Button cancelButton = new Button("Отменить рейс");
        cancelButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> handleCancelTrip());
        
        Button notesButton = new Button("Заметки");
        notesButton.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        notesButton.setOnAction(e -> showNotesDialog());
        
        Button attachmentsButton = new Button("Документы PDF");
        attachmentsButton.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());
        
        Button customersButton = new Button("Заказчики");
        customersButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        customersButton.setOnAction(e -> showCustomerManagementDialog());
        
        Button deleteButton = new Button("Удалить рейс");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteTrip());
        
        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(e -> refreshData());
        
        HBox buttonBox1 = new HBox(10, addButton, editButton, startButton, completeButton, cancelButton);
        HBox buttonBox2 = new HBox(10, notesButton, attachmentsButton, customersButton, deleteButton, refreshButton);
        VBox buttonContainer = new VBox(5, buttonBox1, buttonBox2);

        TextField searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Все", "Машина", "Прицеп", "Водитель", "Откуда", "Куда", "Товар", "Заказчик", "Статус");
        searchParam.setValue("Все");

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
                case "Машина" -> containsTruck(trip.getTruck(), lower);
                case "Прицеп" -> containsTrailer(trip.getTrailer(), lower);
                case "Водитель" -> trip.getDriver() != null && contains(trip.getDriver().getFullName(), lower);
                case "Откуда" -> contains(trip.getOrigin(), lower);
                case "Куда" -> contains(trip.getDestination(), lower);
                case "Товар" -> contains(trip.getCargoDescription(), lower);
                case "Заказчик" -> trip.getCustomer() != null && contains(trip.getCustomer().getName(), lower);
                case "Статус" -> contains(statusLabel(trip.getStatus()), lower);
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

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);
        
        TableColumn<Trip, String> truckCol = new TableColumn<>("Машина");
        truckCol.setCellValueFactory(cellData -> {
            Truck truck = cellData.getValue().getTruck();
            return new SimpleStringProperty(truck != null ?
                truck.getBrand() + " (" + truck.getRegistrationNumber() + ")" : "-");
        });
        truckCol.setPrefWidth(170);
        
        TableColumn<Trip, String> trailerCol = new TableColumn<>("Прицеп");
        trailerCol.setCellValueFactory(cellData -> {
            Trailer trailer = cellData.getValue().getTrailer();
            return new SimpleStringProperty(trailer != null ?
                trailer.getBrand() + " (" + trailer.getRegistrationNumber() + ")" : "-");
        });
        trailerCol.setPrefWidth(170);
        
        TableColumn<Trip, String> driverCol = new TableColumn<>("Водитель");
        driverCol.setCellValueFactory(cellData -> {
            Driver driver = cellData.getValue().getDriver();
            return new SimpleStringProperty(driver != null ? driver.getFullName() : "-");
        });
        driverCol.setPrefWidth(130);
        
        TableColumn<Trip, String> originCol = new TableColumn<>("Откуда");
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));
        originCol.setPrefWidth(110);
        
        TableColumn<Trip, String> destCol = new TableColumn<>("Куда");
        destCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        destCol.setPrefWidth(110);
        
        TableColumn<Trip, String> cargoCol = new TableColumn<>("Товар");
        cargoCol.setCellValueFactory(new PropertyValueFactory<>("cargoDescription"));
        cargoCol.setPrefWidth(110);
        
        TableColumn<Trip, String> customerCol = new TableColumn<>("Заказчик");
        customerCol.setCellValueFactory(cellData -> {
            Customer cust = cellData.getValue().getCustomer();
            return new SimpleStringProperty(cust != null ? cust.getName() : "-");
        });
        customerCol.setPrefWidth(120);
        
        TableColumn<Trip, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> {
            Trip.TripStatus st = cellData.getValue().getStatus();
            String label = switch (st) {
                case PLANNED -> "Запланирован";
                case IN_PROGRESS -> "В пути";
                case COMPLETED -> "Завершён";
                case CANCELLED -> "Отменён";
            };
            return new SimpleStringProperty(label);
        });
        statusCol.setPrefWidth(100);
        
        TableColumn<Trip, String> notesCol = new TableColumn<>("Заметки");
        notesCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getNoteCount();
            return new SimpleStringProperty(count > 0 ? count + " шт." : "—");
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
            createLegendItem("Запланирован", "#fff9c4"),
            createLegendItem("В пути", "#c8e6c9"),
            createLegendItem("Завершён", "#e0e0e0"),
            createLegendItem("Отменён", "#ffcdd2")
        );
        legend.setPadding(new Insets(5, 0, 0, 0));
        
        view.getChildren().addAll(titleLabel, showAllCheckbox, buttonContainer, searchBox, tableView, legend);
        VBox.setVgrow(tableView, Priority.ALWAYS);
    }
    
    private Label createLegendItem(String text, String color) {
        Label label = new Label("■ " + text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
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

    // ---- Add Trip ----
    
    private void showAddTripDialog() {
        Dialog<Trip> dialog = new Dialog<>();
        dialog.setTitle("Новый рейс");
        dialog.setHeaderText("Создать новый рейс");
        
        ButtonType addButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
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
        originField.setPromptText("Пункт отправления");
        
        TextField destinationField = new TextField();
        destinationField.setPromptText("Пункт назначения");
        
        TextField cargoField = new TextField();
        cargoField.setPromptText("Описание товара");

        ComboBox<Customer> customerCombo = new ComboBox<>();
        customerCombo.getItems().addAll(customerService.getAllCustomers());
        customerCombo.setConverter(customerConverter());
        customerCombo.setPrefWidth(300);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Машина:"), truckCombo,
            new Label("Прицеп (необязательно):"), trailerCombo,
            new Label("Водитель:"), driverCombo,
            new Label("Откуда:"), originField,
            new Label("Куда:"), destinationField,
            new Label("Товар:"), cargoField,
            new Label("Заказчик:"), customerCombo
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
                showAlert("Ошибка", "Выберите машину", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (driverCombo.getValue() == null) {
                showAlert("Ошибка", "Выберите водителя", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (originField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Укажите пункт отправления", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (destinationField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Укажите пункт назначения", Alert.AlertType.ERROR);
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
        
        dialog.showAndWait().ifPresent(trip -> {
            try {
                tripService.createTrip(trip);
                refreshData();
                showAlert("Успех", "Рейс создан", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось создать рейс: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // ---- Edit Trip ----

    private void showEditTripDialog() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите рейс для редактирования", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Trip> dialog = new Dialog<>();
        dialog.setTitle("Редактировать рейс");
        dialog.setHeaderText("Рейс: " + selected.getRouteDescription());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
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
            new Label("Откуда:"), originField,
            new Label("Куда:"), destinationField,
            new Label("Товар:"), cargoField,
            new Label("Заказчик:"), customerCombo
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

        dialog.showAndWait().ifPresent(trip -> {
            try {
                tripService.updateTrip(trip);
                refreshData();
                showAlert("Успех", "Рейс обновлён", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось обновить рейс: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    // ---- Trip lifecycle ----
    
    private void handleStartTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите рейс для начала", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() != Trip.TripStatus.PLANNED) {
            showAlert("Ошибка", "Можно начать только запланированный рейс", Alert.AlertType.WARNING);
            return;
        }
        try {
            tripService.startTrip(selected.getId());
            refreshData();
            showAlert("Успех", "Рейс начат", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось начать рейс: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void handleCompleteTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите рейс для завершения", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() != Trip.TripStatus.IN_PROGRESS) {
            showAlert("Ошибка", "Можно завершить только рейс в пути", Alert.AlertType.WARNING);
            return;
        }
        try {
            tripService.completeTrip(selected.getId());
            refreshData();
            showAlert("Успех", "Рейс завершён", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось завершить рейс: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void handleCancelTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите рейс для отмены", Alert.AlertType.WARNING);
            return;
        }
        if (selected.getStatus() == Trip.TripStatus.COMPLETED ||
            selected.getStatus() == Trip.TripStatus.CANCELLED) {
            showAlert("Ошибка", "Этот рейс не может быть отменён", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Вы уверены, что хотите отменить этот рейс?");
        confirm.setContentText(selected.getRouteDescription());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tripService.cancelTrip(selected.getId());
                    refreshData();
                    showAlert("Успех", "Рейс отменён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось отменить рейс: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void handleDeleteTrip() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите рейс для удаления", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Вы уверены, что хотите удалить этот рейс?");
        confirm.setContentText(selected.getRouteDescription());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tripService.deleteTrip(selected.getId());
                    refreshData();
                    showAlert("Успех", "Рейс удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить рейс: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Notes dialog (like TrailerManagementController) ----

    private void showNotesDialog() {
        Trip selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите рейс для просмотра заметок", Alert.AlertType.WARNING);
            return;
        }

        ObservableList<TripNote> noteList = FXCollections.observableArrayList();
        noteList.addAll(tripService.getNotesByTrip(selected.getId()));

        TableView<TripNote> noteTable = new TableView<>();
        noteTable.setItems(noteList);

        TableColumn<TripNote, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TripNote, String> contentCol = new TableColumn<>("Текст заметки");
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        contentCol.setPrefWidth(350);

        TableColumn<TripNote, String> dateCol = new TableColumn<>("Дата создания");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("—");
        });
        dateCol.setPrefWidth(140);

        noteTable.getColumns().addAll(idCol, contentCol, dateCol);

        Button addNoteBtn = new Button("Добавить заметку");
        addNoteBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addNoteBtn.setOnAction(e -> addNote(selected, noteList));

        Button editNoteBtn = new Button("Редактировать");
        editNoteBtn.setOnAction(e -> {
            TripNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editNote(sel, noteList, selected);
            } else {
                showAlert("Ошибка", "Выберите заметку для редактирования", Alert.AlertType.WARNING);
            }
        });

        Button deleteNoteBtn = new Button("Удалить");
        deleteNoteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteNoteBtn.setOnAction(e -> {
            TripNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteNote(sel, noteList, selected);
            } else {
                showAlert("Ошибка", "Выберите заметку для удаления", Alert.AlertType.WARNING);
            }
        });

        HBox noteBtnBox = new HBox(10, addNoteBtn, editNoteBtn, deleteNoteBtn);
        noteBtnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Заметки рейса: " + selected.getRouteDescription()),
            noteTable,
            noteBtnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(noteTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Заметки рейса");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();

        refreshData();
    }

    private void addNote(Trip trip, ObservableList<TripNote> noteList) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Новая заметка");
        dlg.setHeaderText("Введите текст заметки");
        dlg.setContentText("Заметка:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    TripNote note = new TripNote(text.trim(), trip);
                    tripService.addNote(note);
                    noteList.setAll(tripService.getNotesByTrip(trip.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось добавить заметку: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void editNote(TripNote note, ObservableList<TripNote> noteList, Trip trip) {
        TextInputDialog dlg = new TextInputDialog(note.getContent());
        dlg.setTitle("Редактировать заметку");
        dlg.setHeaderText("Измените текст заметки");
        dlg.setContentText("Заметка:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    note.setContent(text.trim());
                    tripService.updateNote(note);
                    noteList.setAll(tripService.getNotesByTrip(trip.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось обновить заметку: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deleteNote(TripNote note, ObservableList<TripNote> noteList, Trip trip) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить эту заметку?");
        confirm.setContentText(note.getContent());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    tripService.deleteNote(note.getId());
                    noteList.setAll(tripService.getNotesByTrip(trip.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить заметку: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Customer management dialog ----

    private void showCustomerManagementDialog() {
        ObservableList<Customer> customerList = FXCollections.observableArrayList();
        customerList.addAll(customerService.getAllCustomers());

        TableView<Customer> customerTable = new TableView<>();
        customerTable.setItems(customerList);

        TableColumn<Customer, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Название заказчика");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(350);

        customerTable.getColumns().addAll(idCol, nameCol);

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Новый заказчик");
            dlg.setHeaderText("Введите название заказчика");
            dlg.setContentText("Название:");
            dlg.getEditor().setPrefWidth(300);
            dlg.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        customerService.addCustomer(new Customer(name.trim()));
                        customerList.setAll(customerService.getAllCustomers());
                    } catch (Exception ex) {
                        showAlert("Ошибка", "Не удалось добавить заказчика: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setOnAction(e -> {
            Customer sel = customerTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Ошибка", "Выберите заказчика", Alert.AlertType.WARNING);
                return;
            }
            TextInputDialog dlg = new TextInputDialog(sel.getName());
            dlg.setTitle("Редактировать заказчика");
            dlg.setHeaderText("Измените название заказчика");
            dlg.setContentText("Название:");
            dlg.getEditor().setPrefWidth(300);
            dlg.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        sel.setName(name.trim());
                        customerService.updateCustomer(sel);
                        customerList.setAll(customerService.getAllCustomers());
                    } catch (Exception ex) {
                        showAlert("Ошибка", "Не удалось обновить заказчика: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            Customer sel = customerTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Ошибка", "Выберите заказчика для удаления", Alert.AlertType.WARNING);
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Подтверждение");
            confirm.setHeaderText("Удалить заказчика?");
            confirm.setContentText(sel.getName());
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        customerService.deleteCustomer(sel.getId());
                        customerList.setAll(customerService.getAllCustomers());
                    } catch (Exception ex) {
                        showAlert("Ошибка", "Не удалось удалить заказчика: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        HBox btnBox = new HBox(10, addBtn, editBtn, deleteBtn);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Управление заказчиками"),
            customerTable,
            btnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(customerTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Заказчики");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();
    }

    // ---- Attachments dialog (kept from original) ----
    
    private void showAttachmentsDialog() {
        Trip selectedTrip = tableView.getSelectionModel().getSelectedItem();
        if (selectedTrip == null) {
            showAlert("Ошибка", "Выберите рейс", Alert.AlertType.WARNING);
            return;
        }

        selectedTrip = tripService.getTripById(selectedTrip.getId()).orElse(selectedTrip);
        final Trip trip = selectedTrip;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Документы PDF рейса");
        dialog.setHeaderText("Рейс: " + trip.getRouteDescription() +
            "\nВодитель: " + trip.getDriver().getFullName() +
            "\nМашина: " + trip.getTruck().getRegistrationNumber());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TripAttachment> attachmentTable = new TableView<>();
        ObservableList<TripAttachment> attachmentList = FXCollections.observableArrayList(trip.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);
        
        TableColumn<TripAttachment, String> nameCol = new TableColumn<>("Имя файла");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);
        
        TableColumn<TripAttachment, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);
        
        TableColumn<TripAttachment, String> sizeCol = new TableColumn<>("Размер");
        sizeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);
        
        TableColumn<TripAttachment, String> dateCol = new TableColumn<>("Дата добавления");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getUploadedAt().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);
        
        attachmentTable.getColumns().addAll(nameCol, descCol, sizeCol, dateCol);

        Button addBtn = new Button("Добавить PDF");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            addAttachmentToTrip(trip, attachmentList);
            refreshData();
        });
        
        Button downloadBtn = new Button("Скачать PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TripAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                downloadAttachment(sel);
            } else {
                showAlert("Ошибка", "Выберите вложение для скачивания", Alert.AlertType.WARNING);
            }
        });
        
        Button delBtn = new Button("Удалить вложение");
        delBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        delBtn.setOnAction(e -> {
            TripAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteAttachment(trip, sel, attachmentList);
                refreshData();
            } else {
                showAlert("Ошибка", "Выберите вложение для удаления", Alert.AlertType.WARNING);
            }
        });
        
        HBox buttonBox = new HBox(10, addBtn, downloadBtn, delBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox content = new VBox(10,
            new Label("Список документов PDF рейса:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();
    }
    
    private void addAttachmentToTrip(Trip trip, ObservableList<TripAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            TextInputDialog descDialog = new TextInputDialog();
            descDialog.setTitle("Описание документа");
            descDialog.setHeaderText("Добавьте описание документа (необязательно)");
            descDialog.setContentText("Описание:");
            String description = descDialog.showAndWait().orElse("");
            
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                TripAttachment attachment = new TripAttachment(file.getName(), description, fileData, trip);
                attachmentRepository.save(attachment);
                attachmentList.add(attachment);
                showAlert("Успех", "Документ '" + file.getName() + "' добавлен", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось прочитать файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private void downloadAttachment(TripAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить документ PDF");
        fileChooser.setInitialFileName(attachment.getFilename());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf")
        );
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.write(file.toPath(), attachment.getFileData());
                showAlert("Успех", "Документ сохранён как:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось сохранить файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private void deleteAttachment(Trip trip, TripAttachment attachment, ObservableList<TripAttachment> attachmentList) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Вы уверены, что хотите удалить этот документ?");
        confirmAlert.setContentText("Файл: " + attachment.getFilename());
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    attachmentRepository.delete(attachment);
                    attachmentList.remove(attachment);
                    showAlert("Успех", "Документ удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить документ: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Converters ----

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
    
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private static boolean containsTruck(Truck truck, String search) {
        return truck != null && (contains(truck.getBrand(), search) || contains(truck.getRegistrationNumber(), search));
    }

    private static boolean containsTrailer(Trailer trailer, String search) {
        return trailer != null && (contains(trailer.getBrand(), search) || contains(trailer.getRegistrationNumber(), search));
    }

    private static String statusLabel(Trip.TripStatus st) {
        return switch (st) {
            case PLANNED -> "Запланирован";
            case IN_PROGRESS -> "В пути";
            case COMPLETED -> "Завершён";
            case CANCELLED -> "Отменён";
        };
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
