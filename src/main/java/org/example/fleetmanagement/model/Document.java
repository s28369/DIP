package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Klasa reprezentująca dokument przypisany do ciężarówki
 */
@Entity
@Table(name = "document")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
    
    @Column(length = 500)
    private String description;
    
    @Lob
    @Column(name = "pdf_data")
    private byte[] pdfData;
    
    @Column(name = "pdf_filename", length = 255)
    private String pdfFilename;
    
    /**
     * Enum określający typ dokumentu
     */
    public enum DocumentType {
        INSURANCE("Ubezpieczenie"),
        TECHNICAL_INSPECTION("Przegląd techniczny"),
        REGISTRATION("Rejestracja"),
        PERMISSION("Zezwolenie"),
        OTHER("Inne");
        
        private final String displayName;
        
        DocumentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    
    public Document() {
    }
    
    public Document(Truck truck, DocumentType documentType, LocalDate expiryDate, String description) {
        this.truck = truck;
        this.documentType = documentType;
        this.expiryDate = expiryDate;
        this.description = description;
    }
    
    public Document(Long id, Truck truck, DocumentType documentType, LocalDate expiryDate, String description) {
        this.id = id;
        this.truck = truck;
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
    
    public Truck getTruck() {
        return truck;
    }
    
    public void setTruck(Truck truck) {
        this.truck = truck;
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
    
    /**
     * Sprawdza czy dokument ma załączony plik PDF
     */
    public boolean hasPdf() {
        return pdfData != null && pdfData.length > 0;
    }
    
    /**
     * Sprawdza czy dokument wygasł
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }
}
