package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TripAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripAttachmentRepository extends JpaRepository<TripAttachment, Long> {
    List<TripAttachment> findByTripId(Long tripId);
    void deleteByTripId(Long tripId);

    @Query("SELECT a.fileData FROM TripAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
