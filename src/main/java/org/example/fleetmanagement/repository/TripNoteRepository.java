package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TripNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripNoteRepository extends JpaRepository<TripNote, Long> {
    List<TripNote> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
