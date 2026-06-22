package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TrailerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PDF attachments belonging to trailers.
 */
@Repository
public interface TrailerAttachmentRepository extends JpaRepository<TrailerAttachment, Long> {
    // Returns all attachments of the given trailer.
    List<TrailerAttachment> findByTrailerId(Long trailerId);
    // Deletes all attachments of the given trailer.
    void deleteByTrailerId(Long trailerId);

    // Loads only the raw file bytes of a single attachment.
    @Query("SELECT a.fileData FROM TrailerAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
