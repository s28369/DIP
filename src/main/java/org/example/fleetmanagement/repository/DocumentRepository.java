package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Document;
import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Репозиторий для операций с сущностью Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * Ищет все документы, привязанные к указанному грузовику
     * @param truck грузовик
     * @return список документов
     */
    List<Document> findByTruck(Truck truck);
    
    /**
     * Ищет документы, срок действия которых истекает до указанной даты
     * @param date граничная дата
     * @return список истекающих документов
     */
    List<Document> findByExpiryDateBefore(LocalDate date);
    
    /**
     * Ищет документы, срок действия которых истекает в указанном временном интервале
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return список документов
     */
    @Query("SELECT d FROM Document d WHERE d.expiryDate BETWEEN :startDate AND :endDate")
    List<Document> findExpiringDocuments(LocalDate startDate, LocalDate endDate);
}
