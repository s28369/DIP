package org.example.fleetmanagement.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.fleetmanagement.model.Trailer;
import org.example.fleetmanagement.model.TrailerAttachment;
import org.example.fleetmanagement.model.TrailerNote;
import org.example.fleetmanagement.repository.TrailerAttachmentRepository;
import org.example.fleetmanagement.service.TrailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class TrailerManagementController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TrailerService trailerService;
    private final TrailerAttachmentRepository attachmentRepository;
    private final ObservableList<Trailer> trailerList = FXCollections.observableArrayList();
    private FilteredList<Trailer> filteredList;
    private VBox view;
    private TableView<Trailer> tableView;

    @Autowired
    public TrailerManagementController(TrailerService trailerService,
                                       TrailerAttachmentRepository attachmentRepository) {
        this.trailerService = trailerService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }

    private ComboBox<String> createEditableComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setEditable(true);
        combo.setPrefWidth(300);
        return combo;
    }

    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Управление прицепами");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Добавить прицеп");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddDialog());

        Button editButton = new Button("Редактировать");
        editButton.setOnAction(e -> showEditDialog());

        Button notesButton = new Button("Заметки");
        notesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        notesButton.setOnAction(e -> showNotesDialog());

        Button attachmentsButton = new Button("Вложения PDF");
        attachmentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());

        Button deleteButton = new Button("Удалить прицеп");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDelete());

        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });

        HBox buttonBox = new HBox(10, addButton, editButton, notesButton, attachmentsButton, deleteButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Все", "Номер", "Марка", "Страна", "Статус", "Местоположение");
        searchParam.setValue("Все");

        filteredList = new FilteredList<>(trailerList, p -> true);

        Runnable applyFilter = () -> {
            String text = searchField.getText();
            String param = searchParam.getValue();
            if (text == null || text.trim().isEmpty()) {
                filteredList.setPredicate(p -> true);
                return;
            }
            String lower = text.trim().toLowerCase();
            filteredList.setPredicate(t -> switch (param) {
                case "Номер" -> contains(t.getRegistrationNumber(), lower);
                case "Марка" -> contains(t.getBrand(), lower);
                case "Страна" -> contains(t.getRegistrationCountry(), lower);
                case "Статус" -> contains(t.getStatus(), lower);
                case "Местоположение" -> contains(t.getCurrentLocation(), lower);
                default -> contains(t.getRegistrationNumber(), lower)
                        || contains(t.getBrand(), lower)
                        || contains(t.getRegistrationCountry(), lower)
                        || contains(t.getStatus(), lower)
                        || contains(t.getCurrentLocation(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);

        TableColumn<Trailer, String> regCol = new TableColumn<>("Номер");
        regCol.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        regCol.setPrefWidth(150);

        TableColumn<Trailer, String> brandCol = new TableColumn<>("Марка");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));
        brandCol.setPrefWidth(160);

        TableColumn<Trailer, String> countryCol = new TableColumn<>("Страна регистрации");
        countryCol.setCellValueFactory(new PropertyValueFactory<>("registrationCountry"));
        countryCol.setPrefWidth(150);

        TableColumn<Trailer, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        TableColumn<Trailer, String> locationCol = new TableColumn<>("Местоположение");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("currentLocation"));
        locationCol.setPrefWidth(180);

        TableColumn<Trailer, String> notesCol = new TableColumn<>("Заметки");
        notesCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getNoteCount();
            return new SimpleStringProperty(count > 0 ? count + " шт." : "—");
        });
        notesCol.setPrefWidth(80);

        tableView.getColumns().addAll(regCol, brandCol, countryCol, statusCol, locationCol, notesCol);

        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Trailer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getExpirationStyle(item.getAttachments()));
                }
            }
        });

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                showAttachmentsDialog();
            }
        });

        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
    }

    public Parent getView() {
        return view;
    }

    public void refreshData() {
        var data = trailerService.getAllTrailers();
        if (javafx.application.Platform.isFxApplicationThread()) {
            trailerList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> trailerList.setAll(data));
        }
    }

    // ---- Add ----

    private void showAddDialog() {
        Dialog<Trailer> dialog = new Dialog<>();
        dialog.setTitle("Добавить прицеп");
        dialog.setHeaderText("Введите данные нового прицепа");

        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField regField = new TextField();
        regField.setPromptText("Номер (напр. PO12345)");

        TextField brandField = new TextField();
        brandField.setPromptText("Марка (напр. Schmitz Cargobull)");

        ComboBox<String> countryCombo = createEditableComboBox(
                "Польша", "Беларусь", "Чехия", "РФ");

        ComboBox<String> statusCombo = createEditableComboBox(
                Trailer.STATUS_AVAILABLE, Trailer.STATUS_ON_TRIP, Trailer.STATUS_MAINTENANCE);
        statusCombo.setValue(Trailer.STATUS_AVAILABLE);

        TextField locationField = new TextField();
        locationField.setPromptText("напр. Варшава, база");

        VBox content = new VBox(10,
            new Label("Номер:"), regField,
            new Label("Марка:"), brandField,
            new Label("Страна регистрации:"), countryCombo,
            new Label("Статус:"), statusCombo,
            new Label("Местоположение:"), locationField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (regField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Номер не может быть пустым", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (brandField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Марка не может быть пустой", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                Trailer t = new Trailer();
                t.setRegistrationNumber(regField.getText().trim());
                t.setBrand(brandField.getText().trim());
                String country = countryCombo.getEditor().getText();
                t.setRegistrationCountry(country != null ? country.trim() : "");
                String status = statusCombo.getEditor().getText();
                t.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Trailer.STATUS_AVAILABLE);
                t.setCurrentLocation(locationField.getText().trim());
                return t;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(trailer -> runAsync(() -> {
            trailerService.addTrailer(trailer);
            refreshData();
        }, "Прицеп добавлен", "Не удалось добавить прицеп"));
    }

    // ---- Edit ----

    private void showEditDialog() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите прицеп для редактирования", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Trailer> dialog = new Dialog<>();
        dialog.setTitle("Редактировать прицеп");
        dialog.setHeaderText("Прицеп: " + selected.getRegistrationNumber());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField regField = new TextField(selected.getRegistrationNumber());
        TextField brandField = new TextField(selected.getBrand());

        ComboBox<String> countryCombo = createEditableComboBox(
                "Польша", "Беларусь", "Чехия", "РФ");
        if (selected.getRegistrationCountry() != null) {
            countryCombo.setValue(selected.getRegistrationCountry());
        }

        ComboBox<String> statusCombo = createEditableComboBox(
                Trailer.STATUS_AVAILABLE, Trailer.STATUS_ON_TRIP, Trailer.STATUS_MAINTENANCE);
        statusCombo.setValue(selected.getStatus());

        TextField locationField = new TextField(
                selected.getCurrentLocation() != null ? selected.getCurrentLocation() : "");

        VBox content = new VBox(10,
            new Label("Номер:"), regField,
            new Label("Марка:"), brandField,
            new Label("Страна регистрации:"), countryCombo,
            new Label("Статус:"), statusCombo,
            new Label("Местоположение:"), locationField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                selected.setRegistrationNumber(regField.getText().trim());
                selected.setBrand(brandField.getText().trim());
                String country = countryCombo.getEditor().getText();
                selected.setRegistrationCountry(country != null ? country.trim() : "");
                String status = statusCombo.getEditor().getText();
                selected.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : Trailer.STATUS_AVAILABLE);
                selected.setCurrentLocation(locationField.getText().trim());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(trailer -> runAsync(() -> {
            trailerService.updateTrailer(trailer);
            refreshData();
        }, "Данные прицепа обновлены", "Не удалось обновить прицеп"));
    }

    // ---- Delete ----

    private void handleDelete() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите прицеп для удаления", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Вы уверены, что хотите удалить этот прицеп?");
        confirm.setContentText("Номер: " + selected.getRegistrationNumber() +
            "\nМарка: " + selected.getBrand());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runAsync(() -> {
                    trailerService.deleteTrailer(selected.getId());
                    refreshData();
                }, "Прицеп удалён", "Не удалось удалить прицеп");
            }
        });
    }

    // ---- Notes dialog ----

    private void showNotesDialog() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите прицеп для просмотра заметок", Alert.AlertType.WARNING);
            return;
        }

        ObservableList<TrailerNote> noteList = FXCollections.observableArrayList();
        noteList.addAll(trailerService.getNotesByTrailer(selected.getId()));

        TableView<TrailerNote> noteTable = new TableView<>();
        noteTable.setItems(noteList);

        TableColumn<TrailerNote, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TrailerNote, String> contentCol = new TableColumn<>("Текст заметки");
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        contentCol.setPrefWidth(350);

        TableColumn<TrailerNote, String> dateCol = new TableColumn<>("Дата создания");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DT_FMT));
            }
            return new SimpleStringProperty("—");
        });
        dateCol.setPrefWidth(140);

        noteTable.getColumns().addAll(idCol, contentCol, dateCol);

        Button addNoteBtn = new Button("Добавить заметку");
        addNoteBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addNoteBtn.setOnAction(e -> addNote(selected, noteList));

        Button editNoteBtn = new Button("Редактировать");
        editNoteBtn.setOnAction(e -> {
            TrailerNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editNote(sel, noteList, selected);
            } else {
                showAlert("Ошибка", "Выберите заметку для редактирования", Alert.AlertType.WARNING);
            }
        });

        Button deleteNoteBtn = new Button("Удалить");
        deleteNoteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteNoteBtn.setOnAction(e -> {
            TrailerNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteNote(sel, noteList, selected);
            } else {
                showAlert("Ошибка", "Выберите заметку для удаления", Alert.AlertType.WARNING);
            }
        });

        HBox noteBtnBox = new HBox(10, addNoteBtn, editNoteBtn, deleteNoteBtn);
        noteBtnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Заметки прицепа: " + selected.getRegistrationNumber() + " (" + selected.getBrand() + ")"),
            noteTable,
            noteBtnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(noteTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Заметки прицепа");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();

        refreshData();
    }

    private void addNote(Trailer trailer, ObservableList<TrailerNote> noteList) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Новая заметка");
        dlg.setHeaderText("Введите текст заметки");
        dlg.setContentText("Заметка:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    TrailerNote note = new TrailerNote(text.trim(), trailer);
                    trailerService.addNote(note);
                    noteList.setAll(trailerService.getNotesByTrailer(trailer.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось добавить заметку: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void editNote(TrailerNote note, ObservableList<TrailerNote> noteList, Trailer trailer) {
        TextInputDialog dlg = new TextInputDialog(note.getContent());
        dlg.setTitle("Редактировать заметку");
        dlg.setHeaderText("Измените текст заметки");
        dlg.setContentText("Заметка:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    note.setContent(text.trim());
                    trailerService.updateNote(note);
                    noteList.setAll(trailerService.getNotesByTrailer(trailer.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось обновить заметку: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deleteNote(TrailerNote note, ObservableList<TrailerNote> noteList, Trailer trailer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить эту заметку?");
        confirm.setContentText(note.getContent());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    trailerService.deleteNote(note.getId());
                    noteList.setAll(trailerService.getNotesByTrailer(trailer.getId()));
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить заметку: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Attachments dialog ----

    private void showAttachmentsDialog() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите прицеп", Alert.AlertType.WARNING);
            return;
        }

        Trailer trailer = trailerService.getTrailerById(selected.getId()).orElse(selected);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Вложения PDF");
        dialog.setHeaderText("Прицеп: " + trailer.getBrand() + " (" + trailer.getRegistrationNumber() + ")");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TrailerAttachment> attachmentTable = new TableView<>();
        ObservableList<TrailerAttachment> attachmentList = FXCollections.observableArrayList(trailer.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);

        TableColumn<TrailerAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TrailerAttachment, String> nameCol = new TableColumn<>("Имя файла");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);

        TableColumn<TrailerAttachment, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);

        TableColumn<TrailerAttachment, String> sizeCol = new TableColumn<>("Размер");
        sizeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);

        TableColumn<TrailerAttachment, String> dateCol = new TableColumn<>("Дата добавления");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getUploadedAt().format(DT_FMT));
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);

        TableColumn<TrailerAttachment, String> expCol = new TableColumn<>("Дата окончания");
        expCol.setCellValueFactory(cellData -> {
            LocalDate exp = cellData.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? exp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "—");
        });
        expCol.setPrefWidth(120);

        attachmentTable.getColumns().addAll(idCol, nameCol, descCol, sizeCol, dateCol, expCol);

        attachmentTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TrailerAttachment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getExpirationDate() == null) {
                    setStyle("");
                } else {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), item.getExpirationDate());
                    if (days <= 7) {
                        setStyle("-fx-background-color: #ffcdd2;");
                    } else if (days <= 30) {
                        setStyle("-fx-background-color: #fff9c4;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        Button addBtn = new Button("Добавить PDF");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            addAttachmentToTrailer(trailer, attachmentList);
            refreshData();
        });

        Button downloadBtn = new Button("Скачать PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                downloadAttachment(sel);
            } else {
                showAlert("Ошибка", "Выберите вложение для скачивания", Alert.AlertType.WARNING);
            }
        });

        Button editDescBtn = new Button("Изменить описание");
        editDescBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        editDescBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editAttachmentDescription(sel, attachmentList, trailer);
            } else {
                showAlert("Ошибка", "Выберите вложение", Alert.AlertType.WARNING);
            }
        });

        Button expDateBtn = new Button("Дата окончания");
        expDateBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        expDateBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editExpirationDate(sel, attachmentList, trailer);
            } else {
                showAlert("Ошибка", "Выберите вложение", Alert.AlertType.WARNING);
            }
        });

        Button deleteBtn = new Button("Удалить вложение");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteAttachment(sel, attachmentList);
                refreshData();
            } else {
                showAlert("Ошибка", "Выберите вложение для удаления", Alert.AlertType.WARNING);
            }
        });

        HBox buttonBox = new HBox(10, addBtn, downloadBtn, editDescBtn, expDateBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Список вложений PDF:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();
    }

    private void addAttachmentToTrailer(Trailer trailer, ObservableList<TrailerAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файлы");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Все поддерживаемые", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.jpeg", "*.png"));

        Stage stage = (Stage) view.getScene().getWindow();
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            int added = 0;
            for (File file : files) {
                try {
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    TrailerAttachment attachment = new TrailerAttachment(file.getName(), "", fileData, trailer);
                    attachmentRepository.save(attachment);
                    attachmentList.add(attachment);
                    added++;
                } catch (IOException e) {
                    showAlert("Ошибка", "Не удалось прочитать файл: " + file.getName() + "\n" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            if (added > 0) {
                showAlert("Успех", "Добавлено файлов: " + added, Alert.AlertType.INFORMATION);
            }
        }
    }

    private void editAttachmentDescription(TrailerAttachment attachment, ObservableList<TrailerAttachment> attachmentList, Trailer trailer) {
        TextInputDialog dlg = new TextInputDialog(attachment.getDescription() != null ? attachment.getDescription() : "");
        dlg.setTitle("Описание документа");
        dlg.setHeaderText("Файл: " + attachment.getFilename());
        dlg.setContentText("Описание:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            try {
                attachment.setDescription(text.trim());
                attachmentRepository.save(attachment);
                attachmentList.setAll(
                    new java.util.ArrayList<>(trailerService.getTrailerById(trailer.getId()).map(Trailer::getAttachments).orElse(java.util.Set.of())));
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось сохранить описание: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void downloadAttachment(TrailerAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл");
        fileChooser.setInitialFileName(attachment.getFilename());
        String ext = getExtension(attachment.getFilename());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(ext.toUpperCase() + " файлы", "*." + ext),
            new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));

        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            file = ensureExtension(file, ext);
            try {
                byte[] data = attachmentRepository.findFileDataById(attachment.getId());
                Files.write(file.toPath(), data);
                showAlert("Успех", "Файл сохранён как:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось сохранить файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void deleteAttachment(TrailerAttachment attachment, ObservableList<TrailerAttachment> attachmentList) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Вы уверены, что хотите удалить это вложение?");
        confirm.setContentText("Файл: " + attachment.getFilename());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    attachmentRepository.delete(attachment);
                    attachmentList.remove(attachment);
                    showAlert("Успех", "Вложение удалено", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить вложение: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void editExpirationDate(TrailerAttachment attachment, ObservableList<TrailerAttachment> attachmentList, Trailer trailer) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Дата окончания документа");
        dlg.setHeaderText("Файл: " + attachment.getFilename());

        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("Убрать дату", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, clearType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(attachment.getExpirationDate());
        VBox content = new VBox(10, new Label("Дата окончания:"), datePicker);
        content.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(content);

        dlg.setResultConverter(btn -> {
            if (btn == saveType) return datePicker.getValue();
            if (btn == clearType) return LocalDate.MIN;
            return null;
        });

        dlg.showAndWait().ifPresent(date -> {
            try {
                attachment.setExpirationDate(date == LocalDate.MIN ? null : date);
                attachmentRepository.save(attachment);
                attachmentList.setAll(
                    new java.util.ArrayList<>(trailerService.getTrailerById(trailer.getId()).map(Trailer::getAttachments).orElse(java.util.Set.of())));
                refreshData();
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось сохранить дату: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private static String getExpirationStyle(java.util.Collection<TrailerAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";
        long minDays = Long.MAX_VALUE;
        for (TrailerAttachment a : attachments) {
            if (a.getExpirationDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), a.getExpirationDate());
                if (days < minDays) minDays = days;
            }
        }
        if (minDays == Long.MAX_VALUE) return "";
        if (minDays <= 7) return "-fx-background-color: #ffcdd2;";
        if (minDays <= 30) return "-fx-background-color: #fff9c4;";
        return "";
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private void runAsync(Runnable task, String successMsg, String errorPrefix) {
        Thread.ofVirtual().start(() -> {
            try {
                task.run();
                javafx.application.Platform.runLater(() ->
                    showAlert("Успех", successMsg, Alert.AlertType.INFORMATION));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    showAlert("Ошибка", errorPrefix + ": " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "pdf";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private static java.io.File ensureExtension(java.io.File file, String ext) {
        String name = file.getName();
        if (name.contains(".") && name.toLowerCase().endsWith("." + ext.toLowerCase())) return file;
        if (!name.contains(".")) return new java.io.File(file.getParent(), name + "." + ext);
        return file;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
