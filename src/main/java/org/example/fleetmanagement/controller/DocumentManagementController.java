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
 * Controller for the truck-document management view (list, add, delete, PDF upload/download).
 */
@Component
public class DocumentManagementController {
    
    private final DocumentService documentService;
    private final TruckService truckService;
    private final ObservableList<Document> documentList = FXCollections.observableArrayList();
    private FilteredList<Document> filteredList;
    private VBox view;
    private TableView<Document> tableView;
    
    // Constructor injection of the document and truck services; builds the view.
    @Autowired
    public DocumentManagementController(DocumentService documentService, TruckService truckService) {
        this.documentService = documentService;
        this.truckService = truckService;
        initializeView();
    }
    
    // Builds the table, search box and action buttons for the documents screen.
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(15));
        

        Label titleLabel = new Label("Zarządzanie dokumentami");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        

        Button addButton = new Button("Dodaj dokument");
        addButton.setOnAction(e -> showAddDocumentDialog());
        
        Button deleteButton = new Button("Usuń dokument");
        deleteButton.setOnAction(e -> handleDeleteDocument());
        
        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(e -> { if (MainController.getInstance() != null) MainController.getInstance().invalidateCache(); refreshData(); });
        
        Button expiringButton = new Button("Wygasające dokumenty");
        expiringButton.setOnAction(e -> showExpiringDocuments());
        
        Button uploadPdfButton = new Button("Dodaj PDF");
        uploadPdfButton.setOnAction(e -> handleUploadPdf());
        
        Button downloadPdfButton = new Button("Pobierz PDF");
        downloadPdfButton.setOnAction(e -> handleDownloadPdf());
        
        HBox buttonBox = new HBox(10, addButton, deleteButton, uploadPdfButton, downloadPdfButton, refreshButton, expiringButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Wpisz tekst do wyszukania...");
        searchField.setPrefWidth(250);

        ComboBox<String> searchParam = new ComboBox<>();
        searchParam.getItems().addAll("Wszystkie", "Ciężarówka", "Typ", "Opis");
        searchParam.setValue("Wszystkie");

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
                    case "Ciężarówka" -> contains(truckReg, lower);
                    case "Typ" -> contains(typeName, lower);
                    case "Opis" -> contains(desc, lower);
                    default -> contains(truckReg, lower) || contains(typeName, lower) || contains(desc, lower);
                };
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        searchParam.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox searchBox = new HBox(10, new Label("Szukaj:"), searchField, searchParam);
        searchBox.setPadding(new Insets(0, 0, 5, 0));

        tableView = new TableView<>();
        tableView.setItems(filteredList);
        
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
        
        view.getChildren().addAll(titleLabel, buttonBox, searchBox, tableView);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    // Returns the root node of this view.
    public Parent getView() {
        return view;
    }
    
    // Reloads all documents into the table (on the FX thread).
    public void refreshData() {
        var data = documentService.getAllDocuments();
        if (javafx.application.Platform.isFxApplicationThread()) {
            documentList.setAll(data);
        } else {
            javafx.application.Platform.runLater(() -> documentList.setAll(data));
        }
    }
    
    // Shows the dialog for adding a new document linked to a chosen truck.
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
                    showAlert("Błąd", "Wybierz ciężarówkę", Alert.AlertType.ERROR);
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
                    showAlert("Błąd", "Nie udało się dodać dokumentu: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    // Deletes the selected document after confirmation.
    private void handleDeleteDocument() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Błąd", "Wybierz dokument do usunięcia", Alert.AlertType.WARNING);
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
                    showAlert("Błąd", "Nie udało się usunąć dokumentu: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    // Shows an info dialog listing documents expiring within 30 days.
    private void showExpiringDocuments() {
        var expiringDocs = documentService.getExpiringDocuments();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Wygasające dokumenty");
        alert.setHeaderText("Dokumenty z kończącym się terminem ważności (30 dni):");
        
        if (expiringDocs.isEmpty()) {
            alert.setContentText("Brak dokumentów z kończącym się terminem ważności");
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
    
    // Lets the user pick a file and attaches it to the selected document.
    private void handleUploadPdf() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Błąd", "Wybierz dokument, aby dodać PDF", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Wszystkie obsługiwane", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Obrazy", "*.jpg", "*.jpeg", "*.png")
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
                showAlert("Błąd", "Nie udało się odczytać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    // Saves the PDF attached to the selected document to a file chosen by the user.
    private void handleDownloadPdf() {
        Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedDocument == null) {
            showAlert("Błąd", "Wybierz dokument", Alert.AlertType.WARNING);
            return;
        }
        
        if (!selectedDocument.hasPdf()) {
            showAlert("Błąd", "Wybrany dokument nie ma dołączonego PDF", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik");
        fileChooser.setInitialFileName(selectedDocument.getPdfFilename());
        String ext = getExtension(selectedDocument.getPdfFilename());
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
                Files.write(file.toPath(), selectedDocument.getPdfData());
                showAlert("Sukces", "Plik PDF został zapisany:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Błąd", "Nie udało się zapisać pliku: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    // Returns the lowercase file extension, defaulting to "pdf" when none is present.
    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "pdf";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    // Ensures the saved file ends with the expected extension.
    private static File ensureExtension(File file, String ext) {
        String name = file.getName();
        if (name.contains(".") && name.toLowerCase().endsWith("." + ext.toLowerCase())) return file;
        if (!name.contains(".")) return new File(file.getParent(), name + "." + ext);
        return file;
    }

    // Case-insensitive substring check used by the search filter.
    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    // Shows a simple modal alert dialog with the given title and message.
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Renders a Truck in combo boxes as "registration - brand" and is not used for parsing.
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
