package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozytorium do obsługi operacji na rejsach/trasach
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    /**
     * Wyszukuje rejsy po statusie
     */
    List<Trip> findByStatus(Trip.TripStatus status);
    
    /**
     * Wyszukuje aktywne rejsy (zaplanowane lub w trakcie)
     */
    List<Trip> findByStatusIn(List<Trip.TripStatus> statuses);
    
    /**
     * Wyszukuje rejsy dla danej ciężarówki
     */
    List<Trip> findByTruckId(Long truckId);
    
    /**
     * Wyszukuje rejsy dla danego kierowcy
     */
    List<Trip> findByDriverId(Long driverId);
    
    /**
     * Wyszukuje aktywne rejsy dla danego kierowcy
     */
    List<Trip> findByDriverIdAndStatusIn(Long driverId, List<Trip.TripStatus> statuses);
    
    /**
     * Wyszukuje aktywne rejsy dla danej ciężarówki
     */
    List<Trip> findByTruckIdAndStatusIn(Long truckId, List<Trip.TripStatus> statuses);
}
