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
 * Main application controller that manages navigation between the feature views.
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

    // Returns the singleton instance of this controller (used to invalidate the view cache).
    public static MainController getInstance() { return instance; }
    
    // Constructor injection of the authentication service and all feature controllers.
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

    // Swaps the center content to the given view and refreshes its data off the FX thread, using a short-lived cache.
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
    
    // JavaFX lifecycle hook: shows user info, applies role-based access and opens the trucks view.
    @FXML
    public void initialize() {
        instance = this;
        updateUserInfo();
        configureAdminAccess();
        showTruckManagement();
    }

    // Clears the view refresh cache so the next navigation reloads fresh data.
    public void invalidateCache() {
        lastRefreshTime.clear();
    }
    
    // Updates the header labels with the logged-in user's name and role.
    private void updateUserInfo() {
        if (authenticationService.isLoggedIn()) {
            welcomeLabel.setText("Witamy, " + authenticationService.getCurrentUser().getFullName());
            roleLabel.setText("Rola: " + getRoleDisplayName());
        }
    }
    
    // Shows or hides the admin panel button depending on the current user's role.
    private void configureAdminAccess() {
        if (adminButton != null) {
            boolean isAdmin = authenticationService.isLoggedIn() && 
                authenticationService.getCurrentUser().getRole() == User.UserRole.ADMINISTRATOR;
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
    }
    
    // Maps the current user's role enum to its Polish display label.
    private String getRoleDisplayName() {
        return switch (authenticationService.getCurrentUser().getRole()) {
            case ADMINISTRATOR -> "Administrator";
            case LOGISTICIAN -> "Logistyk";
        };
    }
    
    // Shows the trucks management view.
    @FXML
    private void showTruckManagement() {
        showViewAsync("trucks", truckManagementController.getView(),
            truckManagementController::refreshData);
    }
    
    // Shows the trailers management view.
    @FXML
    private void showTrailerManagement() {
        showViewAsync("trailers", trailerManagementController.getView(),
            trailerManagementController::refreshData);
    }

    // Shows the documents management view.
    @FXML
    private void showDocumentManagement() {
        showViewAsync("documents", documentManagementController.getView(),
            documentManagementController::refreshData);
    }
    
    // Shows the drivers management view.
    @FXML
    private void showDriverManagement() {
        showViewAsync("drivers", driverManagementController.getView(),
            driverManagementController::refreshData);
    }
    
    // Shows the trips (active routes) management view.
    @FXML
    private void showTripManagement() {
        showViewAsync("trips", tripManagementController.getView(),
            tripManagementController::refreshData);
    }
    
    // Shows the admin user-management view (administrators only).
    @FXML
    private void showUserManagement() {
        if (!authenticationService.isLoggedIn() || 
            authenticationService.getCurrentUser().getRole() != User.UserRole.ADMINISTRATOR) {
            return;
        }
        showViewAsync("users", userManagementController.getView(),
            userManagementController::refreshData);
    }
    
    // Logs the current user out and returns to the login screen.
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
