package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TruckAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozytorium do obsługi operacji na załącznikach ciężarówek
 */
@Repository
public interface TruckAttachmentRepository extends JpaRepository<TruckAttachment, Long> {
    
    /**
     * Wyszukuje załączniki przypisane do ciężarówki
     * @param truckId identyfikator ciężarówki
     * @return lista załączników
     */
    List<TruckAttachment> findByTruckId(Long truckId);
    
    /**
     * Usuwa wszystkie załączniki przypisane do ciężarówki
     * @param truckId identyfikator ciężarówki
     */
    void deleteByTruckId(Long truckId);
}
