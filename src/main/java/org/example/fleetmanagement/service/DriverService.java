package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serwis obsługujący operacje na kierowcach
 */
@Service
@Transactional
public class DriverService {
    
    private final DriverRepository driverRepository;
    
    @Autowired
    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }
    
    /**
     * Zwraca wszystkich kierowców
     */
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }
    
    /**
     * Wyszukuje kierowcę po ID
     */
    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }
    
    /**
     * Zwraca dostępnych kierowców
     */
    public List<Driver> getAvailableDrivers() {
        return driverRepository.findAvailable();
    }
    
    /**
     * Zwraca kierowców po statusie
     */
    public List<Driver> getDriversByStatus(Driver.DriverStatus status) {
        return driverRepository.findByStatus(status);
    }
    
    /**
     * Dodaje nowego kierowcę
     */
    public Driver addDriver(Driver driver) {
        if (driverRepository.existsByLicenseNumber(driver.getLicenseNumber())) {
            throw new IllegalArgumentException("Kierowca o numerze prawa jazdy " 
                + driver.getLicenseNumber() + " już istnieje");
        }
        return driverRepository.save(driver);
    }
    
    /**
     * Aktualizuje dane kierowcy
     */
    public Driver updateDriver(Driver driver) {
        return driverRepository.save(driver);
    }
    
    /**
     * Usuwa kierowcę
     */
    public void deleteDriver(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new IllegalArgumentException("Kierowca o ID " + id + " nie istnieje");
        }
        driverRepository.deleteById(id);
    }
}
