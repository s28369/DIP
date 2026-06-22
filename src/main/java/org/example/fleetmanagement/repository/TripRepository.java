package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Trip entity operations.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Returns trips with the given status.
    List<Trip> findByStatus(Trip.TripStatus status);

    // Returns trips whose status is in the given list.
    List<Trip> findByStatusIn(List<Trip.TripStatus> statuses);

    // Returns trips assigned to the given truck.
    List<Trip> findByTruckId(Long truckId);

    // Returns trips assigned to the given driver.
    List<Trip> findByDriverId(Long driverId);

    // Returns trips of the given driver whose status is in the given list.
    List<Trip> findByDriverIdAndStatusIn(Long driverId, List<Trip.TripStatus> statuses);

    // Returns trips of the given truck whose status is in the given list.
    List<Trip> findByTruckIdAndStatusIn(Long truckId, List<Trip.TripStatus> statuses);

    // Returns all trips with attachments and notes eagerly fetched.
    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.attachments LEFT JOIN FETCH t.tripNotes")
    List<Trip> findAllWithDetails();

    // Returns a single trip by id with attachments and notes eagerly fetched.
    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.attachments LEFT JOIN FETCH t.tripNotes WHERE t.id = :id")
    Optional<Trip> findByIdWithDetails(Long id);
}
