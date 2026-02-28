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
 * Контроллер, обрабатывающий вход пользователя в систему
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
     * Инициализация контроллера
     */
    @FXML
    public void initialize() {
    }
    
    /**
     * Обработка нажатия кнопки входа
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Ошибка", "Пожалуйста, заполните все поля", Alert.AlertType.ERROR);
            return;
        }
        
        if (authenticationService.login(username, password)) {
            try {
                FleetManagementApplication.showMainScreen();
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось загрузить главный экран: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        } else {
            showAlert("Ошибка входа", "Неверное имя пользователя или пароль", 
                Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Отображает диалоговое окно с сообщением
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
