package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * JPA entity representing a document assigned to a driver (with optional attached PDF).
 */
@Entity
@Table(name = "driver_document")
public class DriverDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(length = 500)
    private String description;

    @Lob
    @Column(name = "pdf_data", columnDefinition = "LONGBLOB")
    private byte[] pdfData;

    @Column(name = "pdf_filename", length = 255)
    private String pdfFilename;

    // Driver document type with its Polish display label.
    public enum DocumentType {
        DRIVING_LICENSE("Prawo jazdy"),
        MEDICAL_CERTIFICATE("Zaświadczenie lekarskie"),
        TRAINING_CERTIFICATE("Świadectwo ukończenia szkolenia"),
        CPC_CARD("Karta kwalifikacji kierowcy (CPC)"),
        OTHER("Inne");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        // Returns the human-readable label of this document type.
        public String getDisplayName() {
            return displayName;
        }
    }

    // Default constructor required by JPA.
    public DriverDocument() {
    }

    // Convenience constructor with the core fields.
    public DriverDocument(Driver driver, DocumentType documentType, LocalDate expiryDate, String description) {
        this.driver = driver;
        this.documentType = documentType;
        this.expiryDate = expiryDate;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public void setPdfData(byte[] pdfData) {
        this.pdfData = pdfData;
    }

    public String getPdfFilename() {
        return pdfFilename;
    }

    public void setPdfFilename(String pdfFilename) {
        this.pdfFilename = pdfFilename;
    }

    // Returns true if a PDF file is attached to this document.
    public boolean hasPdf() {
        return pdfData != null && pdfData.length > 0;
    }

    // Returns true if the document's expiry date has passed.
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }
}
