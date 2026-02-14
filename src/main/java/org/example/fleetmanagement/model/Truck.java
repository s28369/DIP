package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa reprezentująca ciężarówkę w systemie zarządzania flotą
 */
@Entity
@Table(name = "truck")
public class Truck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String brand;
    
    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TruckStatus status = TruckStatus.ACTIVE;
    
    @Column(name = "current_location", length = 200)
    private String currentLocation;
    
    @Column(name = "cargo_description", length = 500)
    private String cargoDescription;
    
    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();
    
    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TruckAttachment> attachments = new ArrayList<>();
    
    /**
     * Enum określający status ciężarówki
     */
    public enum TruckStatus {
        ACTIVE,
        MAINTENANCE,
        INACTIVE
    }

    
    public Truck() {
    }
    
    public Truck(String brand, String registrationNumber) {
        this.brand = brand;
        this.registrationNumber = registrationNumber;
        this.status = TruckStatus.ACTIVE;
    }
    
    public Truck(Long id, String brand, String registrationNumber, TruckStatus status, List<Document> documents) {
        this.id = id;
        this.brand = brand;
        this.registrationNumber = registrationNumber;
        this.status = status;
        this.documents = documents;
    }
    

    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
    
    public TruckStatus getStatus() {
        return status;
    }
    
    public void setStatus(TruckStatus status) {
        this.status = status;
    }
    
    public String getCurrentLocation() {
        return currentLocation;
    }
    
    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
    
    public String getCargoDescription() {
        return cargoDescription;
    }
    
    public void setCargoDescription(String cargoDescription) {
        this.cargoDescription = cargoDescription;
    }
    
    public List<Document> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }
    
    public List<TruckAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<TruckAttachment> attachments) {
        this.attachments = attachments;
    }
    
    /**
     * Dodaje załącznik do ciężarówki
     */
    public void addAttachment(TruckAttachment attachment) {
        attachments.add(attachment);
        attachment.setTruck(this);
    }
    
    /**
     * Usuwa załącznik z ciężarówki
     */
    public void removeAttachment(TruckAttachment attachment) {
        attachments.remove(attachment);
        attachment.setTruck(null);
    }
    
    /**
     * Zwraca liczbę załączników
     */
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}
