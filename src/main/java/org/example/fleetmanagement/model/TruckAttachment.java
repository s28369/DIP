package org.example.fleetmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Klasa reprezentująca załącznik PDF przypisany do ciężarówki
 */
@Entity
@Table(name = "truck_attachment")
public class TruckAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;
    

    
    public TruckAttachment() {
        this.uploadedAt = LocalDateTime.now();
    }
    
    public TruckAttachment(String filename, String description, byte[] fileData, Truck truck) {
        this.filename = filename;
        this.description = description;
        this.fileData = fileData;
        this.fileSize = (long) fileData.length;
        this.uploadedAt = LocalDateTime.now();
        this.truck = truck;
    }
    

    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
        this.fileSize = fileData != null ? (long) fileData.length : 0L;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public Truck getTruck() {
        return truck;
    }
    
    public void setTruck(Truck truck) {
        this.truck = truck;
    }
    
    /**
     * Zwraca rozmiar pliku w czytelnym formacie
     */
    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
