package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByStatus(Trip.TripStatus status);

    List<Trip> findByStatusIn(List<Trip.TripStatus> statuses);

    List<Trip> findByTruckId(Long truckId);

    List<Trip> findByDriverId(Long driverId);

    List<Trip> findByDriverIdAndStatusIn(Long driverId, List<Trip.TripStatus> statuses);

    List<Trip> findByTruckIdAndStatusIn(Long truckId, List<Trip.TripStatus> statuses);

    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.attachments LEFT JOIN FETCH t.tripNotes")
    List<Trip> findAllWithDetails();

    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.attachments LEFT JOIN FETCH t.tripNotes WHERE t.id = :id")
    Optional<Trip> findByIdWithDetails(Long id);
}
