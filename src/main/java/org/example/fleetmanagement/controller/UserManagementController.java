package org.example.fleetmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.service.AuthenticationService;
import org.example.fleetmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Kontroler zarządzający panelem administratora - użytkownicy
 */
@Component
public class UserManagementController {
    
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private VBox view;
    private TableView<User> tableView;
    
    @Autowired
    public UserManagementController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        initializeView();
    }
    
    /**
     * Inicjalizuje widok zarządzania użytkownikami
     */
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Panel Administratora - Zarządzanie Użytkownikami");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");

        Label infoLabel = new Label("Tylko administratorzy mają dostęp do tego panelu");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Button addButton = new Button("Dodaj użytkownika");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddUserDialog());
        
        Button editButton = new Button("Edytuj użytkownika");
        editButton.setOnAction(e -> showEditUserDialog());
        
        Button deleteButton = new Button("Usuń użytkownika");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteUser());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> refreshData());
        
        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, refreshButton);

        tableView = new TableView<>();
        tableView.setItems(userList);
        
        TableColumn<User, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        
        TableColumn<User, String> usernameColumn = new TableColumn<>("Login");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(150);
        
        TableColumn<User, String> fullNameColumn = new TableColumn<>("Imię i nazwisko");
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        fullNameColumn.setPrefWidth(200);
        
        TableColumn<User, User.UserRole> roleColumn = new TableColumn<>("Rola");
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleColumn.setPrefWidth(150);

        TableColumn<User, String> passwordColumn = new TableColumn<>("Hasło");
        passwordColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty("********"));
        passwordColumn.setPrefWidth(100);
        
        tableView.getColumns().addAll(idColumn, usernameColumn, fullNameColumn, roleColumn, passwordColumn);
        
        view.getChildren().addAll(titleLabel, infoLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Zwraca widok kontrolera
     */
    public Parent getView() {
        return view;
    }
    
    /**
     * Odświeża dane w tabeli
     */
    public void refreshData() {
        userList.clear();
        userList.addAll(userService.getAllUsers());
    }
    
    /**
     * Wyświetla dialog dodawania nowego użytkownika
     */
    private void showAddUserDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Dodaj użytkownika");
        dialog.setHeaderText("Wprowadź dane nowego użytkownika");
        
        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Login (np. jan.kowalski)");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Hasło");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Potwierdź hasło");
        
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Imię i nazwisko (np. Jan Kowalski)");
        
        ComboBox<User.UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(User.UserRole.values());
        roleComboBox.setValue(User.UserRole.LOGISTICIAN);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Login:"), usernameField,
            new Label("Hasło:"), passwordField,
            new Label("Potwierdź hasło:"), confirmPasswordField,
            new Label("Imię i nazwisko:"), fullNameField,
            new Label("Rola:"), roleComboBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (usernameField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Login nie może być pusty", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (passwordField.getText().isEmpty()) {
                showAlert("Błąd", "Hasło nie może być puste", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                showAlert("Błąd", "Hasła nie są zgodne", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (fullNameField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Imię i nazwisko nie może być puste", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                User user = new User();
                user.setUsername(usernameField.getText().trim());
                user.setPassword(passwordField.getText());
                user.setFullName(fullNameField.getText().trim());
                user.setRole(roleComboBox.getValue());
                return user;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(user -> {
            try {
                userService.addUser(user);
                refreshData();
                showAlert("Sukces", "Użytkownik " + user.getUsername() + " został dodany", 
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można dodać użytkownika: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Wyświetla dialog edycji użytkownika
     */
    private void showEditUserDialog() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            showAlert("Błąd", "Proszę wybrać użytkownika do edycji", Alert.AlertType.WARNING);
            return;
        }
        
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edytuj użytkownika");
        dialog.setHeaderText("Edycja użytkownika: " + selectedUser.getUsername());
        
        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        TextField fullNameField = new TextField(selectedUser.getFullName());
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nowe hasło (zostaw puste aby nie zmieniać)");
        
        ComboBox<User.UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(User.UserRole.values());
        roleComboBox.setValue(selectedUser.getRole());
        

        Label warningLabel = new Label();
        if (selectedUser.getId().equals(authenticationService.getCurrentUser().getId())) {
            warningLabel.setText("⚠ Edytujesz własne konto!");
            warningLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        }
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            warningLabel,
            new Label("Login (nie można zmienić):"), 
            new Label(selectedUser.getUsername()),
            new Label("Imię i nazwisko:"), fullNameField,
            new Label("Nowe hasło:"), newPasswordField,
            new Label("Rola:"), roleComboBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selectedUser.setFullName(fullNameField.getText().trim());
                selectedUser.setRole(roleComboBox.getValue());
                if (!newPasswordField.getText().isEmpty()) {
                    selectedUser.setPassword(newPasswordField.getText());
                }
                return selectedUser;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(user -> {
            try {
                userService.updateUser(user);
                refreshData();
                showAlert("Sukces", "Dane użytkownika zostały zaktualizowane", 
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie można zaktualizować użytkownika: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Obsługuje usuwanie użytkownika
     */
    private void handleDeleteUser() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            showAlert("Błąd", "Proszę wybrać użytkownika do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        

        if (selectedUser.getId().equals(authenticationService.getCurrentUser().getId())) {
            showAlert("Błąd", "Nie możesz usunąć własnego konta!", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz usunąć tego użytkownika?");
        confirmAlert.setContentText("Login: " + selectedUser.getUsername() + 
            "\nImię i nazwisko: " + selectedUser.getFullName() +
            "\nRola: " + selectedUser.getRole());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.deleteUser(selectedUser.getId());
                    refreshData();
                    showAlert("Sukces", "Użytkownik został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć użytkownika: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
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
