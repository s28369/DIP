package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TripAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PDF attachments belonging to trips.
 */
@Repository
public interface TripAttachmentRepository extends JpaRepository<TripAttachment, Long> {
    // Returns all attachments of the given trip.
    List<TripAttachment> findByTripId(Long tripId);
    // Deletes all attachments of the given trip.
    void deleteByTripId(Long tripId);

    // Loads only the raw file bytes of a single attachment.
    @Query("SELECT a.fileData FROM TripAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
