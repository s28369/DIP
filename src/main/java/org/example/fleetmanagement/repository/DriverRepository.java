package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByStatus(String status);

    default List<Driver> findAvailable() {
        return findByStatus(Driver.STATUS_AVAILABLE);
    }
}
