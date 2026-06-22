package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Document;
import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Document entity operations.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    // Returns all documents assigned to the given truck.
    List<Document> findByTruck(Truck truck);
    
    // Returns documents whose expiry date is before the given date.
    List<Document> findByExpiryDateBefore(LocalDate date);
    
    // Returns documents expiring within the given date range (inclusive).
    @Query("SELECT d FROM Document d WHERE d.expiryDate BETWEEN :startDate AND :endDate")
    List<Document> findExpiringDocuments(LocalDate startDate, LocalDate endDate);
}
