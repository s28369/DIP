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
import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverDocument;
import org.example.fleetmanagement.model.DriverPhone;
import org.example.fleetmanagement.service.DriverDocumentService;
import org.example.fleetmanagement.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

@Component
public class DriverManagementController {

    private final DriverService driverService;
    private final DriverDocumentService driverDocumentService;
    private final ObservableList<Driver> driverList = FXCollections.observableArrayList();
    private FilteredList<Driver> filteredList;
    private VBox view;
    private TableView<Driver> tableView;

    @Autowired
    public DriverManagementController(DriverService driverService, DriverDocumentService driverDocumentService) {
        this.driverService = driverService;
        this.driverDocumentService = driverDocumentService;
        initializeView();
    }

    private ComboBox<String> createEditableComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setEditable(true);
        combo.setPrefWidth(300);
        return combo;
    }

    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Управление водителями");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Добавить водителя");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddDriverDialog());

        Button editButton = new Button("Редактировать");
        editButton.setOnAction(e -> showEditDriverDialog());

        Button deleteButton = new Button("Удалить водителя");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteDriver());

        Button documentsButton = new Button("Документы");
        documentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        documentsButton.setOnAction(e -> showDriverDocumentsDialog());

        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(e -> refreshData());

        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, documentsButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Все", "ФИО", "Статус");
        searchParam.setValue("Все");

        filteredList = new FilteredList<>(driverList, p -> true);

        Runnable applyFilter = () -> {
            String text = searchField.getText();
            String param = searchParam.getValue();
            if (text == null || text.trim().isEmpty()) {
                filteredList.setPredicate(p -> true);
                return;
            }
            String lower = text.trim().toLowerCase();
            filteredList.setPredicate(d -> switch (param) {
                case "ФИО" -> contains(d.getFullName(), lower);
                case "Статус" -> contains(d.getStatus(), lower);
                default -> contains(d.getFullName(), lower)
                        || contains(d.getStatus(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);

        TableColumn<Driver, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(250);

        TableColumn<Driver, String> phonesCol = new TableColumn<>("Номера телефонов");
        phonesCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getPhoneCount();
            return new SimpleStringProperty(count > 0 ? count + " номер(ов)" : "—");
        });
        phonesCol.setPrefWidth(180);
        phonesCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Driver driver = getTableView().getItems().get(getIndex());
                    showPhonesDialog(driver);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    btn.setText(item);
                    setGraphic(btn);
                }
            }
        });

        TableColumn<Driver, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        tableView.getColumns().addAll(nameCol, phonesCol, statusCol);

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                showDriverDocumentsDialog();
            }
        });

        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
    }

    public Parent getView() {
        return view;
    }

    public void refreshData() {
        var data = driverService.getAllDrivers();
        if (javafx.application.Platform.isFxApplicationThread()) {
            driverList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> driverList.setAll(data));
        }
    }

    // ---- Add ----

    private void showAddDriverDialog() {
        Dialog<Driver> dialog = new Dialog<>();
        dialog.setTitle("Добавить водителя");
        dialog.setHeaderText("Введите данные нового водителя");

        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("ФИО (напр. Иванов Иван Иванович)");

        ComboBox<String> statusCombo = createEditableComboBox(
                Driver.STATUS_AVAILABLE, Driver.STATUS_ON_TRIP, Driver.STATUS_MAINTENANCE);
        statusCombo.setValue(Driver.STATUS_AVAILABLE);

        VBox content = new VBox(10,
            new Label("ФИО:"), fullNameField,
            new Label("Статус:"), statusCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (fullNameField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "ФИО не может быть пустым", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                Driver d = new Driver();
                d.setFullName(fullNameField.getText().trim());
                String status = statusCombo.getEditor().getText();
                d.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Driver.STATUS_AVAILABLE);
                return d;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(driver -> {
            try {
                driverService.addDriver(driver);
                refreshData();
                showAlert("Успех", "Водитель добавлен", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось добавить водителя: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // ---- Edit ----

    private void showEditDriverDialog() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите водителя для редактирования", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Driver> dialog = new Dialog<>();
        dialog.setTitle("Редактировать водителя");
        dialog.setHeaderText("Редактирование: " + selected.getFullName());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField fullNameField = new TextField(selected.getFullName());

        ComboBox<String> statusCombo = createEditableComboBox(
                Driver.STATUS_AVAILABLE, Driver.STATUS_ON_TRIP, Driver.STATUS_MAINTENANCE);
        statusCombo.setValue(selected.getStatus());

        VBox content = new VBox(10,
            new Label("ФИО:"), fullNameField,
            new Label("Статус:"), statusCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                selected.setFullName(fullNameField.getText().trim());
                String status = statusCombo.getEditor().getText();
                selected.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Driver.STATUS_AVAILABLE);
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(driver -> {
            try {
                driverService.updateDriver(driver);
                refreshData();
                showAlert("Успех", "Данные водителя обновлены", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось обновить водителя: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // ---- Delete ----

    private void handleDeleteDriver() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите водителя для удаления", Alert.AlertType.WARNING);
            return;
        }

        if (Driver.STATUS_ON_TRIP.equals(selected.getStatus())) {
            showAlert("Ошибка", "Невозможно удалить водителя в рейсе", Alert.AlertType.ERROR);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Вы уверены, что хотите удалить этого водителя?");
        confirm.setContentText(selected.getFullName());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    driverService.deleteDriver(selected.getId());
                    refreshData();
                    showAlert("Успех", "Водитель удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить водителя: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Phones dialog ----

    private void showPhonesDialog(Driver driver) {
        ObservableList<DriverPhone> phoneList = FXCollections.observableArrayList();
        phoneList.addAll(driverService.getPhonesByDriver(driver.getId()));

        TableView<DriverPhone> phoneTable = new TableView<>();
        phoneTable.setItems(phoneList);

        TableColumn<DriverPhone, String> countryCol = new TableColumn<>("Страна");
        countryCol.setCellValueFactory(new PropertyValueFactory<>("country"));
        countryCol.setPrefWidth(120);

        TableColumn<DriverPhone, String> numberCol = new TableColumn<>("Номер телефона");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        numberCol.setPrefWidth(200);

        phoneTable.getColumns().addAll(countryCol, numberCol);

        Button addPhoneBtn = new Button("Добавить номер");
        addPhoneBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addPhoneBtn.setOnAction(e -> addPhone(driver, phoneList));

        Button editPhoneBtn = new Button("Редактировать");
        editPhoneBtn.setOnAction(e -> {
            DriverPhone sel = phoneTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editPhone(sel, driver, phoneList);
            } else {
                showAlert("Ошибка", "Выберите номер для редактирования", Alert.AlertType.WARNING);
            }
        });

        Button deletePhoneBtn = new Button("Удалить");
        deletePhoneBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deletePhoneBtn.setOnAction(e -> {
            DriverPhone sel = phoneTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deletePhone(sel, driver, phoneList);
            } else {
                showAlert("Ошибка", "Выберите номер для удаления", Alert.AlertType.WARNING);
            }
        });

        HBox phoneBtnBox = new HBox(10, addPhoneBtn, editPhoneBtn, deletePhoneBtn);
        phoneBtnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Номера телефонов: " + driver.getFullName()),
            phoneTable,
            phoneBtnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(phoneTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Номера телефонов");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();

        refreshData();
    }

    private void addPhone(Driver driver, ObservableList<DriverPhone> phoneList) {
        Dialog<DriverPhone> dialog = new Dialog<>();
        dialog.setTitle("Добавить номер");
        dialog.setHeaderText("Введите данные номера телефона");

        ButtonType addType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        TextField phoneField = new TextField();
        phoneField.setPromptText("+48 123 456 789");

        ComboBox<String> countryCombo = createEditableComboBox(
                "Польша", "Беларусь", "Чехия", "РФ");

        VBox content = new VBox(10,
            new Label("Страна:"), countryCombo,
            new Label("Номер телефона:"), phoneField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(350);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (phoneField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Номер телефона не может быть пустым", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == addType) {
                String country = countryCombo.getEditor().getText();
                return new DriverPhone(
                    phoneField.getText().trim(),
                    country != null ? country.trim() : "",
                    driver
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(phone -> {
            try {
                driverService.addPhone(phone);
                phoneList.setAll(driverService.getPhonesByDriver(driver.getId()));
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось добавить номер: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editPhone(DriverPhone phone, Driver driver, ObservableList<DriverPhone> phoneList) {
        Dialog<DriverPhone> dialog = new Dialog<>();
        dialog.setTitle("Редактировать номер");
        dialog.setHeaderText(null);

        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField phoneField = new TextField(phone.getPhoneNumber());

        ComboBox<String> countryCombo = createEditableComboBox(
                "Польша", "Беларусь", "Чехия", "РФ");
        if (phone.getCountry() != null) {
            countryCombo.setValue(phone.getCountry());
        }

        VBox content = new VBox(10,
            new Label("Страна:"), countryCombo,
            new Label("Номер телефона:"), phoneField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(350);

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                phone.setPhoneNumber(phoneField.getText().trim());
                String country = countryCombo.getEditor().getText();
                phone.setCountry(country != null ? country.trim() : "");
                return phone;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                driverService.updatePhone(p);
                phoneList.setAll(driverService.getPhonesByDriver(driver.getId()));
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось обновить номер: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deletePhone(DriverPhone phone, Driver driver, ObservableList<DriverPhone> phoneList) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить этот номер?");
        confirm.setContentText(phone.getCountry() + ": " + phone.getPhoneNumber());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    driverService.deletePhone(phone.getId());
                    phoneList.setAll(driverService.getPhonesByDriver(driver.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить номер: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Documents dialog (kept from before) ----

    private void showDriverDocumentsDialog() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите водителя для просмотра документов", Alert.AlertType.WARNING);
            return;
        }

        ObservableList<DriverDocument> docList = FXCollections.observableArrayList();
        docList.addAll(driverDocumentService.getDocumentsByDriver(selected));

        TableView<DriverDocument> docTable = new TableView<>();
        docTable.setItems(docList);

        TableColumn<DriverDocument, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<DriverDocument, DriverDocument.DocumentType> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        typeCol.setPrefWidth(180);

        TableColumn<DriverDocument, LocalDate> expiryCol = new TableColumn<>("Срок действия");
        expiryCol.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        expiryCol.setPrefWidth(120);

        TableColumn<DriverDocument, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<DriverDocument, String> pdfCol = new TableColumn<>("PDF");
        pdfCol.setCellValueFactory(cellData -> {
            DriverDocument doc = cellData.getValue();
            String status = doc.hasPdf() ? "Да (" + doc.getPdfFilename() + ")" : "Нет";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        pdfCol.setPrefWidth(150);

        docTable.getColumns().addAll(idCol, typeCol, expiryCol, descCol, pdfCol);

        Button addDocButton = new Button("Добавить документ");
        addDocButton.setOnAction(e -> addDriverDocument(selected, docList));

        Button deleteDocButton = new Button("Удалить документ");
        deleteDocButton.setOnAction(e -> deleteDriverDocument(docTable.getSelectionModel().getSelectedItem(), docList));

        Button uploadPdfButton = new Button("Добавить PDF");
        uploadPdfButton.setOnAction(e -> uploadDriverDocumentPdf(docTable.getSelectionModel().getSelectedItem(), docList));

        Button downloadPdfButton = new Button("Скачать PDF");
        downloadPdfButton.setOnAction(e -> downloadDriverDocumentPdf(docTable.getSelectionModel().getSelectedItem()));

        Button expiringButton = new Button("Истекающие");
        expiringButton.setOnAction(e -> showExpiringDriverDocuments(selected));

        HBox docButtonBox = new HBox(10, addDocButton, deleteDocButton, uploadPdfButton, downloadPdfButton, expiringButton);

        VBox docContent = new VBox(10);
        docContent.setPadding(new Insets(15));
        docContent.getChildren().addAll(
            new Label("Документы водителя: " + selected.getFullName()),
            docButtonBox,
            docTable
        );
        VBox.setVgrow(docTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Документы водителя");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(docContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();
    }

    private void addDriverDocument(Driver driver, ObservableList<DriverDocument> docList) {
        Dialog<DriverDocument> dialog = new Dialog<>();
        dialog.setTitle("Добавить документ водителя");
        dialog.setHeaderText("Введите данные документа");

        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        ComboBox<DriverDocument.DocumentType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(DriverDocument.DocumentType.values());
        typeCombo.setValue(DriverDocument.DocumentType.DRIVING_LICENSE);

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusYears(1));

        TextField descField = new TextField();
        descField.setPromptText("Описание (необязательно)");

        VBox content = new VBox(10,
            new Label("Тип документа:"), typeCombo,
            new Label("Срок действия:"), datePicker,
            new Label("Описание:"), descField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(350);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                DriverDocument doc = new DriverDocument();
                doc.setDriver(driver);
                doc.setDocumentType(typeCombo.getValue());
                doc.setExpiryDate(datePicker.getValue());
                doc.setDescription(descField.getText());
                return doc;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(doc -> {
            try {
                driverDocumentService.addDocument(doc);
                docList.clear();
                docList.addAll(driverDocumentService.getDocumentsByDriver(driver));
                showAlert("Успех", "Документ добавлен", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Ошибка", "Не удалось добавить документ: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteDriverDocument(DriverDocument doc, ObservableList<DriverDocument> docList) {
        if (doc == null) {
            showAlert("Ошибка", "Выберите документ для удаления", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Вы уверены, что хотите удалить этот документ?");
        confirm.setContentText(doc.getDocumentType().getDisplayName());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    driverDocumentService.deleteDocument(doc.getId());
                    docList.remove(doc);
                    showAlert("Успех", "Документ удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить документ: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void uploadDriverDocumentPdf(DriverDocument doc, ObservableList<DriverDocument> docList) {
        if (doc == null) {
            showAlert("Ошибка", "Выберите документ для добавления PDF", Alert.AlertType.WARNING);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf"));
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                doc.setPdfData(data);
                doc.setPdfFilename(file.getName());
                driverDocumentService.updateDocument(doc);
                docList.clear();
                docList.addAll(driverDocumentService.getDocumentsByDriver(doc.getDriver()));
                showAlert("Успех", "Файл PDF добавлен", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось прочитать файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void downloadDriverDocumentPdf(DriverDocument doc) {
        if (doc == null) {
            showAlert("Ошибка", "Выберите документ", Alert.AlertType.WARNING);
            return;
        }
        if (!doc.hasPdf()) {
            showAlert("Ошибка", "У выбранного документа нет прикреплённого PDF", Alert.AlertType.WARNING);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл PDF");
        fileChooser.setInitialFileName(doc.getPdfFilename());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf"));
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.write(file.toPath(), doc.getPdfData());
                showAlert("Успех", "Файл сохранён: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось сохранить файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showExpiringDriverDocuments(Driver driver) {
        var allExpiring = driverDocumentService.getExpiringDocuments();
        var expiringForDriver = allExpiring.stream()
            .filter(d -> d.getDriver().getId().equals(driver.getId()))
            .toList();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Истекающие документы");
        alert.setHeaderText("Документы водителя " + driver.getFullName() + " с истекающим сроком действия (30 дней):");
        if (expiringForDriver.isEmpty()) {
            alert.setContentText("Нет документов с истекающим сроком.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (DriverDocument d : expiringForDriver) {
                sb.append("• ").append(d.getDocumentType().getDisplayName())
                  .append(" — действителен до: ").append(d.getExpiryDate()).append("\n");
            }
            alert.setContentText(sb.toString());
        }
        alert.showAndWait();
    }

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
