package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByStatus(String status);

    default List<Driver> findAvailable() {
        return findByStatus(Driver.STATUS_AVAILABLE);
    }

    @Query("SELECT DISTINCT d FROM Driver d LEFT JOIN FETCH d.phones LEFT JOIN FETCH d.attachments")
    List<Driver> findAllWithDetails();

    @Query("SELECT DISTINCT d FROM Driver d LEFT JOIN FETCH d.phones LEFT JOIN FETCH d.attachments WHERE d.id = :id")
    Optional<Driver> findByIdWithDetails(Long id);
}
