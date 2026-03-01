package org.example.fleetmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.fleetmanagement.model.Document;
import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.service.DocumentService;
import org.example.fleetmanagement.service.TruckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.util.StringConverter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Контроллер управления представлением документов
 */
@Component
public class DocumentManagementController {
    
    private final DocumentService documentService;
    private final TruckService truckService;
    private final ObservableList<Document> documentList = FXCollections.observableArrayList();
    private FilteredList<Document> filteredList;
    private VBox view;
    private TableView<Document> tableView;
    
    @Autowired
    public DocumentManagementController(DocumentService documentService, TruckService truckService) {
        this.documentService = documentService;
        this.truckService = truckService;
        initializeView();
    }
    
    /**
     * Инициализирует представление управления документами
     */
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        

        Label titleLabel = new Label("Управление документами");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        

        Button addButton = new Button("Добавить документ");
        addButton.setOnAction(e -> showAddDocumentDialog());
        
        Button deleteButton = new Button("Удалить документ");
        deleteButton.setOnAction(e -> handleDeleteDocument());
        
        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(e -> refreshData());
        
        Button expiringButton = new Button("Истекающие документы");
        expiringButton.setOnAction(e -> showExpiringDocuments());
        
        Button uploadPdfButton = new Button("Добавить PDF");
        uploadPdfButton.setOnAction(e -> handleUploadPdf());
        
        Button downloadPdfButton = new Button("Скачать PDF");
        downloadPdfButton.setOnAction(e -> handleDownloadPdf());
        
        HBox buttonBox = new HBox(10, addButton, deleteButton, uploadPdfButton, downloadPdfButton, refreshButton, expiringButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Все", "Грузовик", "Тип", "Описание");
        searchParam.setValue("Все");

        filteredList = new FilteredList<>(documentList, p -> true);

        Runnable applyFilter = () -> {
            String text = searchField.getText();
            String param = searchParam.getValue();
            if (text == null || text.trim().isEmpty()) {
                filteredList.setPredicate(p -> true);
                return;
            }
            String lower = text.trim().toLowerCase();
            filteredList.setPredicate(doc -> {
                String truckReg = doc.getTruck() != null ? doc.getTruck().getRegistrationNumber() : "";
                String typeName = doc.getDocumentType() != null ? doc.getDocumentType().getDisplayName() : "";
                String desc = doc.getDescription() != null ? doc.getDescription() : "";
                return switch (param) {
                    case "Грузовик" -> contains(truckReg, lower);
                    case "Тип" -> contains(typeName, lower);
                    case "Описание" -> contains(desc, lower);
                    default -> contains(truckReg, lower) || contains(typeName, lower) || contains(desc, lower);
                };
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);
        
        TableColumn<Document, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        
        TableColumn<Document, String> truckColumn = new TableColumn<>("Грузовик");
        truckColumn.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            Truck truck = doc != null ? doc.getTruck() : null;
            String regNumber = truck != null ? truck.getRegistrationNumber() : "Нет";
            return new javafx.beans.property.SimpleStringProperty(regNumber);
        });
        truckColumn.setPrefWidth(150);
        
        TableColumn<Document, Document.DocumentType> typeColumn = new TableColumn<>("Тип документа");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        typeColumn.setPrefWidth(150);
        
        TableColumn<Document, LocalDate> expiryColumn = new TableColumn<>("Срок действия");
        expiryColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        expiryColumn.setPrefWidth(120);
        
        TableColumn<Document, String> descriptionColumn = new TableColumn<>("Описание");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setPrefWidth(200);
        
        TableColumn<Document, String> pdfColumn = new TableColumn<>("PDF");
        pdfColumn.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            String pdfStatus = doc.hasPdf() ? "Да (" + doc.getPdfFilename() + ")" : "Нет";
            return new javafx.beans.property.SimpleStringProperty(pdfStatus);
        });
        pdfColumn.setPrefWidth(150);
        
        tableView.getColumns().addAll(idColumn, truckColumn, typeColumn, expiryColumn, descriptionColumn, pdfColumn);
        
        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
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
        var data = documentService.getAllDocuments();
        if (javafx.application.Platform.isFxApplicationThread()) {
            documentList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> documentList.setAll(data));
        }
    }
    
    /**
     * Отображает диалог добавления нового документа
     */
    private void showAddDocumentDialog() {
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle("Добавить документ");
        dialog.setHeaderText("Введите данные нового документа");
        
        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        

        ComboBox<Truck> truckComboBox = new ComboBox<>();
        truckComboBox.getItems().addAll(truckService.getAllTrucks());
        truckComboBox.setConverter(new TruckStringConverter());
        
        ComboBox<Document.DocumentType> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll(Document.DocumentType.values());
        typeComboBox.setValue(Document.DocumentType.INSURANCE);
        
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusMonths(12));
        
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Описание документа");
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Грузовик:"), truckComboBox,
            new Label("Тип документа:"), typeComboBox,
            new Label("Срок действия:"), datePicker,
            new Label("Описание:"), descriptionField
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (truckComboBox.getValue() == null) {
                    showAlert("Ошибка", "Выберите грузовик", Alert.AlertType.ERROR);
                    return null;
                }
                
                Document document = new Document();
                document.setTruck(truckComboBox.getValue());
                document.setDocumentType(typeComboBox.getValue());
                document.setExpiryDate(datePicker.getValue());
                document.setDescription(descriptionField.getText());
                return document;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(document -> {
            if (document != null) {
                try {
                    documentService.addDocument(document);
                    refreshData();
                    showAlert("Успех", "Документ добавлен", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось добавить документ: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Обрабатывает удаление документа
     */
    private void handleDeleteDocument() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Ошибка", "Выберите документ для удаления", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Вы уверены, что хотите удалить этот документ?");
        confirmAlert.setContentText("Тип: " + selectedDocument.getDocumentType().getDisplayName());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    documentService.deleteDocument(selectedDocument.getId());
                    refreshData();
                    showAlert("Успех", "Документ удалён", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить документ: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Отображает список истекающих документов
     */
    private void showExpiringDocuments() {
        var expiringDocs = documentService.getExpiringDocuments();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Истекающие документы");
        alert.setHeaderText("Документы с истекающим сроком (30 дней):");
        
        if (expiringDocs.isEmpty()) {
            alert.setContentText("Нет документов с истекающим сроком");
        } else {
            StringBuilder content = new StringBuilder();
            for (Document doc : expiringDocs) {
                content.append(String.format("• %s - %s (действителен до: %s)\n",
                    doc.getTruck().getRegistrationNumber(),
                    doc.getDocumentType().getDisplayName(),
                    doc.getExpiryDate()));
            }
            alert.setContentText(content.toString());
        }
        
        alert.showAndWait();
    }
    
    /**
     * Обрабатывает загрузку файла PDF к выбранному документу
     */
    private void handleUploadPdf() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Ошибка", "Выберите документ для добавления PDF", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            try {
                byte[] pdfData = Files.readAllBytes(file.toPath());
                selectedDocument.setPdfData(pdfData);
                selectedDocument.setPdfFilename(file.getName());
                documentService.updateDocument(selectedDocument);
                refreshData();
                showAlert("Успех", "Файл PDF добавлен к документу", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось прочитать файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Обрабатывает скачивание файла PDF из выбранного документа
     */
    private void handleDownloadPdf() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Ошибка", "Выберите документ", Alert.AlertType.WARNING);
            return;
        }
        
        if (!selectedDocument.hasPdf()) {
            showAlert("Ошибка", "У выбранного документа нет прикреплённого PDF", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл PDF");
        fileChooser.setInitialFileName(selectedDocument.getPdfFilename());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Файлы PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Files.write(file.toPath(), selectedDocument.getPdfData());
                showAlert("Успех", "Файл PDF сохранён:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось сохранить файл: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Отображает диалоговое окно с сообщением
     */
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private static class TruckStringConverter extends StringConverter<Truck> {
        @Override
        public String toString(Truck truck) {
            return truck != null ? truck.getRegistrationNumber() + " - " + truck.getBrand() : "";
        }
        
        @Override
        public Truck fromString(String string) {
            return null;
        }
    }
}
