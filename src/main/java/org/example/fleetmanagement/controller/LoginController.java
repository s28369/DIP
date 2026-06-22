package org.example.fleetmanagement.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.fleetmanagement.FleetManagementApplication;
import org.example.fleetmanagement.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controller handling the user login screen.
 */
@Component
public class LoginController {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    private final AuthenticationService authenticationService;
    
    // Constructor injection of the authentication service.
    @Autowired
    public LoginController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    // JavaFX lifecycle hook called after the FXML is loaded.
    @FXML
    public void initialize() {
    }
    
    // Validates input, authenticates the user and opens the main screen on success.
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Błąd", "Proszę wypełnić wszystkie pola", Alert.AlertType.ERROR);
            return;
        }
        
        if (authenticationService.login(username, password)) {
            try {
                FleetManagementApplication.showMainScreen();
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się załadować ekranu głównego: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        } else {
            showAlert("Błąd logowania", "Nieprawidłowa nazwa użytkownika lub hasło", 
                Alert.AlertType.ERROR);
        }
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
