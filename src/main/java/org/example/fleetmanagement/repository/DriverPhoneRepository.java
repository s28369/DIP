package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.DriverPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for driver phone numbers.
 */
@Repository
public interface DriverPhoneRepository extends JpaRepository<DriverPhone, Long> {

    // Returns all phone numbers of the given driver.
    List<DriverPhone> findByDriverId(Long driverId);
}
