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
 * Kontroler obsługujący logowanie użytkownika
 */
@Component
public class LoginController {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    private final AuthenticationService authenticationService;
    
    @Autowired
    public LoginController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    /**
     * Inicjalizacja kontrolera
     */
    @FXML
    public void initialize() {
        usernameField.setText("admin");
        passwordField.setText("admin123");
    }
    
    /**
     * Obsługa przycisku logowania
     */
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
                showAlert("Błąd", "Nie można załadować głównego ekranu: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        } else {
            showAlert("Błąd logowania", "Nieprawidłowa nazwa użytkownika lub hasło", 
                Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Wyświetla okno dialogowe z komunikatem
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
