package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Репозиторий для операций с документами водителей
 */
@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {

    List<DriverDocument> findByDriver(Driver driver);

    List<DriverDocument> findByExpiryDateBefore(LocalDate date);

    @Query("SELECT d FROM DriverDocument d WHERE d.expiryDate BETWEEN :startDate AND :endDate")
    List<DriverDocument> findExpiringDocuments(LocalDate startDate, LocalDate endDate);
}
