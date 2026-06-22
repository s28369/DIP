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

/**
 * Controller for the driver management view (drivers, their phones, documents and PDF attachments).
 */
@Component
public class DriverManagementController {

    private final DriverService driverService;
    private final DriverAttachmentRepository attachmentRepository;
    private final ObservableList<Driver> driverList = FXCollections.observableArrayList();
    private FilteredList<Driver> filteredList;
    private VBox view;
    private TableView<Driver> tableView;

    // Constructor injection of the driver service and attachment repository; builds the view.
    @Autowired
    public DriverManagementController(DriverService driverService, DriverAttachmentRepository attachmentRepository) {
        this.driverService = driverService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }

    // Creates an editable combo box pre-filled with the given items.
    private ComboBox<String> createEditableComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setEditable(true);
        combo.setPrefWidth(300);
        return combo;
    }

    // Builds the table, search box and action buttons for the drivers screen.
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Zarządzanie kierowcami");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Dodaj kierowcę");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddDriverDialog());

        Button editButton = new Button("Edytuj");
        editButton.setOnAction(e -> showEditDriverDialog());

        Button deleteButton = new Button("Usuń kierowcę");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteDriver());

        Button documentsButton = new Button("Dokumenty");
        documentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        documentsButton.setOnAction(e -> showDriverDocumentsDialog());

        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });

        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, documentsButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Wpisz tekst do wyszukania...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Wszystkie", "Imię i nazwisko", "Firma", "Status");
        searchParam.setValue("Wszystkie");

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
                case "Imię i nazwisko" -> contains(d.getFullName(), lower);
                case "Firma" -> contains(d.getCompany(), lower);
                case "Status" -> contains(d.getStatus(), lower);
                default -> contains(d.getFullName(), lower)
                        || contains(d.getCompany(), lower)
                        || contains(d.getStatus(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Szukaj:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);

        TableColumn<Driver, String> nameCol = new TableColumn<>("Imię i nazwisko");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(250);

        TableColumn<Driver, String> companyCol = new TableColumn<>("Firma");
        companyCol.setCellValueFactory(cellData -> {
            String c = cellData.getValue().getCompany();
            return new SimpleStringProperty(c != null ? c : "—");
        });
        companyCol.setPrefWidth(120);

        TableColumn<Driver, String> phonesCol = new TableColumn<>("Numery telefonów");
        phonesCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getPhoneCount();
            return new SimpleStringProperty(count > 0 ? count + " numer(ów)" : "—");
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

        TableColumn<Driver, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        tableView.getColumns().addAll(nameCol, companyCol, phonesCol, statusCol);

        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Driver item, boolean empty) {
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
                showDriverDocumentsDialog();
            }
        });

        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
    }

    // Returns the root node of this view.
    public Parent getView() {
        return view;
    }

    // Reloads all drivers into the table (on the FX thread).
    public void refreshData() {
        var data = driverService.getAllDrivers();
        if (javafx.application.Platform.isFxApplicationThread()) {
            driverList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> driverList.setAll(data));
        }
    }

    // ---- Add ----

    // Shows the dialog for adding a new driver.
    private void showAddDriverDialog() {
        Dialog<Driver> dialog = new Dialog<>();
        dialog.setTitle("Dodaj kierowcę");
        dialog.setHeaderText("Wprowadź dane nowego kierowcy");

        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Imię i nazwisko (np. Jan Kowalski)");

        ComboBox<String> companyCombo = createEditableComboBox(
                Driver.COMPANY_MTG, Driver.COMPANY_APA, Driver.COMPANY_ABSOLUT);

        ComboBox<String> statusCombo = createEditableComboBox(
                Driver.STATUS_AVAILABLE, Driver.STATUS_ON_TRIP, Driver.STATUS_MAINTENANCE);
        statusCombo.setValue(Driver.STATUS_AVAILABLE);

        VBox content = new VBox(10,
            new Label("Imię i nazwisko:"), fullNameField,
            new Label("Firma:"), companyCombo,
            new Label("Status:"), statusCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (fullNameField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Imię i nazwisko nie może być puste", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                Driver d = new Driver();
                d.setFullName(fullNameField.getText().trim());
                String company = companyCombo.getEditor().getText();
                d.setCompany(company != null && !company.trim().isEmpty() ? company.trim() : null);
                String status = statusCombo.getEditor().getText();
                d.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Driver.STATUS_AVAILABLE);
                return d;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(driver -> runAsync(() -> {
            driverService.addDriver(driver);
            refreshData();
        }, "Kierowca został dodany", "Nie udało się dodać kierowcy"));
    }

    // ---- Edit ----

    // Shows the dialog for editing the selected driver.
    private void showEditDriverDialog() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz kierowcę do edycji", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Driver> dialog = new Dialog<>();
        dialog.setTitle("Edytuj kierowcę");
        dialog.setHeaderText("Edycja: " + selected.getFullName());

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
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

        VBox content = new VBox(10,
            new Label("Imię i nazwisko:"), fullNameField,
            new Label("Firma:"), companyCombo,
            new Label("Status:"), statusCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                selected.setFullName(fullNameField.getText().trim());
                String company = companyCombo.getEditor().getText();
                selected.setCompany(company != null && !company.trim().isEmpty() ? company.trim() : null);
                String status = statusCombo.getEditor().getText();
                selected.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Driver.STATUS_AVAILABLE);
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(driver -> runAsync(() -> {
            driverService.updateDriver(driver);
            refreshData();
        }, "Dane kierowcy zostały zaktualizowane", "Nie udało się zaktualizować kierowcy"));
    }

    // ---- Delete ----

    // Deletes the selected driver after confirmation (not allowed while on a trip).
    private void handleDeleteDriver() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz kierowcę do usunięcia", Alert.AlertType.WARNING);
            return;
        }

        if (Driver.STATUS_ON_TRIP.equals(selected.getStatus())) {
            showAlert("Błąd", "Nie można usunąć kierowcy w trasie", Alert.AlertType.ERROR);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć tego kierowcę?");
        confirm.setContentText(selected.getFullName());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runAsync(() -> {
                    driverService.deleteDriver(selected.getId());
                    refreshData();
                }, "Kierowca został usunięty", "Nie udało się usunąć kierowcy");
            }
        });
    }

    // ---- Phones dialog ----

    // Opens a dialog listing the driver's phone numbers with add/edit/delete actions.
    private void showPhonesDialog(Driver driver) {
        ObservableList<DriverPhone> phoneList = FXCollections.observableArrayList();
        phoneList.addAll(driverService.getPhonesByDriver(driver.getId()));

        TableView<DriverPhone> phoneTable = new TableView<>();
        phoneTable.setItems(phoneList);

        TableColumn<DriverPhone, String> countryCol = new TableColumn<>("Kraj");
        countryCol.setCellValueFactory(new PropertyValueFactory<>("country"));
        countryCol.setPrefWidth(120);

        TableColumn<DriverPhone, String> numberCol = new TableColumn<>("Numer telefonu");
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
                    showAlert("Skopiowano", sel.getPhoneNumber(), Alert.AlertType.INFORMATION);
                }
            }
        });

        Button addPhoneBtn = new Button("Dodaj numer");
        addPhoneBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addPhoneBtn.setOnAction(e -> addPhone(driver, phoneList));

        Button editPhoneBtn = new Button("Edytuj");
        editPhoneBtn.setOnAction(e -> {
            DriverPhone sel = phoneTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editPhone(sel, driver, phoneList);
            } else {
                showAlert("Błąd", "Wybierz numer do edycji", Alert.AlertType.WARNING);
            }
        });

        Button deletePhoneBtn = new Button("Usuń");
        deletePhoneBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deletePhoneBtn.setOnAction(e -> {
            DriverPhone sel = phoneTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deletePhone(sel, driver, phoneList);
            } else {
                showAlert("Błąd", "Wybierz numer do usunięcia", Alert.AlertType.WARNING);
            }
        });

        HBox phoneBtnBox = new HBox(10, addPhoneBtn, editPhoneBtn, deletePhoneBtn);
        phoneBtnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Numery telefonów: " + driver.getFullName()),
            phoneTable,
            phoneBtnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(phoneTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Numery telefonów");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();

        refreshData();
    }

    // Adds a new phone number to the driver and refreshes the phone list.
    private void addPhone(Driver driver, ObservableList<DriverPhone> phoneList) {
        Dialog<DriverPhone> dialog = new Dialog<>();
        dialog.setTitle("Dodaj numer");
        dialog.setHeaderText("Wprowadź dane numeru telefonu");

        ButtonType addType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        TextField phoneField = new TextField();
        phoneField.setPromptText("+48 123 456 789");

        ComboBox<String> countryCombo = createEditableComboBox(
                "Polska", "Białoruś", "Czechy", "Rosja");

        VBox content = new VBox(10,
            new Label("Kraj:"), countryCombo,
            new Label("Numer telefonu:"), phoneField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(350);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (phoneField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Numer telefonu nie może być pusty", Alert.AlertType.ERROR);
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
                showAlert("Błąd", "Nie udało się dodać numeru: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Edits an existing phone number of the driver.
    private void editPhone(DriverPhone phone, Driver driver, ObservableList<DriverPhone> phoneList) {
        Dialog<DriverPhone> dialog = new Dialog<>();
        dialog.setTitle("Edytuj numer");
        dialog.setHeaderText(null);

        ButtonType saveType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField phoneField = new TextField(phone.getPhoneNumber());

        ComboBox<String> countryCombo = createEditableComboBox(
                "Polska", "Białoruś", "Czechy", "Rosja");
        if (phone.getCountry() != null) {
            countryCombo.setValue(phone.getCountry());
        }

        VBox content = new VBox(10,
            new Label("Kraj:"), countryCombo,
            new Label("Numer telefonu:"), phoneField
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
                showAlert("Błąd", "Nie udało się zaktualizować numeru: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Deletes a phone number from the driver.
    private void deletePhone(DriverPhone phone, Driver driver, ObservableList<DriverPhone> phoneList) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Usunąć ten numer?");
        confirm.setContentText(phone.getCountry() + ": " + phone.getPhoneNumber());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    driverService.deletePhone(phone.getId());
                    phoneList.setAll(driverService.getPhonesByDriver(driver.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się usunąć numeru: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Attachments dialog ----

    // Opens a dialog listing the driver's document attachments with add/edit/download/delete actions.
    private void showDriverDocumentsDialog() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz kierowcę, aby zobaczyć dokumenty", Alert.AlertType.WARNING);
            return;
        }

        Driver driver = driverService.getDriverById(selected.getId()).orElse(selected);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dokumenty");
        dialog.setHeaderText("Kierowca: " + driver.getFullName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<DriverAttachment> attachmentTable = new TableView<>();
        ObservableList<DriverAttachment> attachmentList = FXCollections.observableArrayList(driver.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);

        TableColumn<DriverAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<DriverAttachment, String> nameCol = new TableColumn<>("Nazwa pliku");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);

        TableColumn<DriverAttachment, String> descCol = new TableColumn<>("Opis");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);

        TableColumn<DriverAttachment, String> dateCol = new TableColumn<>("Data dodania");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);

        TableColumn<DriverAttachment, String> expCol = new TableColumn<>("Data ważności");
        expCol.setCellValueFactory(cellData -> {
            LocalDate exp = cellData.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? exp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "—");
        });
        expCol.setPrefWidth(120);

        TableColumn<DriverAttachment, String> sizeCol = new TableColumn<>("Rozmiar");
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

        Button addBtn = new Button("Dodaj plik");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> { addAttachmentToDriver(driver, attachmentList); refreshData(); });

        Button downloadBtn = new Button("Pobierz");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) downloadAttachment(sel);
            else showAlert("Błąd", "Wybierz plik do pobrania", Alert.AlertType.WARNING);
        });

        Button editDescBtn = new Button("Edytuj opis");
        editDescBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        editDescBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) editAttachmentDescription(sel, attachmentList, driver);
            else showAlert("Błąd", "Wybierz plik", Alert.AlertType.WARNING);
        });

        Button expDateBtn = new Button("Data ważności");
        expDateBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        expDateBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) editExpirationDate(sel, attachmentList, driver);
            else showAlert("Błąd", "Wybierz plik", Alert.AlertType.WARNING);
        });

        Button deleteBtn = new Button("Usuń");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            DriverAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) { deleteAttachment(driver, sel, attachmentList); refreshData(); }
            else showAlert("Błąd", "Wybierz plik do usunięcia", Alert.AlertType.WARNING);
        });

        HBox buttonBox = new HBox(10, addBtn, downloadBtn, editDescBtn, expDateBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Lista dokumentów:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();
    }

    // Lets the user pick a file and attaches it (with description/expiry) to the driver.
    private void addAttachmentToDriver(Driver driver, ObservableList<DriverAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz pliki");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Wszystkie obsługiwane", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Obrazy", "*.jpg", "*.jpeg", "*.png"));

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
                    showAlert("Błąd", "Nie udało się odczytać pliku: " + file.getName() + "\n" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            if (added > 0) {
                showAlert("Sukces", "Dodano plików: " + added, Alert.AlertType.INFORMATION);
            }
        }
    }

    // Edits the description of a driver document attachment.
    private void editAttachmentDescription(DriverAttachment attachment, ObservableList<DriverAttachment> attachmentList, Driver driver) {
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
                    new java.util.ArrayList<>(driverService.getDriverById(driver.getId()).map(Driver::getAttachments).orElse(java.util.Set.of())));
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać opisu: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Edits the expiration date of a driver document attachment.
    private void editExpirationDate(DriverAttachment attachment, ObservableList<DriverAttachment> attachmentList, Driver driver) {
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
                    new java.util.ArrayList<>(driverService.getDriverById(driver.getId()).map(Driver::getAttachments).orElse(java.util.Set.of())));
                refreshData();
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać daty: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Saves the selected attachment's file to disk.
    private void downloadAttachment(DriverAttachment attachment) {
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
                showAlert("Sukces", "Plik został zapisany: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // Deletes the selected attachment from the driver.
    private void deleteAttachment(Driver driver, DriverAttachment attachment, ObservableList<DriverAttachment> attachmentList) {
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
                    showAlert("Błąd", "Nie udało się usunąć załącznika: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // Returns a row style highlighting drivers whose documents are expired or expiring soon.
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

    // Case-insensitive substring check used by the search filter.
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
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
