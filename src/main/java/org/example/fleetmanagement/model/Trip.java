package org.example.fleetmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a trip (transport route) with its truck, trailer, driver and customer.
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
    @JoinColumn(name = "trailer_id")
    private Trailer trailer;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
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
    
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TripAttachment> attachments = new HashSet<>();
    
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TripNote> tripNotes = new HashSet<>();
    
    public enum TripStatus {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
    
    // Default constructor; initializes the start time to now.
    public Trip() {
        this.startTime = LocalDateTime.now();
    }
    
    // Convenience constructor for a planned trip with the core fields.
    public Trip(Truck truck, Driver driver, String origin, String destination) {
        this.truck = truck;
        this.driver = driver;
        this.origin = origin;
        this.destination = destination;
        this.startTime = LocalDateTime.now();
        this.status = TripStatus.PLANNED;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Truck getTruck() { return truck; }
    public void setTruck(Truck truck) { this.truck = truck; }
    
    public Trailer getTrailer() { return trailer; }
    public void setTrailer(Trailer trailer) { this.trailer = trailer; }
    
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    
    public String getCargoDescription() { return cargoDescription; }
    public void setCargoDescription(String cargoDescription) { this.cargoDescription = cargoDescription; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(LocalDateTime estimatedArrival) { this.estimatedArrival = estimatedArrival; }
    
    public LocalDateTime getActualArrival() { return actualArrival; }
    public void setActualArrival(LocalDateTime actualArrival) { this.actualArrival = actualArrival; }
    
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // Returns a human-readable "origin → destination" route label.
    public String getRouteDescription() {
        return origin + " → " + destination;
    }
    
    public Set<TripAttachment> getAttachments() { return attachments; }
    public void setAttachments(Set<TripAttachment> attachments) { this.attachments = attachments; }
    
    // Adds an attachment and sets its back-reference to this trip.
    public void addAttachment(TripAttachment attachment) {
        attachments.add(attachment);
        attachment.setTrip(this);
    }
    
    // Removes an attachment and clears its back-reference.
    public void removeAttachment(TripAttachment attachment) {
        attachments.remove(attachment);
        attachment.setTrip(null);
    }
    
    // Returns the number of attachments on this trip.
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
    
    public Set<TripNote> getTripNotes() { return tripNotes; }
    public void setTripNotes(Set<TripNote> tripNotes) { this.tripNotes = tripNotes; }
    
    // Returns the number of notes on this trip.
    public int getNoteCount() {
        return tripNotes != null ? tripNotes.size() : 0;
    }
}
