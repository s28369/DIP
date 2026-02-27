package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс, представляющий грузовик в системе управления автопарком
 */
@Entity
@Table(name = "truck")
public class Truck {

    public static final String STATUS_AVAILABLE = "Доступна";
    public static final String STATUS_ON_TRIP = "в рейсе";
    public static final String STATUS_MAINTENANCE = "на ремонте";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(name = "registration_country", length = 100)
    private String registrationCountry;

    @Column(nullable = false, length = 50)
    private String status = STATUS_AVAILABLE;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    @Column(name = "cargo_description", length = 500)
    private String cargoDescription;

    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TruckAttachment> attachments = new ArrayList<>();

    public Truck() {
    }

    public Truck(String brand, String registrationNumber) {
        this.brand = brand;
        this.registrationNumber = registrationNumber;
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

    public String getRegistrationCountry() {
        return registrationCountry;
    }

    public void setRegistrationCountry(String registrationCountry) {
        this.registrationCountry = registrationCountry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
     * Добавляет вложение к грузовику
     */
    public void addAttachment(TruckAttachment attachment) {
        attachments.add(attachment);
        attachment.setTruck(this);
    }
    
    /**
     * Удаляет вложение у грузовика
     */
    public void removeAttachment(TruckAttachment attachment) {
        attachments.remove(attachment);
        attachment.setTruck(null);
    }
    
    /**
     * Возвращает количество вложений
     */
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}
