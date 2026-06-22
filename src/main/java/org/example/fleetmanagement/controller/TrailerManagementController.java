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

/**
 * Controller for the trailer (semi-trailer) management view, including notes and PDF attachments.
 */
@Component
public class TrailerManagementController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TrailerService trailerService;
    private final TrailerAttachmentRepository attachmentRepository;
    private final ObservableList<Trailer> trailerList = FXCollections.observableArrayList();
    private FilteredList<Trailer> filteredList;
    private VBox view;
    private TableView<Trailer> tableView;

    // Constructor injection of the trailer service and attachment repository; builds the view.
    @Autowired
    public TrailerManagementController(TrailerService trailerService,
                                       TrailerAttachmentRepository attachmentRepository) {
        this.trailerService = trailerService;
        this.attachmentRepository = attachmentRepository;
        initializeView();
    }

    // Creates an editable combo box pre-filled with the given items.
    private ComboBox<String> createEditableComboBox(String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setEditable(true);
        combo.setPrefWidth(300);
        return combo;
    }

    // Builds the table, search box and action buttons for the trailers screen.
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));

        Label titleLabel = new Label("Zarządzanie naczepami");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button addButton = new Button("Dodaj naczepę");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addButton.setOnAction(e -> showAddDialog());

        Button editButton = new Button("Edytuj");
        editButton.setOnAction(e -> showEditDialog());

        Button notesButton = new Button("Uwagi");
        notesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        notesButton.setOnAction(e -> showNotesDialog());

        Button attachmentsButton = new Button("Załączniki PDF");
        attachmentsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        attachmentsButton.setOnAction(e -> showAttachmentsDialog());

        Button deleteButton = new Button("Usuń naczepę");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> handleDelete());

        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });

        HBox buttonBox = new HBox(10, addButton, editButton, notesButton, attachmentsButton, deleteButton, refreshButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Wpisz tekst do wyszukania...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Wszystko", "Numer", "Marka", "Kraj", "Status", "Lokalizacja");
        searchParam.setValue("Wszystko");

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
                case "Numer" -> contains(t.getRegistrationNumber(), lower);
                case "Marka" -> contains(t.getBrand(), lower);
                case "Kraj" -> contains(t.getRegistrationCountry(), lower);
                case "Status" -> contains(t.getStatus(), lower);
                case "Lokalizacja" -> contains(t.getCurrentLocation(), lower);
                default -> contains(t.getRegistrationNumber(), lower)
                        || contains(t.getBrand(), lower)
                        || contains(t.getRegistrationCountry(), lower)
                        || contains(t.getStatus(), lower)
                        || contains(t.getCurrentLocation(), lower);
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Szukaj:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);

        TableColumn<Trailer, String> regCol = new TableColumn<>("Numer");
        regCol.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        regCol.setPrefWidth(150);

        TableColumn<Trailer, String> brandCol = new TableColumn<>("Marka");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));
        brandCol.setPrefWidth(160);

        TableColumn<Trailer, String> countryCol = new TableColumn<>("Kraj rejestracji");
        countryCol.setCellValueFactory(new PropertyValueFactory<>("registrationCountry"));
        countryCol.setPrefWidth(150);

        TableColumn<Trailer, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        TableColumn<Trailer, String> locationCol = new TableColumn<>("Lokalizacja");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("currentLocation"));
        locationCol.setPrefWidth(180);

        TableColumn<Trailer, String> notesCol = new TableColumn<>("Uwagi");
        notesCol.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getNoteCount();
            return new SimpleStringProperty(count > 0 ? count + " szt." : "—");
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

    // Returns the root node of this view.
    public Parent getView() {
        return view;
    }

    // Reloads all trailers into the table (on the FX thread).
    public void refreshData() {
        var data = trailerService.getAllTrailers();
        if (javafx.application.Platform.isFxApplicationThread()) {
            trailerList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> trailerList.setAll(data));
        }
    }

    // ---- Dodawanie ----

    // Shows the dialog for adding a new trailer.
    private void showAddDialog() {
        Dialog<Trailer> dialog = new Dialog<>();
        dialog.setTitle("Dodaj naczepę");
        dialog.setHeaderText("Wprowadź dane nowej naczepy");

        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField regField = new TextField();
        regField.setPromptText("Numer (np. PO12345)");

        TextField brandField = new TextField();
        brandField.setPromptText("Marka (np. Schmitz Cargobull)");

        ComboBox<String> countryCombo = createEditableComboBox(
                "Polska", "Białoruś", "Czechy", "Rosja");

        ComboBox<String> statusCombo = createEditableComboBox(
                Trailer.STATUS_AVAILABLE, Trailer.STATUS_ON_TRIP, Trailer.STATUS_MAINTENANCE);
        statusCombo.setValue(Trailer.STATUS_AVAILABLE);

        TextField locationField = new TextField();
        locationField.setPromptText("np. Warszawa, baza");

        VBox content = new VBox(10,
            new Label("Numer:"), regField,
            new Label("Marka:"), brandField,
            new Label("Kraj rejestracji:"), countryCombo,
            new Label("Status:"), statusCombo,
            new Label("Lokalizacja:"), locationField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        final Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (regField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Numer nie może być pusty", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (brandField.getText().trim().isEmpty()) {
                showAlert("Błąd", "Marka nie może być pusta", Alert.AlertType.ERROR);
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
        }, "Naczepa dodana", "Nie udało się dodać naczepy"));
    }

    // ---- Edycja ----

    // Shows the dialog for editing the selected trailer.
    private void showEditDialog() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz naczepę do edycji", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Trailer> dialog = new Dialog<>();
        dialog.setTitle("Edytuj naczepę");
        dialog.setHeaderText("Naczepa: " + selected.getRegistrationNumber());

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField regField = new TextField(selected.getRegistrationNumber());
        TextField brandField = new TextField(selected.getBrand());

        ComboBox<String> countryCombo = createEditableComboBox(
                "Polska", "Białoruś", "Czechy", "Rosja");
        if (selected.getRegistrationCountry() != null) {
            countryCombo.setValue(selected.getRegistrationCountry());
        }

        ComboBox<String> statusCombo = createEditableComboBox(
                Trailer.STATUS_AVAILABLE, Trailer.STATUS_ON_TRIP, Trailer.STATUS_MAINTENANCE);
        statusCombo.setValue(selected.getStatus());

        TextField locationField = new TextField(
                selected.getCurrentLocation() != null ? selected.getCurrentLocation() : "");

        VBox content = new VBox(10,
            new Label("Numer:"), regField,
            new Label("Marka:"), brandField,
            new Label("Kraj rejestracji:"), countryCombo,
            new Label("Status:"), statusCombo,
            new Label("Lokalizacja:"), locationField
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
        }, "Dane naczepy zaktualizowane", "Nie udało się zaktualizować naczepy"));
    }

    // ---- Usuwanie ----

    // Deletes the selected trailer after confirmation.
    private void handleDelete() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz naczepę do usunięcia", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć tę naczepę?");
        confirm.setContentText("Numer: " + selected.getRegistrationNumber() +
            "\nMarka: " + selected.getBrand());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runAsync(() -> {
                    trailerService.deleteTrailer(selected.getId());
                    refreshData();
                }, "Naczepa usunięta", "Nie udało się usunąć naczepy");
            }
        });
    }

    // ---- Okno dialogowe uwag ----

    // Opens a dialog listing the trailer's notes with add/edit/delete actions.
    private void showNotesDialog() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz naczepę, aby zobaczyć uwagi", Alert.AlertType.WARNING);
            return;
        }

        ObservableList<TrailerNote> noteList = FXCollections.observableArrayList();
        noteList.addAll(trailerService.getNotesByTrailer(selected.getId()));

        TableView<TrailerNote> noteTable = new TableView<>();
        noteTable.setItems(noteList);

        TableColumn<TrailerNote, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TrailerNote, String> contentCol = new TableColumn<>("Treść uwagi");
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        contentCol.setPrefWidth(350);

        TableColumn<TrailerNote, String> dateCol = new TableColumn<>("Data utworzenia");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DT_FMT));
            }
            return new SimpleStringProperty("—");
        });
        dateCol.setPrefWidth(140);

        noteTable.getColumns().addAll(idCol, contentCol, dateCol);

        Button addNoteBtn = new Button("Dodaj uwagę");
        addNoteBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addNoteBtn.setOnAction(e -> addNote(selected, noteList));

        Button editNoteBtn = new Button("Edytuj");
        editNoteBtn.setOnAction(e -> {
            TrailerNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editNote(sel, noteList, selected);
            } else {
                showAlert("Błąd", "Wybierz uwagę do edycji", Alert.AlertType.WARNING);
            }
        });

        Button deleteNoteBtn = new Button("Usuń");
        deleteNoteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteNoteBtn.setOnAction(e -> {
            TrailerNote sel = noteTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteNote(sel, noteList, selected);
            } else {
                showAlert("Błąd", "Wybierz uwagę do usunięcia", Alert.AlertType.WARNING);
            }
        });

        HBox noteBtnBox = new HBox(10, addNoteBtn, editNoteBtn, deleteNoteBtn);
        noteBtnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Uwagi naczepy: " + selected.getRegistrationNumber() + " (" + selected.getBrand() + ")"),
            noteTable,
            noteBtnBox
        );
        content.setPadding(new Insets(15));
        VBox.setVgrow(noteTable, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Uwagi naczepy");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.showAndWait();

        refreshData();
    }

    // Adds a new note to the trailer.
    private void addNote(Trailer trailer, ObservableList<TrailerNote> noteList) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nowa uwaga");
        dlg.setHeaderText("Wprowadź treść uwagi");
        dlg.setContentText("Uwaga:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    TrailerNote note = new TrailerNote(text.trim(), trailer);
                    trailerService.addNote(note);
                    noteList.setAll(trailerService.getNotesByTrailer(trailer.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się dodać uwagi: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // Edits an existing trailer note.
    private void editNote(TrailerNote note, ObservableList<TrailerNote> noteList, Trailer trailer) {
        TextInputDialog dlg = new TextInputDialog(note.getContent());
        dlg.setTitle("Edytuj uwagę");
        dlg.setHeaderText("Zmień treść uwagi");
        dlg.setContentText("Uwaga:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                try {
                    note.setContent(text.trim());
                    trailerService.updateNote(note);
                    noteList.setAll(trailerService.getNotesByTrailer(trailer.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się zaktualizować uwagi: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // Deletes a trailer note.
    private void deleteNote(TrailerNote note, ObservableList<TrailerNote> noteList, Trailer trailer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Usunąć tę uwagę?");
        confirm.setContentText(note.getContent());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    trailerService.deleteNote(note.getId());
                    noteList.setAll(trailerService.getNotesByTrailer(trailer.getId()));
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się usunąć uwagi: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ---- Okno dialogowe załączników ----

    // Opens a dialog listing the trailer's PDF attachments with add/edit/download/delete actions.
    private void showAttachmentsDialog() {
        Trailer selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Błąd", "Wybierz naczepę", Alert.AlertType.WARNING);
            return;
        }

        Trailer trailer = trailerService.getTrailerById(selected.getId()).orElse(selected);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Załączniki PDF");
        dialog.setHeaderText("Naczepa: " + trailer.getBrand() + " (" + trailer.getRegistrationNumber() + ")");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<TrailerAttachment> attachmentTable = new TableView<>();
        ObservableList<TrailerAttachment> attachmentList = FXCollections.observableArrayList(trailer.getAttachments());
        attachmentTable.setItems(attachmentList);
        attachmentTable.setPrefHeight(250);

        TableColumn<TrailerAttachment, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TrailerAttachment, String> nameCol = new TableColumn<>("Nazwa pliku");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        nameCol.setPrefWidth(200);

        TableColumn<TrailerAttachment, String> descCol = new TableColumn<>("Opis");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(150);

        TableColumn<TrailerAttachment, String> sizeCol = new TableColumn<>("Rozmiar");
        sizeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);

        TableColumn<TrailerAttachment, String> dateCol = new TableColumn<>("Data dodania");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getUploadedAt().format(DT_FMT));
            }
            return new SimpleStringProperty("-");
        });
        dateCol.setPrefWidth(120);

        TableColumn<TrailerAttachment, String> expCol = new TableColumn<>("Data ważności");
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

        Button addBtn = new Button("Dodaj PDF");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            addAttachmentToTrailer(trailer, attachmentList);
            refreshData();
        });

        Button downloadBtn = new Button("Pobierz PDF");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                downloadAttachment(sel);
            } else {
                showAlert("Błąd", "Wybierz załącznik do pobrania", Alert.AlertType.WARNING);
            }
        });

        Button editDescBtn = new Button("Edytuj opis");
        editDescBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        editDescBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editAttachmentDescription(sel, attachmentList, trailer);
            } else {
                showAlert("Błąd", "Wybierz załącznik", Alert.AlertType.WARNING);
            }
        });

        Button expDateBtn = new Button("Data ważności");
        expDateBtn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        expDateBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editExpirationDate(sel, attachmentList, trailer);
            } else {
                showAlert("Błąd", "Wybierz załącznik", Alert.AlertType.WARNING);
            }
        });

        Button deleteBtn = new Button("Usuń załącznik");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> {
            TrailerAttachment sel = attachmentTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                deleteAttachment(sel, attachmentList);
                refreshData();
            } else {
                showAlert("Błąd", "Wybierz załącznik do usunięcia", Alert.AlertType.WARNING);
            }
        });

        HBox buttonBox = new HBox(10, addBtn, downloadBtn, editDescBtn, expDateBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(10,
            new Label("Lista załączników PDF:"),
            attachmentTable,
            buttonBox
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.showAndWait();
    }

    // Lets the user pick a file and attaches it (with description/expiry) to the trailer.
    private void addAttachmentToTrailer(Trailer trailer, ObservableList<TrailerAttachment> attachmentList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz pliki");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Wszystkie obsługiwane", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Obrazy", "*.jpg", "*.jpeg", "*.png"));

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
                    showAlert("Błąd", "Nie udało się odczytać pliku: " + file.getName() + "\n" + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            if (added > 0) {
                showAlert("Sukces", "Dodano plików: " + added, Alert.AlertType.INFORMATION);
            }
        }
    }

    // Edits the description of a trailer attachment.
    private void editAttachmentDescription(TrailerAttachment attachment, ObservableList<TrailerAttachment> attachmentList, Trailer trailer) {
        TextInputDialog dlg = new TextInputDialog(attachment.getDescription() != null ? attachment.getDescription() : "");
        dlg.setTitle("Opis dokumentu");
        dlg.setHeaderText("Plik: " + attachment.getFilename());
        dlg.setContentText("Opis:");
        dlg.getEditor().setPrefWidth(350);

        dlg.showAndWait().ifPresent(text -> {
            try {
                attachment.setDescription(text.trim());
                attachmentRepository.save(attachment);
                attachmentList.setAll(
                    new java.util.ArrayList<>(trailerService.getTrailerById(trailer.getId()).map(Trailer::getAttachments).orElse(java.util.Set.of())));
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać opisu: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Saves the selected attachment's file to disk.
    private void downloadAttachment(TrailerAttachment attachment) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik");
        fileChooser.setInitialFileName(attachment.getFilename());
        String ext = getExtension(attachment.getFilename());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(ext.toUpperCase() + " pliki", "*." + ext),
            new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*")
        );
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));

        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            file = ensureExtension(file, ext);
            try {
                byte[] data = attachmentRepository.findFileDataById(attachment.getId());
                Files.write(file.toPath(), data);
                showAlert("Sukces", "Plik został zapisany jako:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // Deletes the selected attachment from the trailer.
    private void deleteAttachment(TrailerAttachment attachment, ObservableList<TrailerAttachment> attachmentList) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć ten załącznik?");
        confirm.setContentText("Plik: " + attachment.getFilename());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    attachmentRepository.delete(attachment);
                    attachmentList.remove(attachment);
                    showAlert("Sukces", "Załącznik usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie udało się usunąć załącznika: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // Edits the expiration date of a trailer attachment.
    private void editExpirationDate(TrailerAttachment attachment, ObservableList<TrailerAttachment> attachmentList, Trailer trailer) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Data ważności dokumentu");
        dlg.setHeaderText("Plik: " + attachment.getFilename());

        ButtonType saveType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("Usuń datę", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, clearType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(attachment.getExpirationDate());
        VBox content = new VBox(10, new Label("Data ważności:"), datePicker);
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
                showAlert("Błąd", "Nie udało się zapisać daty: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Returns a row style highlighting trailers whose documents are expired or expiring soon.
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

    // Case-insensitive substring check used by the search filter.
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    // Runs a blocking task on a background thread and shows a success/error alert on the FX thread.
    private void runAsync(Runnable task, String successMsg, String errorPrefix) {
        Thread.ofVirtual().start(() -> {
            try {
                task.run();
                javafx.application.Platform.runLater(() ->
                    showAlert("Sukces", successMsg, Alert.AlertType.INFORMATION));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    showAlert("Błąd", errorPrefix + ": " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    // Returns the lowercase file extension, defaulting to "pdf" when none is present.
    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "pdf";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    // Ensures the saved file ends with the expected extension.
    private static java.io.File ensureExtension(java.io.File file, String ext) {
        String name = file.getName();
        if (name.contains(".") && name.toLowerCase().endsWith("." + ext.toLowerCase())) return file;
        if (!name.contains(".")) return new java.io.File(file.getParent(), name + "." + ext);
        return file;
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
