package org.example.fleetmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasa reprezentująca rejs/trasę w systemie zarządzania flotą
 */
@Entity
@Table(name = "trip")
public class Trip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    
    @Column(name = "origin", nullable = false, length = 200)
    private String origin;
    
    @Column(name = "destination", nullable = false, length = 200)
    private String destination;
    
    @Column(name = "cargo_description", length = 500)
    private String cargoDescription;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;
    
    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status = TripStatus.PLANNED;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TripAttachment> attachments = new ArrayList<>();
    
    /**
     * Enum określający status rejsu
     */
    public enum TripStatus {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
    

    
    public Trip() {
        this.startTime = LocalDateTime.now();
    }
    
    public Trip(Truck truck, Driver driver, String origin, String destination) {
        this.truck = truck;
        this.driver = driver;
        this.origin = origin;
        this.destination = destination;
        this.startTime = LocalDateTime.now();
        this.status = TripStatus.PLANNED;
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
    
    public Driver getDriver() {
        return driver;
    }
    
    public void setDriver(Driver driver) {
        this.driver = driver;
    }
    
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getCargoDescription() {
        return cargoDescription;
    }
    
    public void setCargoDescription(String cargoDescription) {
        this.cargoDescription = cargoDescription;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEstimatedArrival() {
        return estimatedArrival;
    }
    
    public void setEstimatedArrival(LocalDateTime estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }
    
    public LocalDateTime getActualArrival() {
        return actualArrival;
    }
    
    public void setActualArrival(LocalDateTime actualArrival) {
        this.actualArrival = actualArrival;
    }
    
    public TripStatus getStatus() {
        return status;
    }
    
    public void setStatus(TripStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Zwraca opis trasy (skąd -> dokąd)
     */
    public String getRouteDescription() {
        return origin + " → " + destination;
    }
    
    public List<TripAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<TripAttachment> attachments) {
        this.attachments = attachments;
    }
    
    /**
     * Dodaje załącznik do rejsu
     */
    public void addAttachment(TripAttachment attachment) {
        attachments.add(attachment);
        attachment.setTrip(this);
    }
    
    /**
     * Usuwa załącznik z rejsu
     */
    public void removeAttachment(TripAttachment attachment) {
        attachments.remove(attachment);
        attachment.setTrip(null);
    }
    
    /**
     * Zwraca liczbę załączników
     */
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}
