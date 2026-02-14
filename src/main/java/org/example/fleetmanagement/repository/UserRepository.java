package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repozytorium do obsługi operacji na encji User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Wyszukuje użytkownika po nazwie użytkownika
     * @param username nazwa użytkownika
     * @return Optional z użytkownikiem jeśli istnieje
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Sprawdza czy użytkownik o podanej nazwie istnieje
     * @param username nazwa użytkownika
     * @return true jeśli istnieje, false w przeciwnym wypadku
     */
    boolean existsByUsername(String username);
}
