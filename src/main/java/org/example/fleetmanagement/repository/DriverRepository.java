package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Driver entity operations.
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Returns drivers filtered by status.
    List<Driver> findByStatus(String status);

    // Convenience method returning only the currently available drivers.
    default List<Driver> findAvailable() {
        return findByStatus(Driver.STATUS_AVAILABLE);
    }

    // Returns all drivers with their phones and attachments eagerly fetched.
    @Query("SELECT DISTINCT d FROM Driver d LEFT JOIN FETCH d.phones LEFT JOIN FETCH d.attachments")
    List<Driver> findAllWithDetails();

    // Returns a single driver by id with phones and attachments eagerly fetched.
    @Query("SELECT DISTINCT d FROM Driver d LEFT JOIN FETCH d.phones LEFT JOIN FETCH d.attachments WHERE d.id = :id")
    Optional<Driver> findByIdWithDetails(Long id);
}
