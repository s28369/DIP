package org.example.fleetmanagement.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.fleetmanagement.FleetManagementApplication;
import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Główny kontroler aplikacji zarządzający widokami
 */
@Component
public class MainController {
    
    @FXML
    private Label welcomeLabel;
    
    @FXML
    private Label roleLabel;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private BorderPane mainPane;
    
    @FXML
    private Button adminButton;
    
    private final AuthenticationService authenticationService;
    private final TruckManagementController truckManagementController;
    private final DocumentManagementController documentManagementController;
    private final UserManagementController userManagementController;
    private final DriverManagementController driverManagementController;
    private final TripManagementController tripManagementController;
    
    @Autowired
    public MainController(
        AuthenticationService authenticationService,
        TruckManagementController truckManagementController,
        DocumentManagementController documentManagementController,
        UserManagementController userManagementController,
        DriverManagementController driverManagementController,
        TripManagementController tripManagementController
    ) {
        this.authenticationService = authenticationService;
        this.truckManagementController = truckManagementController;
        this.documentManagementController = documentManagementController;
        this.userManagementController = userManagementController;
        this.driverManagementController = driverManagementController;
        this.tripManagementController = tripManagementController;
    }
    
    /**
     * Inicjalizacja kontrolera
     */
    @FXML
    public void initialize() {
        updateUserInfo();
        configureAdminAccess();
        showTruckManagement();
    }
    
    /**
     * Aktualizuje informacje o zalogowanym użytkowniku
     */
    private void updateUserInfo() {
        if (authenticationService.isLoggedIn()) {
            welcomeLabel.setText("Witaj, " + authenticationService.getCurrentUser().getFullName());
            roleLabel.setText("Rola: " + getRoleDisplayName());
        }
    }
    
    /**
     * Konfiguruje dostęp do panelu administratora
     */
    private void configureAdminAccess() {
        if (adminButton != null) {
            boolean isAdmin = authenticationService.isLoggedIn() && 
                authenticationService.getCurrentUser().getRole() == User.UserRole.ADMINISTRATOR;
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
    }
    
    /**
     * Zwraca nazwę roli użytkownika do wyświetlenia
     */
    private String getRoleDisplayName() {
        return switch (authenticationService.getCurrentUser().getRole()) {
            case ADMINISTRATOR -> "Administrator";
            case LOGISTICIAN -> "Logistyk";
        };
    }
    
    /**
     * Wyświetla widok zarządzania flotą
     */
    @FXML
    private void showTruckManagement() {
        try {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(truckManagementController.getView());
            truckManagementController.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Wyświetla widok zarządzania dokumentami
     */
    @FXML
    private void showDocumentManagement() {
        try {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(documentManagementController.getView());
            documentManagementController.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Wyświetla widok zarządzania kierowcami
     */
    @FXML
    private void showDriverManagement() {
        try {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(driverManagementController.getView());
            driverManagementController.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Wyświetla widok aktywnych rejsów
     */
    @FXML
    private void showTripManagement() {
        try {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(tripManagementController.getView());
            tripManagementController.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Wyświetla panel administratora - zarządzanie użytkownikami
     */
    @FXML
    private void showUserManagement() {
        // Sprawdź uprawnienia
        if (!authenticationService.isLoggedIn() || 
            authenticationService.getCurrentUser().getRole() != User.UserRole.ADMINISTRATOR) {
            return;
        }
        
        try {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(userManagementController.getView());
            userManagementController.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Obsługa wylogowania
     */
    @FXML
    private void handleLogout() {
        authenticationService.logout();
        try {
            FleetManagementApplication.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
