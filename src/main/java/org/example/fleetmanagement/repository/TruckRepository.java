package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для операций с сущностью Truck
 */
@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {
    
    /**
     * Ищет грузовик по регистрационному номеру
     * @param registrationNumber регистрационный номер
     * @return Optional с грузовиком, если существует
     */
    Optional<Truck> findByRegistrationNumber(String registrationNumber);
    
    /**
     * Ищет грузовики по статусу
     * @param status статус грузовика
     * @return список грузовиков с указанным статусом
     */
    List<Truck> findByStatus(String status);
    
    /**
     * Проверяет, существует ли грузовик с указанным регистрационным номером
     * @param registrationNumber регистрационный номер
     * @return true, если существует, false в противном случае
     */
    boolean existsByRegistrationNumber(String registrationNumber);
}
