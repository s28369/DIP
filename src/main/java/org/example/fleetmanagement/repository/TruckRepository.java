package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Truck entity operations.
 */
@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {

    // Finds a truck by its registration number.
    Optional<Truck> findByRegistrationNumber(String registrationNumber);

    // Returns trucks filtered by status.
    List<Truck> findByStatus(String status);

    // Returns true if a truck with the given registration number exists.
    boolean existsByRegistrationNumber(String registrationNumber);

    // Returns all trucks with their attachments eagerly fetched.
    @Query("SELECT DISTINCT t FROM Truck t LEFT JOIN FETCH t.attachments")
    List<Truck> findAllWithDetails();

    // Returns a single truck by id with its attachments eagerly fetched.
    @Query("SELECT DISTINCT t FROM Truck t LEFT JOIN FETCH t.attachments WHERE t.id = :id")
    Optional<Truck> findByIdWithDetails(Long id);
}
