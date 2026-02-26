package org.example.fleetmanagement.controller;

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
import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverDocument;
import org.example.fleetmanagement.service.DriverDocumentService;
import org.example.fleetmanagement.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Kontroler zarządzający widokiem kierowców
 */
@Component
public class DriverManagementController {

    private final DriverService driverService;
    private final DriverDocumentService driverDocumentService;
    private final ObservableList<Driver> driverList = FXCollections.observableArrayList();
    private VBox view;
    private TableView<Driver> tableView;

    @Autowired
    public DriverManagementController(DriverService driverService, DriverDocumentService driverDocumentService) {
        this.driverService = driverService;
        this.driverDocumentService = driverDocumentService;
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        
        Label titleLabel = new Label("Zarządzanie Kierowcami");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Dodaj kierowcę");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddDriverDialog());
        
        Button editButton = new Button("Edytuj kierowcę");
        editButton.setOnAction(e -> showEditDriverDialog());
        
        Button deleteButton = new Button("Usuń kierowcę");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteDriver());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> refreshData());

        Button documentsButton = new Button("Dokumenty kierowcy");
        documentsButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        documentsButton.setOnAction(e -> showDriverDocumentsDialog());
        
        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, documentsButton, refreshButton);

        tableView = new TableView<>();
        tableView.setItems(driverList);
        
        TableColumn<Driver, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Driver, String> firstNameCol = new TableColumn<>("Imię");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        firstNameCol.setPrefWidth(120);
        
        TableColumn<Driver, String> lastNameCol = new TableColumn<>("Nazwisko");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastNameCol.setPrefWidth(120);
        
        TableColumn<Driver, String> phoneCol = new TableColumn<>("Telefon");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        phoneCol.setPrefWidth(120);
        
        TableColumn<Driver, String> licenseCol = new TableColumn<>("Nr prawa jazdy");
        licenseCol.setCellValueFactory(new PropertyValueFactory<>("licenseNumber"));
        licenseCol.setPrefWidth(150);
        
        TableColumn<Driver, Driver.DriverStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);
        
        tableView.getColumns().addAll(idCol, firstNameCol, lastNameCol, phoneCol, licenseCol, statusCol);
        
        view.getChildren().addAll(titleLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    public Parent getView() {
        return view;
    }
    
    public void refreshData() {
        driverList.clear();
        driverList.addAll(driverService.getAllDrivers());
    }
    
    private void showAddDriverDialog() {
        Dialog<Driver> dialog = new Dialog<>();
        dialog.setTitle("Dodaj kierowcę");
        dialog.setHeaderText("Wprowadź dane nowego kierowcy");
        
        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Imię");
        
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Nazwisko");
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Numer telefonu");
        
        TextField licenseField = new TextField();
        licenseField.setPromptText("Numer prawa jazdy");
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Imię:"), firstNameField,
            new Label("Nazwisko:"), lastNameField,
            new Label("Telefon:"), phoneField,
            new Label("Nr prawa jazdy:"), licenseField
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(350);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Driver driver = new Driver();
                driver.setFirstName(firstNameField.getText().trim());
                driver.setLastName(lastNameField.getText().trim());
                driver.setPhoneNumber(phoneField.getText().trim());
                driver.setLicenseNumber(licenseField.getText().trim());
                driver.setStatus(Driver.DriverStatus.AVAILABLE);
                return driver;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(driver -> {
            try {
                driverService.addDriver(driver);
                refreshData();
                showAlert("Sukces", "Kierowca został dodany", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można dodać kierowcy: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
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
        
        TextField firstNameField = new TextField(selected.getFirstName());
        TextField lastNameField = new TextField(selected.getLastName());
        TextField phoneField = new TextField(selected.getPhoneNumber() != null ? selected.getPhoneNumber() : "");
        TextField licenseField = new TextField(selected.getLicenseNumber());
        
        ComboBox<Driver.DriverStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Driver.DriverStatus.values());
        statusCombo.setValue(selected.getStatus());
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Imię:"), firstNameField,
            new Label("Nazwisko:"), lastNameField,
            new Label("Telefon:"), phoneField,
            new Label("Nr prawa jazdy:"), licenseField,
            new Label("Status:"), statusCombo
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(350);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setFirstName(firstNameField.getText().trim());
                selected.setLastName(lastNameField.getText().trim());
                selected.setPhoneNumber(phoneField.getText().trim());
                selected.setLicenseNumber(licenseField.getText().trim());
                selected.setStatus(statusCombo.getValue());
                return selected;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(driver -> {
            try {
                driverService.updateDriver(driver);
                refreshData();
                showAlert("Sukces", "Dane kierowcy zaktualizowane", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można zaktualizować kierowcy: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    
    private void handleDeleteDriver() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz kierowcę do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        
        if (selected.getStatus() == Driver.DriverStatus.ON_TRIP) {
            showAlert("Błąd", "Nie można usunąć kierowcy w trasie", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć tego kierowcę?");
        confirm.setContentText(selected.getFullName() + " (" + selected.getLicenseNumber() + ")");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    driverService.deleteDriver(selected.getId());
                    refreshData();
                    showAlert("Sukces", "Kierowca został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć kierowcy: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Wyświetla dialog z dokumentami wybranego kierowcy
     */
    private void showDriverDocumentsDialog() {
        Driver selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz kierowcę, aby zobaczyć jego dokumenty", Alert.AlertType.WARNING);
            return;
        }

        ObservableList<DriverDocument> docList = FXCollections.observableArrayList();
        docList.addAll(driverDocumentService.getDocumentsByDriver(selected));

        TableView<DriverDocument> docTable = new TableView<>();
        docTable.setItems(docList);

        TableColumn<DriverDocument, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<DriverDocument, DriverDocument.DocumentType> typeCol = new TableColumn<>("Typ");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        typeCol.setPrefWidth(180);

        TableColumn<DriverDocument, LocalDate> expiryCol = new TableColumn<>("Data ważności");
        expiryCol.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        expiryCol.setPrefWidth(120);

        TableColumn<DriverDocument, String> descCol = new TableColumn<>("Opis");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<DriverDocument, String> pdfCol = new TableColumn<>("PDF");
        pdfCol.setCellValueFactory(cellData -> {
            DriverDocument doc = cellData.getValue();
            String status = doc.hasPdf() ? "Tak (" + doc.getPdfFilename() + ")" : "Brak";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        pdfCol.setPrefWidth(150);

        docTable.getColumns().addAll(idCol, typeCol, expiryCol, descCol, pdfCol);

        Button addDocButton = new Button("Dodaj dokument");
        addDocButton.setOnAction(e -> addDriverDocument(selected, docList));

        Button deleteDocButton = new Button("Usuń dokument");
        deleteDocButton.setOnAction(e -> deleteDriverDocument(docTable.getSelectionModel().getSelectedItem(), docList));

        Button uploadPdfButton = new Button("Dodaj PDF");
        uploadPdfButton.setOnAction(e -> uploadDriverDocumentPdf(docTable.getSelectionModel().getSelectedItem(), docList));

        Button downloadPdfButton = new Button("Pobierz PDF");
        downloadPdfButton.setOnAction(e -> downloadDriverDocumentPdf(docTable.getSelectionModel().getSelectedItem()));

        Button expiringButton = new Button("Wygasające");
        expiringButton.setOnAction(e -> showExpiringDriverDocuments(selected));

        HBox docButtonBox = new HBox(10, addDocButton, deleteDocButton, uploadPdfButton, downloadPdfButton, expiringButton);

        VBox docContent = new VBox(10);
        docContent.setPadding(new Insets(15));
        docContent.getChildren().addAll(
            new Label("Dokumenty kierowcy: " + selected.getFullName()),
            docButtonBox,
            docTable
        );
        VBox.setVgrow(docTable, javafx.scene.layout.Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dokumenty kierowcy");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(docContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();
    }

    private void addDriverDocument(Driver driver, ObservableList<DriverDocument> docList) {
        Dialog<DriverDocument> dialog = new Dialog<>();
        dialog.setTitle("Dodaj dokument kierowcy");
        dialog.setHeaderText("Wprowadź dane dokumentu");

        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        ComboBox<DriverDocument.DocumentType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(DriverDocument.DocumentType.values());
        typeCombo.setValue(DriverDocument.DocumentType.DRIVING_LICENSE);

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusYears(1));

        TextField descField = new TextField();
        descField.setPromptText("Opis (opcjonalnie)");

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Typ dokumentu:"), typeCombo,
            new Label("Data ważności:"), datePicker,
            new Label("Opis:"), descField
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
                showAlert("Sukces", "Dokument został dodany", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Błąd", "Nie można dodać dokumentu: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteDriverDocument(DriverDocument doc, ObservableList<DriverDocument> docList) {
        if (doc == null) {
            showAlert("Błąd", "Wybierz dokument do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć ten dokument?");
        confirm.setContentText(doc.getDocumentType().getDisplayName());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    driverDocumentService.deleteDocument(doc.getId());
                    docList.remove(doc);
                    showAlert("Sukces", "Dokument został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć dokumentu: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void uploadDriverDocumentPdf(DriverDocument doc, ObservableList<DriverDocument> docList) {
        if (doc == null) {
            showAlert("Błąd", "Wybierz dokument, do którego chcesz dodać PDF", Alert.AlertType.WARNING);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"));
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
                showAlert("Sukces", "Plik PDF został dodany", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie można odczytać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void downloadDriverDocumentPdf(DriverDocument doc) {
        if (doc == null) {
            showAlert("Błąd", "Wybierz dokument", Alert.AlertType.WARNING);
            return;
        }
        if (!doc.hasPdf()) {
            showAlert("Błąd", "Wybrany dokument nie ma załączonego PDF", Alert.AlertType.WARNING);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik PDF");
        fileChooser.setInitialFileName(doc.getPdfFilename());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"));
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.write(file.toPath(), doc.getPdfData());
                showAlert("Sukces", "Plik zapisano: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie można zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showExpiringDriverDocuments(Driver driver) {
        var allExpiring = driverDocumentService.getExpiringDocuments();
        var expiringForDriver = allExpiring.stream()
            .filter(d -> d.getDriver().getId().equals(driver.getId()))
            .toList();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Wygasające dokumenty");
        alert.setHeaderText("Dokumenty kierowcy " + driver.getFullName() + " wygasające w ciągu 30 dni:");
        if (expiringForDriver.isEmpty()) {
            alert.setContentText("Brak wygasających dokumentów.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (DriverDocument d : expiringForDriver) {
                sb.append("• ").append(d.getDocumentType().getDisplayName())
                  .append(" – ważny do: ").append(d.getExpiryDate()).append("\n");
            }
            alert.setContentText(sb.toString());
        }
        alert.showAndWait();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
