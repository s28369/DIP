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
 * Главный контроллер приложения, управляющий представлениями
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
    private final TrailerManagementController trailerManagementController;
    private final DocumentManagementController documentManagementController;
    private final UserManagementController userManagementController;
    private final DriverManagementController driverManagementController;
    private final TripManagementController tripManagementController;
    
    @Autowired
    public MainController(
        AuthenticationService authenticationService,
        TruckManagementController truckManagementController,
        TrailerManagementController trailerManagementController,
        DocumentManagementController documentManagementController,
        UserManagementController userManagementController,
        DriverManagementController driverManagementController,
        TripManagementController tripManagementController
    ) {
        this.authenticationService = authenticationService;
        this.truckManagementController = truckManagementController;
        this.trailerManagementController = trailerManagementController;
        this.documentManagementController = documentManagementController;
        this.userManagementController = userManagementController;
        this.driverManagementController = driverManagementController;
        this.tripManagementController = tripManagementController;
    }
    
    /**
     * Инициализация контроллера
     */
    @FXML
    public void initialize() {
        updateUserInfo();
        configureAdminAccess();
        showTruckManagement();
    }
    
    /**
     * Обновляет информацию о залогированном пользователе
     */
    private void updateUserInfo() {
        if (authenticationService.isLoggedIn()) {
            welcomeLabel.setText("Добро пожаловать, " + authenticationService.getCurrentUser().getFullName());
            roleLabel.setText("Роль: " + getRoleDisplayName());
        }
    }
    
    /**
     * Настраивает доступ к панели администратора
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
     * Возвращает название роли пользователя для отображения
     */
    private String getRoleDisplayName() {
        return switch (authenticationService.getCurrentUser().getRole()) {
            case ADMINISTRATOR -> "Администратор";
            case LOGISTICIAN -> "Логист";
        };
    }
    
    /**
     * Отображает представление управления автопарком
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
    
    @FXML
    private void showTrailerManagement() {
        try {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(trailerManagementController.getView());
            trailerManagementController.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Отображает представление управления документами
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
     * Отображает представление управления водителями
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
     * Отображает представление активных рейсов
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
     * Отображает панель администратора — управление пользователями
     */
    @FXML
    private void showUserManagement() {
        // Проверка прав доступа
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
     * Обработка выхода из системы
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
