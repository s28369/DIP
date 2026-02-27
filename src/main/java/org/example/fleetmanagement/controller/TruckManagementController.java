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
import java.time.format.DateTimeFormatter;

/**
 * Контроллер управления представлением автопарка грузовиков
 */
@Component
public class TruckManagementController {
    
    private final TruckService truckService;
    private final TruckAttachmentRepository attachmentRepository;
    private final ObservableList<Truck> truckList = FXCollections.observableArrayList();
    private FilteredList<Truck> filteredList;
    private VBox view;
    private TableView<Truck> tableView;
    
    @Autowired
    public TruckManagementController(TruckService truckService, TruckAttachmentRepository attachmentRepository) {
        this.truckService = truckService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }
    
    /**
     * Инициализирует представление управления грузовиками
     */
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Управление автопарком");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Добавить грузовик");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddTruckDialog());

        Button editButton = new Button("Редактировать");
        editButton.setOnAction(e -> showEditTruckDialog());

        Button attachmentsButton = new Button("Вложения PDF");
        attachmentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());

        Button deleteButton = new Button("Удалить грузовик");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteTruck());

        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(e -> refreshData());

        HBox buttonBox = new HBox(10, addButton, editButton, attachmentsButton, deleteButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Все", "Рег. номер", "Марка", "Страна", "Статус", "Местоположение");
        searchParam.setValue("Все");

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
                case "Рег. номер" -> contains(truck.getRegistrationNumber(), lower);
                case "Марка" -> contains(truck.getBrand(), lower);
                case "Страна" -> contains(truck.getRegistrationCountry(), lower);
                case "Статус" -> contains(truck.getStatus(), lower);
                case "Местоположение" -> contains(truck.getCurrentLocation(), lower);
                default -> contains(truck.getRegistrationNumber(), lower)
                        || contains(truck.getBrand(), lower)
                        || contains(truck.getRegistrationCountry(), lower)
                        || contains(truck.getStatus(), lower)
                        || contains(truck.getCurrentLocation(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);

        TableColumn<Truck, String> registrationColumn = new TableColumn<>("Регистрационный номер");
        registrationColumn.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        registrationColumn.setPrefWidth(170);

        TableColumn<Truck, String> brandColumn = new TableColumn<>("Марка");
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        brandColumn.setPrefWidth(180);

        TableColumn<Truck, String> countryColumn = new TableColumn<>("Страна регистрации");
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("registrationCountry"));
        countryColumn.setPrefWidth(150);

        TableColumn<Truck, String> statusColumn = new TableColumn<>("Статус");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(120);

        TableColumn<Truck, String> locationColumn = new TableColumn<>("Местоположение");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("currentLocation"));
        locationColumn.setPrefWidth(180);

        tableView.getColumns().addAll(registrationColumn, brandColumn, countryColumn, statusColumn, locationColumn);

        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Возвращает представление контроллера
     */
    public Parent getView() {
        return view;
    }
    
    /**
     * Обновляет данные в таблице
     */
    public void refreshData() {
        truckList.clear();
        truckList.addAll(truckService.getAllTrucks());
    }
    
    /**
     * Отображает диалог добавления нового грузовика
     */
    private ComboBox<String> createEditableComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setEditable(true);
        combo.setPrefWidth(300);
        return combo;
    }

    private void showAddTruckDialog() {
        Dialog<Truck> dialog = new Dialog<>();
        dialog.setTitle("Добавить грузовик");
        dialog.setHeaderText("Введите данные нового грузовика");

        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField registrationField = new TextField();
        registrationField.setPromptText("Рег. номер (напр. WW12345)");

        TextField brandField = new TextField();
        brandField.setPromptText("Марка (напр. Volvo FH16)");

        ComboBox<String> countryCombo = createEditableComboBox(
                "Польша", "Беларусь", "Чехия", "РФ");

        ComboBox<String> statusCombo = createEditableComboBox(
                Truck.STATUS_AVAILABLE, Truck.STATUS_ON_TRIP, Truck.STATUS_MAINTENANCE);
        statusCombo.setValue(Truck.STATUS_AVAILABLE);

        TextField locationField = new TextField();
        locationField.setPromptText("напр. Варшава, ул. Промышленная 15");

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Регистрационный номер:"), registrationField,
            new Label("Марка:"), brandField,
            new Label("Страна регистрации:"), countryCombo,
            new Label("Статус:"), statusCombo,
            new Label("Местоположение:"), locationField
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (registrationField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Регистрационный номер не может быть пустым", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (brandField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Марка не может быть пустой", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Truck truck = new Truck();
                truck.setRegistrationNumber(registrationField.getText().trim());
                truck.setBrand(brandField.getText().trim());
                String country = countryCombo.getEditor().getText();
                truck.setRegistrationCountry(country != null ? country.trim() : "");
                String status = statusCombo.getEditor().getText();
                truck.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Truck.STATUS_AVAILABLE);
                truck.setCurrentLocation(locationField.getText().trim());
                return truck;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(truck -> {
            try {
                truckService.addTruck(truck);
                refreshData();
                showAlert("Успех", "Грузовик добавлен", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось добавить грузовик: " + e.getMessage(),
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    private void showEditTruckDialog() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();

        if (selectedTruck == null) {
            showAlert("Ошибка", "Выберите грузовик для редактирования", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Truck> dialog = new Dialog<>();
        dialog.setTitle("Редактировать грузовик");
        dialog.setHeaderText("Грузовик: " + selectedTruck.getBrand() + " (" + selectedTruck.getRegistrationNumber() + ")");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField registrationField = new TextField(selectedTruck.getRegistrationNumber());
        TextField brandField = new TextField(selectedTruck.getBrand());

        ComboBox<String> countryCombo = createEditableComboBox(
                "Польша", "Беларусь", "Чехия", "РФ");
        if (selectedTruck.getRegistrationCountry() != null) {
            countryCombo.setValue(selectedTruck.getRegistrationCountry());
        }

        ComboBox<String> statusCombo = createEditableComboBox(
                Truck.STATUS_AVAILABLE, Truck.STATUS_ON_TRIP, Truck.STATUS_MAINTENANCE);
        statusCombo.setValue(selectedTruck.getStatus());

        TextField locationField = new TextField(
                selectedTruck.getCurrentLocation() != null ? selectedTruck.getCurrentLocation() : "");

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Регистрационный номер:"), registrationField,
            new Label("Марка:"), brandField,
            new Label("Страна регистрации:"), countryCombo,
            new Label("Статус:"), statusCombo,
            new Label("Местоположение:"), locationField
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selectedTruck.setRegistrationNumber(registrationField.getText().trim());
                selectedTruck.setBrand(brandField.getText().trim());
                String country = countryCombo.getEditor().getText();
                selectedTruck.setRegistrationCountry(country != null ? country.trim() : "");
                String status = statusCombo.getEditor().getText();
                selectedTruck.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Truck.STATUS_AVAILABLE);
                selectedTruck.setCurrentLocation(locationField.getText().trim());
                return selectedTruck;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(truck -> {
            try {
                truckService.updateTruck(truck);
                refreshData();
                showAlert("Успех", "Данные грузовика обновлены", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось обновить грузовик: " + e.getMessage(),
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Обрабатывает удаление грузовика
     */
    private void handleDeleteTruck() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Ошибка", "Выберите грузовик для удаления", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Вы уверены, что хотите удалить этот грузовик?");
        confirmAlert.setContentText("Марка: " + selectedTruck.getBrand() + 
            "\nРег. номер: " + selectedTruck.getRegistrationNumber());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    truckService.deleteTruck(selectedTruck.getId());
                    refreshData();
                    showAlert("Успех", "Грузовик удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить грузовик: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Отображает диалог управления вложениями PDF для грузовика
     */
    private void showAttachmentsDialog() {
        Truck selectedTruck = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedTruck == null) {
            showAlert("Ошибка", "Выберите грузовик", Alert.AlertType.WARNING);
            return;
        }

        selectedTruck = truckService.getTruckById(selectedTruck.getId()).orElse(selectedTruck);
        final Truck truck = selectedTruck;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Вложения PDF");
        dialog.setHeaderText("Грузовик: " + truck.getBrand() + " (" + truck.getRegistrationNumber() + ")");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TruckAttachment> attachmentTable = new TableView<>();
        ObservableList<TruckAttachment> attachmentList = FXCollections.observableArrayList(truck.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);
        
        TableColumn<TruckAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<TruckAttachment, String> nameCol = new TableColumn<>("Имя файла");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);
        
        TableColumn<TruckAttachment, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);
        
        TableColumn<TruckAttachment, String> sizeCol = new TableColumn<>("Размер");
        sizeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);
        
        TableColumn<TruckAttachment, String> dateCol = new TableColumn<>("Дата добавления");
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

        Button addBtn = new Button("Добавить PDF");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            addAttachmentToTruck(truck, attachmentList);
            refreshData();
        });
        
        Button downloadBtn = new Button("Скачать PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TruckAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                downloadAttachment(selected);
            } else {
                showAlert("Ошибка", "Выберите вложение для скачивания", Alert.AlertType.WARNING);
            }
        });
        
        Button deleteBtn = new Button("Удалить вложение");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            TruckAttachment selected = attachmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteAttachment(truck, selected, attachmentList);
                refreshData();
            } else {
                showAlert("Ошибка", "Выберите вложение для удаления", Alert.AlertType.WARNING);
            }
        });
        
        HBox buttonBox = new HBox(10, addBtn, downloadBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Список вложений PDF:"),
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
     * Добавляет новое вложение PDF к грузовику
     */
    private void addAttachmentToTruck(Truck truck, ObservableList<TruckAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            TextInputDialog descDialog = new TextInputDialog();
            descDialog.setTitle("Описание вложения");
            descDialog.setHeaderText("Добавьте описание файла (необязательно)");
            descDialog.setContentText("Описание:");
            
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
                
                showAlert("Успех", "Файл '" + file.getName() + "' добавлен", Alert.AlertType.INFORMATION);
                
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось прочитать файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Загружает вложение и сохраняет на диск
     */
    private void downloadAttachment(TruckAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл PDF");
        fileChooser.setInitialFileName(attachment.getFilename());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Files.write(file.toPath(), attachment.getFileData());
                showAlert("Успех", "Файл сохранён как:\n" + file.getAbsolutePath(), 
                    Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось сохранить файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Удаляет вложение у грузовика
     */
    private void deleteAttachment(Truck truck, TruckAttachment attachment, ObservableList<TruckAttachment> attachmentList) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Вы уверены, что хотите удалить это вложение?");
        confirmAlert.setContentText("Файл: " + attachment.getFilename());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    attachmentRepository.delete(attachment);
                    attachmentList.remove(attachment);
                    showAlert("Успех", "Вложение удалено", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить вложение: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Отображает диалоговое окно с сообщением
     */
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
