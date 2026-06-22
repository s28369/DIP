package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TruckAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PDF attachments belonging to trucks.
 */
@Repository
public interface TruckAttachmentRepository extends JpaRepository<TruckAttachment, Long> {
    // Returns all attachments of the given truck.
    List<TruckAttachment> findByTruckId(Long truckId);
    // Deletes all attachments of the given truck.
    void deleteByTruckId(Long truckId);

    // Loads only the raw file bytes of a single attachment.
    @Query("SELECT a.fileData FROM TruckAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
