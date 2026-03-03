package org.example.fleetmanagement.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.example.fleetmanagement.FleetManagementApplication;
import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final long CACHE_TTL_MS = 30_000;
    private final Map<String, Long> lastRefreshTime = new ConcurrentHashMap<>();
    private static MainController instance;

    public static MainController getInstance() { return instance; }
    
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

    private void showViewAsync(String key, javafx.scene.Parent view, Runnable refreshAction) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);

        long now = System.currentTimeMillis();
        Long last = lastRefreshTime.get(key);
        if (last != null && (now - last) < CACHE_TTL_MS) {
            return;
        }

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        contentArea.getChildren().add(spinner);

        Thread.ofVirtual().start(() -> {
            try {
                refreshAction.run();
                lastRefreshTime.put(key, System.currentTimeMillis());
            } finally {
                Platform.runLater(() -> contentArea.getChildren().remove(spinner));
            }
        });
    }
    
    /**
     * Инициализация контроллера
     */
    @FXML
    public void initialize() {
        instance = this;
        updateUserInfo();
        configureAdminAccess();
        showTruckManagement();
    }

    public void invalidateCache() {
        lastRefreshTime.clear();
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
    
    @FXML
    private void showTruckManagement() {
        showViewAsync("trucks", truckManagementController.getView(),
            truckManagementController::refreshData);
    }
    
    @FXML
    private void showTrailerManagement() {
        showViewAsync("trailers", trailerManagementController.getView(),
            trailerManagementController::refreshData);
    }

    @FXML
    private void showDocumentManagement() {
        showViewAsync("documents", documentManagementController.getView(),
            documentManagementController::refreshData);
    }
    
    @FXML
    private void showDriverManagement() {
        showViewAsync("drivers", driverManagementController.getView(),
            driverManagementController::refreshData);
    }
    
    @FXML
    private void showTripManagement() {
        showViewAsync("trips", tripManagementController.getView(),
            tripManagementController::refreshData);
    }
    
    @FXML
    private void showUserManagement() {
        if (!authenticationService.isLoggedIn() || 
            authenticationService.getCurrentUser().getRole() != User.UserRole.ADMINISTRATOR) {
            return;
        }
        showViewAsync("users", userManagementController.getView(),
            userManagementController::refreshData);
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
