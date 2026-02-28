package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TrailerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrailerAttachmentRepository extends JpaRepository<TrailerAttachment, Long> {
    List<TrailerAttachment> findByTrailerId(Long trailerId);
    void deleteByTrailerId(Long trailerId);

    @Query("SELECT a.fileData FROM TrailerAttachment a WHERE a.id = :id")
    byte[] findFileDataById(@Param("id") Long id);
}
