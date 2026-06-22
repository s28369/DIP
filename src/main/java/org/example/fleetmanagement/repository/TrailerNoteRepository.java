package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TrailerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for trailer notes.
 */
@Repository
public interface TrailerNoteRepository extends JpaRepository<TrailerNote, Long> {

    // Returns the notes of a trailer, newest first.
    List<TrailerNote> findByTrailerIdOrderByCreatedAtDesc(Long trailerId);
}
