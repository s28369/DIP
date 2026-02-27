package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Класс, представляющий документ, привязанный к водителю
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

    /**
     * Перечисление, определяющее тип документа водителя
     */
    public enum DocumentType {
        DRIVING_LICENSE("Водительское удостоверение"),
        MEDICAL_CERTIFICATE("Медицинская справка"),
        TRAINING_CERTIFICATE("Свидетельство об обучении"),
        CPC_CARD("Карта квалификации водителя (CPC)"),
        OTHER("Прочее");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public DriverDocument() {
    }

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

    public boolean hasPdf() {
        return pdfData != null && pdfData.length > 0;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }
}
