package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Finds a user by username.
    Optional<User> findByUsername(String username);
    
    // Returns true if a user with the given username exists.
    boolean existsByUsername(String username);
}
