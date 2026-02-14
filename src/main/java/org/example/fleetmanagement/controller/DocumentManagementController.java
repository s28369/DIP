package org.example.fleetmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
 * Kontroler zarządzający widokiem dokumentów
 */
@Component
public class DocumentManagementController {
    
    private final DocumentService documentService;
    private final TruckService truckService;
    private final ObservableList<Document> documentList = FXCollections.observableArrayList();
    private VBox view;
    private TableView<Document> tableView;
    
    @Autowired
    public DocumentManagementController(DocumentService documentService, TruckService truckService) {
        this.documentService = documentService;
        this.truckService = truckService;
        initializeView();
    }
    
    /**
     * Inicjalizuje widok zarządzania dokumentami
     */
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        

        Label titleLabel = new Label("Zarządzanie Dokumentami");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        

        Button addButton = new Button("Dodaj dokument");
        addButton.setOnAction(e -> showAddDocumentDialog());
        
        Button deleteButton = new Button("Usuń dokument");
        deleteButton.setOnAction(e -> handleDeleteDocument());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> refreshData());
        
        Button expiringButton = new Button("Wygasające dokumenty");
        expiringButton.setOnAction(e -> showExpiringDocuments());
        
        Button uploadPdfButton = new Button("Dodaj PDF");
        uploadPdfButton.setOnAction(e -> handleUploadPdf());
        
        Button downloadPdfButton = new Button("Pobierz PDF");
        downloadPdfButton.setOnAction(e -> handleDownloadPdf());
        
        HBox buttonBox = new HBox(10, addButton, deleteButton, uploadPdfButton, downloadPdfButton, refreshButton, expiringButton);

        tableView = new TableView<>();
        tableView.setItems(documentList);
        
        TableColumn<Document, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        
        TableColumn<Document, String> truckColumn = new TableColumn<>("Ciężarówka");
        truckColumn.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            Truck truck = doc != null ? doc.getTruck() : null;
            String regNumber = truck != null ? truck.getRegistrationNumber() : "Brak";
            return new javafx.beans.property.SimpleStringProperty(regNumber);
        });
        truckColumn.setPrefWidth(150);
        
        TableColumn<Document, Document.DocumentType> typeColumn = new TableColumn<>("Typ dokumentu");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        typeColumn.setPrefWidth(150);
        
        TableColumn<Document, LocalDate> expiryColumn = new TableColumn<>("Data ważności");
        expiryColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        expiryColumn.setPrefWidth(120);
        
        TableColumn<Document, String> descriptionColumn = new TableColumn<>("Opis");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setPrefWidth(200);
        
        TableColumn<Document, String> pdfColumn = new TableColumn<>("PDF");
        pdfColumn.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            String pdfStatus = doc.hasPdf() ? "Tak (" + doc.getPdfFilename() + ")" : "Brak";
            return new javafx.beans.property.SimpleStringProperty(pdfStatus);
        });
        pdfColumn.setPrefWidth(150);
        
        tableView.getColumns().addAll(idColumn, truckColumn, typeColumn, expiryColumn, descriptionColumn, pdfColumn);
        
        view.getChildren().addAll(titleLabel, buttonBox, tableView);
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
        documentList.clear();
        documentList.addAll(documentService.getAllDocuments());
    }
    
    /**
     * Wyświetla dialog dodawania nowego dokumentu
     */
    private void showAddDocumentDialog() {
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle("Dodaj dokument");
        dialog.setHeaderText("Wprowadź dane nowego dokumentu");
        
        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
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
        descriptionField.setPromptText("Opis dokumentu");
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Ciężarówka:"), truckComboBox,
            new Label("Typ dokumentu:"), typeComboBox,
            new Label("Data ważności:"), datePicker,
            new Label("Opis:"), descriptionField
        );
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (truckComboBox.getValue() == null) {
                    showAlert("Błąd", "Proszę wybrać ciężarówkę", Alert.AlertType.ERROR);
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
                    showAlert("Sukces", "Dokument został dodany", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można dodać dokumentu: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Obsługuje usuwanie dokumentu
     */
    private void handleDeleteDocument() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Błąd", "Proszę wybrać dokument do usunięcia", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz usunąć ten dokument?");
        confirmAlert.setContentText("Typ: " + selectedDocument.getDocumentType().getDisplayName());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    documentService.deleteDocument(selectedDocument.getId());
                    refreshData();
                    showAlert("Sukces", "Dokument został usunięty", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Błąd", "Nie można usunąć dokumentu: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Wyświetla listę wygasających dokumentów
     */
    private void showExpiringDocuments() {
        var expiringDocs = documentService.getExpiringDocuments();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Wygasające dokumenty");
        alert.setHeaderText("Dokumenty wygasające w ciągu 30 dni:");
        
        if (expiringDocs.isEmpty()) {
            alert.setContentText("Brak wygasających dokumentów");
        } else {
            StringBuilder content = new StringBuilder();
            for (Document doc : expiringDocs) {
                content.append(String.format("• %s - %s (ważny do: %s)\n",
                    doc.getTruck().getRegistrationNumber(),
                    doc.getDocumentType().getDisplayName(),
                    doc.getExpiryDate()));
            }
            alert.setContentText(content.toString());
        }
        
        alert.showAndWait();
    }
    
    /**
     * Obsługuje przesyłanie pliku PDF do wybranego dokumentu
     */
    private void handleUploadPdf() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Błąd", "Proszę wybrać dokument, do którego chcesz dodać PDF", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf")
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
                showAlert("Sukces", "Plik PDF został dodany do dokumentu", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie można odczytać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Obsługuje pobieranie pliku PDF z wybranego dokumentu
     */
    private void handleDownloadPdf() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Błąd", "Proszę wybrać dokument", Alert.AlertType.WARNING);
            return;
        }
        
        if (!selectedDocument.hasPdf()) {
            showAlert("Błąd", "Wybrany dokument nie ma załączonego pliku PDF", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik PDF");
        fileChooser.setInitialFileName(selectedDocument.getPdfFilename());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf")
        );
        
        Stage stage = (Stage) view.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Files.write(file.toPath(), selectedDocument.getPdfData());
                showAlert("Sukces", "Plik PDF został zapisany:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie można zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
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
    
    /**
     * Konwerter dla wyświetlania ciężarówek w ComboBox
     */
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
