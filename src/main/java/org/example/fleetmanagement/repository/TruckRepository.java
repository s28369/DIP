package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium do obsługi operacji na encji Truck
 */
@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {
    
    /**
     * Wyszukuje ciężarówkę po numerze rejestracyjnym
     * @param registrationNumber numer rejestracyjny
     * @return Optional z ciężarówką jeśli istnieje
     */
    Optional<Truck> findByRegistrationNumber(String registrationNumber);
    
    /**
     * Wyszukuje ciężarówki po statusie
     * @param status status ciężarówki
     * @return lista ciężarówek o danym statusie
     */
    List<Truck> findByStatus(Truck.TruckStatus status);
    
    /**
     * Sprawdza czy ciężarówka o podanym numerze rejestracyjnym istnieje
     * @param registrationNumber numer rejestracyjny
     * @return true jeśli istnieje, false w przeciwnym wypadku
     */
    boolean existsByRegistrationNumber(String registrationNumber);
}
