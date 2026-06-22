package org.example.fleetmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a timestamped note attached to a trailer.
 */
@Entity
@Table(name = "trailer_note")
public class TrailerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trailer_id", nullable = false)
    private Trailer trailer;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor; records the creation timestamp.
    public TrailerNote() {
        this.createdAt = LocalDateTime.now();
    }

    // Convenience constructor with the note content and owning trailer.
    public TrailerNote(String content, Trailer trailer) {
        this.content = content;
        this.trailer = trailer;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trailer getTrailer() {
        return trailer;
    }

    public void setTrailer(Trailer trailer) {
        this.trailer = trailer;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
