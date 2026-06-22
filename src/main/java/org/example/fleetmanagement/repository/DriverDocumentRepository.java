package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for driver document operations.
 */
@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {

    // Returns all documents assigned to the given driver.
    List<DriverDocument> findByDriver(Driver driver);

    // Returns driver documents whose expiry date is before the given date.
    List<DriverDocument> findByExpiryDateBefore(LocalDate date);

    // Returns driver documents expiring within the given date range (inclusive).
    @Query("SELECT d FROM DriverDocument d WHERE d.expiryDate BETWEEN :startDate AND :endDate")
    List<DriverDocument> findExpiringDocuments(LocalDate startDate, LocalDate endDate);
}
