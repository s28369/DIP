package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.DriverAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PDF attachments belonging to drivers.
 */
@Repository
public interface DriverAttachmentRepository extends JpaRepository<DriverAttachment, Long> {
    // Returns all attachments of the given driver.
    List<DriverAttachment> findByDriverId(Long driverId);
    // Deletes all attachments of the given driver.
    void deleteByDriverId(Long driverId);

    // Loads only the raw file bytes of a single attachment (avoids loading the whole row).
    @Query("SELECT a.fileData FROM DriverAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
