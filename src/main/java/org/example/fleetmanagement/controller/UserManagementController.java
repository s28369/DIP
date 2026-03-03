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
 * Контроллер управления панелью администратора — пользователи
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
     * Инициализирует представление управления пользователями
     */
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Панель администратора — Управление пользователями");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");

        Label infoLabel = new Label("Доступ только для администраторов");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Button addButton = new Button("Добавить пользователя");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddUserDialog());
        
        Button editButton = new Button("Редактировать пользователя");
        editButton.setOnAction(e -> showEditUserDialog());
        
        Button deleteButton = new Button("Удалить пользователя");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDeleteUser());
        
        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });
        
        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, refreshButton);

        tableView = new TableView<>();
        tableView.setItems(userList);
        
        TableColumn<User, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        
        TableColumn<User, String> usernameColumn = new TableColumn<>("Логин");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(150);
        
        TableColumn<User, String> fullNameColumn = new TableColumn<>("ФИО");
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        fullNameColumn.setPrefWidth(200);
        
        TableColumn<User, User.UserRole> roleColumn = new TableColumn<>("Роль");
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleColumn.setPrefWidth(150);

        TableColumn<User, String> passwordColumn = new TableColumn<>("Пароль");
        passwordColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty("********"));
        passwordColumn.setPrefWidth(100);
        
        tableView.getColumns().addAll(idColumn, usernameColumn, fullNameColumn, roleColumn, passwordColumn);
        
        view.getChildren().addAll(titleLabel, infoLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Возвращает представление контроллера
     */
    public Parent getView() {
        return view;
    }
    
    /**
     * Обновляет данные в таблице
     */
    public void refreshData() {
        var data = userService.getAllUsers();
        if (javafx.application.Platform.isFxApplicationThread()) {
            userList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> userList.setAll(data));
        }
    }
    
    /**
     * Отображает диалог добавления нового пользователя
     */
    private void showAddUserDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Добавить пользователя");
        dialog.setHeaderText("Введите данные нового пользователя");
        
        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Логин (напр. ivan.petrov)");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Подтвердите пароль");
        
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("ФИО (напр. Иван Петров)");
        
        ComboBox<User.UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(User.UserRole.values());
        roleComboBox.setValue(User.UserRole.LOGISTICIAN);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Логин:"), usernameField,
            new Label("Пароль:"), passwordField,
            new Label("Подтвердите пароль:"), confirmPasswordField,
            new Label("ФИО:"), fullNameField,
            new Label("Роль:"), roleComboBox
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (usernameField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Логин не может быть пустым", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (passwordField.getText().isEmpty()) {
                showAlert("Ошибка", "Пароль не может быть пустым", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                showAlert("Ошибка", "Пароли не совпадают", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (fullNameField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "ФИО не может быть пустым", Alert.AlertType.ERROR);
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
                showAlert("Успех", "Пользователь " + user.getUsername() + " добавлен", 
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось добавить пользователя: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Отображает диалог редактирования пользователя
     */
    private void showEditUserDialog() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            showAlert("Ошибка", "Выберите пользователя для редактирования", Alert.AlertType.WARNING);
            return;
        }
        
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Редактировать пользователя");
        dialog.setHeaderText("Редактирование пользователя: " + selectedUser.getUsername());
        
        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        TextField fullNameField = new TextField(selectedUser.getFullName());
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Новый пароль (оставьте пустым, чтобы не менять)");
        
        ComboBox<User.UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(User.UserRole.values());
        roleComboBox.setValue(selectedUser.getRole());
        

        Label warningLabel = new Label();
        if (selectedUser.getId().equals(authenticationService.getCurrentUser().getId())) {
            warningLabel.setText("⚠ Вы редактируете свою учётную запись!");
            warningLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        }
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            warningLabel,
            new Label("Логин (нельзя изменить):"), 
            new Label(selectedUser.getUsername()),
            new Label("ФИО:"), fullNameField,
            new Label("Новый пароль:"), newPasswordField,
            new Label("Роль:"), roleComboBox
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
                showAlert("Успех", "Данные пользователя обновлены", 
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось обновить пользователя: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Обрабатывает удаление пользователя
     */
    private void handleDeleteUser() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            showAlert("Ошибка", "Выберите пользователя для удаления", Alert.AlertType.WARNING);
            return;
        }
        

        if (selectedUser.getId().equals(authenticationService.getCurrentUser().getId())) {
            showAlert("Ошибка", "Вы не можете удалить свою учётную запись!", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Вы уверены, что хотите удалить этого пользователя?");
        confirmAlert.setContentText("Логин: " + selectedUser.getUsername() + 
            "\nФИО: " + selectedUser.getFullName() +
            "\nРоль: " + selectedUser.getRole());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.deleteUser(selectedUser.getId());
                    refreshData();
                    showAlert("Успех", "Пользователь удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить пользователя: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
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
