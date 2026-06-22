package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Trailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Trailer entity operations.
 */
@Repository
public interface TrailerRepository extends JpaRepository<Trailer, Long> {

    // Finds a trailer by its registration number.
    Optional<Trailer> findByRegistrationNumber(String registrationNumber);

    // Returns trailers filtered by status.
    List<Trailer> findByStatus(String status);

    // Returns true if a trailer with the given registration number exists.
    boolean existsByRegistrationNumber(String registrationNumber);

    // Returns all trailers with their notes and attachments eagerly fetched.
    @Query("SELECT DISTINCT t FROM Trailer t LEFT JOIN FETCH t.notes LEFT JOIN FETCH t.attachments")
    List<Trailer> findAllWithDetails();

    // Returns a single trailer by id with its notes and attachments eagerly fetched.
    @Query("SELECT DISTINCT t FROM Trailer t LEFT JOIN FETCH t.notes LEFT JOIN FETCH t.attachments WHERE t.id = :id")
    Optional<Trailer> findByIdWithDetails(Long id);
}
