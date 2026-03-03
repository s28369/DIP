package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trailer")
public class Trailer {

    public static final String STATUS_AVAILABLE = "Доступен";
    public static final String STATUS_ON_TRIP = "в рейсе";
    public static final String STATUS_MAINTENANCE = "на ремонте";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(name = "registration_country", length = 100)
    private String registrationCountry;

    @Column(nullable = false, length = 50)
    private String status = STATUS_AVAILABLE;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    @OneToMany(mappedBy = "trailer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TrailerNote> notes = new HashSet<>();

    @OneToMany(mappedBy = "trailer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TrailerAttachment> attachments = new HashSet<>();

    public Trailer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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

    public Set<TrailerNote> getNotes() {
        return notes;
    }

    public void setNotes(Set<TrailerNote> notes) {
        this.notes = notes;
    }

    public int getNoteCount() {
        return notes != null ? notes.size() : 0;
    }

    public Set<TrailerAttachment> getAttachments() { return attachments; }
    public void setAttachments(Set<TrailerAttachment> attachments) { this.attachments = attachments; }

    public void addAttachment(TrailerAttachment attachment) {
        attachments.add(attachment);
        attachment.setTrailer(this);
    }

    public void removeAttachment(TrailerAttachment attachment) {
        attachments.remove(attachment);
        attachment.setTrailer(null);
    }

    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}
