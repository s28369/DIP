package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TruckAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для операций с вложениями грузовиков
 */
@Repository
public interface TruckAttachmentRepository extends JpaRepository<TruckAttachment, Long> {
    
    /**
     * Ищет вложения, привязанные к грузовику
     * @param truckId идентификатор грузовика
     * @return список вложений
     */
    List<TruckAttachment> findByTruckId(Long truckId);
    
    /**
     * Удаляет все вложения, привязанные к грузовику
     * @param truckId идентификатор грузовика
     */
    void deleteByTruckId(Long truckId);
}
