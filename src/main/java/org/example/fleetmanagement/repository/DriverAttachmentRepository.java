package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.DriverAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverAttachmentRepository extends JpaRepository<DriverAttachment, Long> {
    List<DriverAttachment> findByDriverId(Long driverId);
    void deleteByDriverId(Long driverId);

    @Query("SELECT a.fileData FROM DriverAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
