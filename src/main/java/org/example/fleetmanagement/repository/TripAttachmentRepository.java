package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.TripAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для операций с вложениями рейсов
 */
@Repository
public interface TripAttachmentRepository extends JpaRepository<TripAttachment, Long> {
    
    /**
     * Ищет вложения, привязанные к рейсу
     * @param tripId идентификатор рейса
     * @return список вложений
     */
    List<TripAttachment> findByTripId(Long tripId);
    
    /**
     * Удаляет все вложения, привязанные к рейсу
     * @param tripId идентификатор рейса
     */
    void deleteByTripId(Long tripId);
}
