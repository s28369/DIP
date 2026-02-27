package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Trailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrailerRepository extends JpaRepository<Trailer, Long> {

    Optional<Trailer> findByRegistrationNumber(String registrationNumber);

    List<Trailer> findByStatus(String status);

    boolean existsByRegistrationNumber(String registrationNumber);
}
