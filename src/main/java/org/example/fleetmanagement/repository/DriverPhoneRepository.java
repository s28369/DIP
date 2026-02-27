package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.DriverPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverPhoneRepository extends JpaRepository<DriverPhone, Long> {

    List<DriverPhone> findByDriverId(Long driverId);
}
