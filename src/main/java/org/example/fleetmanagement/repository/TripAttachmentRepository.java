package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TripAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozytorium do obsługi operacji na załącznikach rejsów
 */
@Repository
public interface TripAttachmentRepository extends JpaRepository<TripAttachment, Long> {
    
    /**
     * Wyszukuje załączniki przypisane do rejsu
     * @param tripId identyfikator rejsu
     * @return lista załączników
     */
    List<TripAttachment> findByTripId(Long tripId);
    
    /**
     * Usuwa wszystkie załączniki przypisane do rejsu
     * @param tripId identyfikator rejsu
     */
    void deleteByTripId(Long tripId);
}
