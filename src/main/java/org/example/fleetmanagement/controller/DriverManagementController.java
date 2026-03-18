package org.example.fleetmanagement.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import org.example.fleetmanagement.model.DriverAttachment;
import org.example.fleetmanagement.model.DriverPhone;
import org.example.fleetmanagement.repository.DriverAttachmentRepository;
import org.example.fleetmanagement.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class DriverManagementController {

    private final DriverService driverService;
    private final DriverAttachmentRepository attachmentRepository;
    private final ObservableList<Driver> driverList = FXCollections.observableArrayList();
    private FilteredList<Driver> filteredList;
    private SortedList<Driver> sortedList;
    private VBox view;
    private TableView<Driver> tableView;

    @Autowired
    public DriverManagementController(DriverService driverService, DriverAttachmentRepository attachmentRepository) {
        this.driverService = driverService;
        this.attachmentRepository = attachmentRepository;
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
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });

        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, documentsButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Все", "ФИО", "Фирма", "Статус");
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
                case "Фирма" -> contains(d.getCompany(), lower);
                case "Статус" -> contains(d.getStatus(), lower);
                default -> contains(d.getFullName(), lower)
                        || contains(d.getCompany(), lower)
                        || contains(d.getStatus(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        sortedList = new SortedList<>(filteredList, (a, b) -> {
            boolean ba = a.isBirthdayToday();
            boolean bb = b.isBirthdayToday();
            if (ba && !bb) return -1;
            if (!ba && bb) return 1;
            return 0;
        });

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(sortedList);

        TableColumn<Driver, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(250);

        TableColumn<Driver, String> companyCol = new TableColumn<>("Фирма");
        companyCol.setCellValueFactory(cellData -> {
            String c = cellData.getValue().getCompany();
            return new SimpleStringProperty(c != null ? c : "—");
        });
        companyCol.setPrefWidth(120);

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

        TableColumn<Driver, String> birthCol = new TableColumn<>("День рождения");
        birthCol.setCellValueFactory(cellData -> {
            LocalDate bd = cellData.getValue().getBirthDate();
            return new SimpleStringProperty(bd != null ? bd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "—");
        });
        birthCol.setPrefWidth(120);

        TableColumn<Driver, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        tableView.getColumns().addAll(nameCol, companyCol, birthCol, phonesCol, statusCol);

        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Driver item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isBirthdayToday()) {
                    setStyle("-fx-background-color: #c8e6c9;");
                } else {
                    setStyle(getExpirationStyle(item.getAttachments()));
                }
            }
        });

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

        ComboBox<String> companyCombo = createEditableComboBox(
                Driver.COMPANY_MTG, Driver.COMPANY_APA, Driver.COMPANY_ABSOLUT);

        ComboBox<String> statusCombo = createEditableComboBox(
                Driver.STATUS_AVAILABLE, Driver.STATUS_ON_TRIP, Driver.STATUS_MAINTENANCE);
        statusCombo.setValue(Driver.STATUS_AVAILABLE);

        DatePicker birthDatePicker = new DatePicker();
        birthDatePicker.setPromptText("День рождения (необязательно)");

        VBox content = new VBox(10,
            new Label("ФИО:"), fullNameField,
            new Label("Фирма:"), companyCombo,
            new Label("День рождения:"), birthDatePicker,
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
                String company = companyCombo.getEditor().getText();
                d.setCompany(company != null && !company.trim().isEmpty() ? company.trim() : null);
                d.setBirthDate(birthDatePicker.getValue());
                String status = statusCombo.getEditor().getText();
                d.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Driver.STATUS_AVAILABLE);
                return d;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(driver -> runAsync(() -> {
            driverService.addDriver(driver);
            refreshData();
        }, "Водитель добавлен", "Не удалось добавить водителя"));
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

        ComboBox<String> companyCombo = createEditableComboBox(
                Driver.COMPANY_MTG, Driver.COMPANY_APA, Driver.COMPANY_ABSOLUT);
        if (selected.getCompany() != null) {
            companyCombo.setValue(selected.getCompany());
        }

        ComboBox<String> statusCombo = createEditableComboBox(
                Driver.STATUS_AVAILABLE, Driver.STATUS_ON_TRIP, Driver.STATUS_MAINTENANCE);
        statusCombo.setValue(selected.getStatus());

        DatePicker birthDatePicker = new DatePicker(selected.getBirthDate());
        birthDatePicker.setPromptText("День рождения (необязательно)");

        VBox content = new VBox(10,
            new Label("ФИО:"), fullNameField,
            new Label("Фирма:"), companyCombo,
            new Label("День рождения:"), birthDatePicker,
            new Label("Статус:"), statusCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                selected.setFullName(fullNameField.getText().trim());
                String company = companyCombo.getEditor().getText();
                selected.setCompany(company != null && !company.trim().isEmpty() ? company.trim() : null);
                selected.setBirthDate(birthDatePicker.getValue());
                String status = statusCombo.getEditor().getText();
                selected.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Driver.STATUS_AVAILABLE);
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(driver -> runAsync(() -> {
            driverService.updateDriver(driver);
            refreshData();
        }, "Данные водителя обновлены", "Не удалось обновить водителя"));
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
                runAsync(() -> {
                    driverService.deleteDriver(selected.getId());
                    refreshData();
                }, "Водитель удалён", "Не удалось удалить водителя");
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

        phoneTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                DriverPhone sel = phoneTable.getSelectionModel().getSelectedItem();
                if (sel != null && sel.getPhoneNumber() != null) {
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(sel.getPhoneNumber());
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                    showAlert("Скопировано", sel.getPhoneNumber(), Alert.AlertType.INFORMATION);
                }
            }
        });

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

    // ---- Attachments dialog ----

    private void showDriverDocumentsDialog() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите водителя для просмотра документов", Alert.AlertType.WARNING);
            return;
        }

        Driver driver = driverService.getDriverById(selected.getId()).orElse(selected);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Документы");
        dialog.setHeaderText("Водитель: " + driver.getFullName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<DriverAttachment> attachmentTable = new TableView<>();
        ObservableList<DriverAttachment> attachmentList = FXCollections.observableArrayList(driver.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);

        TableColumn<DriverAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<DriverAttachment, String> nameCol = new TableColumn<>("Имя файла");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);

        TableColumn<DriverAttachment, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);

        TableColumn<DriverAttachment, String> dateCol = new TableColumn<>("Дата добавления");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);

        TableColumn<DriverAttachment, String> expCol = new TableColumn<>("Дата окончания");
        expCol.setCellValueFactory(cellData -> {
            LocalDate exp = cellData.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? exp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "—");
        });
        expCol.setPrefWidth(120);

        TableColumn<DriverAttachment, String> sizeCol = new TableColumn<>("Размер");
        sizeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);

        attachmentTable.getColumns().addAll(idCol, nameCol, descCol, dateCol, expCol, sizeCol);

        attachmentTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(DriverAttachment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getExpirationDate() == null) {
                    setStyle("");
                } else {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), item.getExpirationDate());
                    if (days <= 7) setStyle("-fx-background-color: #ffcdd2;");
                    else if (days <= 30) setStyle("-fx-background-color: #fff9c4;");
                    else setStyle("");
                }
            }
        });

        Button addBtn = new Button("Добавить файл");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> { addAttachmentToDriver(driver, attachmentList); refreshData(); });

        Button downloadBtn = new Button("Скачать");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) downloadAttachment(sel);
            else showAlert("Ошибка", "Выберите файл для скачивания", Alert.AlertType.WARNING);
        });

        Button editDescBtn = new Button("Изменить описание");
        editDescBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        editDescBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) editAttachmentDescription(sel, attachmentList, driver);
            else showAlert("Ошибка", "Выберите файл", Alert.AlertType.WARNING);
        });

        Button expDateBtn = new Button("Дата окончания");
        expDateBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        expDateBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) editExpirationDate(sel, attachmentList, driver);
            else showAlert("Ошибка", "Выберите файл", Alert.AlertType.WARNING);
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) { deleteAttachment(driver, sel, attachmentList); refreshData(); }
            else showAlert("Ошибка", "Выберите файл для удаления", Alert.AlertType.WARNING);
        });

        HBox buttonBox = new HBox(10, addBtn, downloadBtn, editDescBtn, expDateBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Список документов:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();
    }

    private void addAttachmentToDriver(Driver driver, ObservableList<DriverAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файлы");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Все поддерживаемые", "*.pdf", "*.doc", "*.docx", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Word", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.jpeg", "*.png"));

        Stage stage = (Stage) view.getScene().getWindow();
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            int added = 0;
            for (File file : files) {
                try {
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    DriverAttachment attachment = new DriverAttachment(file.getName(), "", fileData, driver);
                    attachmentRepository.save(attachment);
                    attachmentList.add(attachment);
                    added++;
                } catch (IOException e) {
                    showAlert("Ошибка", "Не удалось прочитать файл: " + file.getName() + "\n" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            if (added > 0) {
                showAlert("Успех", "Добавлено файлов: " + added, Alert.AlertType.INFORMATION);
            }
        }
    }

    private void editAttachmentDescription(DriverAttachment attachment, ObservableList<DriverAttachment> attachmentList, Driver driver) {
        TextInputDialog dlg = new TextInputDialog(attachment.getDescription() != null ? attachment.getDescription() : "");
        dlg.setTitle("Описание документа");
        dlg.setHeaderText("Файл: " + attachment.getFilename());
        dlg.setContentText("Описание:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            try {
                attachment.setDescription(text.trim());
                attachmentRepository.save(attachment);
                attachmentList.setAll(
                    new java.util.ArrayList<>(driverService.getDriverById(driver.getId()).map(Driver::getAttachments).orElse(java.util.Set.of())));
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось сохранить описание: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editExpirationDate(DriverAttachment attachment, ObservableList<DriverAttachment> attachmentList, Driver driver) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Дата окончания документа");
        dlg.setHeaderText("Файл: " + attachment.getFilename());

        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("Убрать дату", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, clearType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(attachment.getExpirationDate());
        VBox dpContent = new VBox(10, new Label("Дата окончания:"), datePicker);
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
                    new java.util.ArrayList<>(driverService.getDriverById(driver.getId()).map(Driver::getAttachments).orElse(java.util.Set.of())));
                refreshData();
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось сохранить дату: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void downloadAttachment(DriverAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл");
        fileChooser.setInitialFileName(attachment.getFilename());
        String ext = getExtension(attachment.getFilename());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(ext.toUpperCase() + " файлы", "*." + ext),
            new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));

        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            file = ensureExtension(file, ext);
            try {
                byte[] data = attachmentRepository.findFileDataById(attachment.getId());
                Files.write(file.toPath(), data);
                showAlert("Успех", "Файл сохранён: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось сохранить файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void deleteAttachment(Driver driver, DriverAttachment attachment, ObservableList<DriverAttachment> attachmentList) {
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

    private static String getExpirationStyle(java.util.Collection<DriverAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";
        long minDays = Long.MAX_VALUE;
        for (DriverAttachment a : attachments) {
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

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private void runAsync(Runnable task, String successMsg, String errorPrefix) {
        Thread.ofVirtual().start(() -> {
            try {
                task.run();
                javafx.application.Platform.runLater(() ->
                    showAlert("Успех", successMsg, Alert.AlertType.INFORMATION));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    showAlert("Ошибка", errorPrefix + ": " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "pdf";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private static java.io.File ensureExtension(java.io.File file, String ext) {
        String name = file.getName();
        if (name.contains(".") && name.toLowerCase().endsWith("." + ext.toLowerCase())) return file;
        if (!name.contains(".")) return new java.io.File(file.getParent(), name + "." + ext);
        return file;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
