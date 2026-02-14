package org.example.fleetmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Kontroler zarządzający widokiem kierowców
 */
@Component
public class DriverManagementController {
    
    private final DriverService driverService;
    private final ObservableList<Driver> driverList = FXCollections.observableArrayList();
    private VBox view;
    private TableView<Driver> tableView;
    
    @Autowired
    public DriverManagementController(DriverService driverService) {
        this.driverService = driverService;
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
        
        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, refreshButton);

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
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
