package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA entity representing a truck (tractor unit) in the fleet.
 */
@Entity
@Table(name = "truck")
public class Truck {

    public static final String STATUS_AVAILABLE = "Dostępny";
    public static final String STATUS_ON_TRIP = "W trasie";
    public static final String STATUS_MAINTENANCE = "W naprawie";

    public static final String COMPANY_MTG = "MTG";
    public static final String COMPANY_APA = "APA";
    public static final String COMPANY_ABSOLUT = "Absolut";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(name = "registration_country", length = 100)
    private String registrationCountry;

    @Column(length = 100)
    private String company;

    @Column(nullable = false, length = 50)
    private String status = STATUS_AVAILABLE;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    @Column(name = "cargo_description", length = 500)
    private String cargoDescription;

    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TruckAttachment> attachments = new HashSet<>();

    // Default constructor required by JPA.
    public Truck() {
    }

    // Convenience constructor with the core identifying fields.
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

    public Set<TruckAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<TruckAttachment> attachments) {
        this.attachments = attachments;
    }
    
    // Adds an attachment and sets its back-reference to this truck.
    public void addAttachment(TruckAttachment attachment) {
        attachments.add(attachment);
        attachment.setTruck(this);
    }
    
    // Removes an attachment and clears its back-reference.
    public void removeAttachment(TruckAttachment attachment) {
        attachments.remove(attachment);
        attachment.setTruck(null);
    }
    
    // Returns the number of attachments on this truck.
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}
