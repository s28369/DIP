package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "trailer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TrailerNote> notes = new ArrayList<>();

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

    public List<TrailerNote> getNotes() {
        return notes;
    }

    public void setNotes(List<TrailerNote> notes) {
        this.notes = notes;
    }

    public int getNoteCount() {
        return notes != null ? notes.size() : 0;
    }
}
