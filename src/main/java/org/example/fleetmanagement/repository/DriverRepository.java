package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium do obsługi operacji na kierowcach
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    /**
     * Wyszukuje kierowcę po numerze prawa jazdy
     */
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    
    /**
     * Sprawdza czy kierowca o podanym numerze prawa jazdy istnieje
     */
    boolean existsByLicenseNumber(String licenseNumber);
    
    /**
     * Wyszukuje kierowców po statusie
     */
    List<Driver> findByStatus(Driver.DriverStatus status);
    
    /**
     * Wyszukuje dostępnych kierowców
     */
    default List<Driver> findAvailable() {
        return findByStatus(Driver.DriverStatus.AVAILABLE);
    }
}
