package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для операций с рейсами/маршрутами
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    /**
     * Ищет рейсы по статусу
     */
    List<Trip> findByStatus(Trip.TripStatus status);
    
    /**
     * Ищет активные рейсы (запланированные или в пути)
     */
    List<Trip> findByStatusIn(List<Trip.TripStatus> statuses);
    
    /**
     * Ищет рейсы для указанного грузовика
     */
    List<Trip> findByTruckId(Long truckId);
    
    /**
     * Ищет рейсы для указанного водителя
     */
    List<Trip> findByDriverId(Long driverId);
    
    /**
     * Ищет активные рейсы для указанного водителя
     */
    List<Trip> findByDriverIdAndStatusIn(Long driverId, List<Trip.TripStatus> statuses);
    
    /**
     * Ищет активные рейсы для указанного грузовика
     */
    List<Trip> findByTruckIdAndStatusIn(Long truckId, List<Trip.TripStatus> statuses);
}
