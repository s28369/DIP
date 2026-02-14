package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Document;
import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repozytorium do obsługi operacji na encji Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * Wyszukuje wszystkie dokumenty przypisane do danej ciężarówki
     * @param truck ciężarówka
     * @return lista dokumentów
     */
    List<Document> findByTruck(Truck truck);
    
    /**
     * Wyszukuje dokumenty wygasające przed określoną datą
     * @param date data graniczna
     * @return lista wygasających dokumentów
     */
    List<Document> findByExpiryDateBefore(LocalDate date);
    
    /**
     * Wyszukuje dokumenty wygasające w określonym przedziale czasowym
     * @param startDate data początkowa
     * @param endDate data końcowa
     * @return lista dokumentów
     */
    @Query("SELECT d FROM Document d WHERE d.expiryDate BETWEEN :startDate AND :endDate")
    List<Document> findExpiringDocuments(LocalDate startDate, LocalDate endDate);
}
