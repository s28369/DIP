package org.example.fleetmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a timestamped note attached to a trip.
 */
@Entity
@Table(name = "trip_note")
public class TripNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor; records the creation timestamp.
    public TripNote() {
        this.createdAt = LocalDateTime.now();
    }

    // Convenience constructor with the note content and owning trip.
    public TripNote(String content, Trip trip) {
        this.content = content;
        this.trip = trip;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
