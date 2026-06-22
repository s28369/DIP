package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TripNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for trip notes.
 */
@Repository
public interface TripNoteRepository extends JpaRepository<TripNote, Long> {
    // Returns the notes of a trip, newest first.
    List<TripNote> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
