package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {

    Optional<Truck> findByRegistrationNumber(String registrationNumber);

    List<Truck> findByStatus(String status);

    boolean existsByRegistrationNumber(String registrationNumber);

    @Query("SELECT DISTINCT t FROM Truck t LEFT JOIN FETCH t.attachments")
    List<Truck> findAllWithDetails();

    @Query("SELECT DISTINCT t FROM Truck t LEFT JOIN FETCH t.attachments WHERE t.id = :id")
    Optional<Truck> findByIdWithDetails(Long id);
}
