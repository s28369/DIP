package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverPhone;
import org.example.fleetmanagement.repository.DriverPhoneRepository;
import org.example.fleetmanagement.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for driver operations and their phone numbers.
 */
@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverPhoneRepository phoneRepository;

    // Constructor injection of the driver and driver-phone repositories.
    @Autowired
    public DriverService(DriverRepository driverRepository, DriverPhoneRepository phoneRepository) {
        this.driverRepository = driverRepository;
        this.phoneRepository = phoneRepository;
    }

    // Returns all drivers with their phones and attachments loaded.
    public List<Driver> getAllDrivers() {
        return driverRepository.findAllWithDetails();
    }

    // Finds a single driver by id with its details loaded.
    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findByIdWithDetails(id);
    }

    // Returns drivers that are currently available for a trip.
    public List<Driver> getAvailableDrivers() {
        return driverRepository.findAvailable();
    }

    // Returns drivers filtered by status.
    public List<Driver> getDriversByStatus(String status) {
        return driverRepository.findByStatus(status);
    }

    // Saves a new driver.
    public Driver addDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    // Persists changes to an existing driver.
    public Driver updateDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    // Deletes a driver by id, failing if it does not exist.
    public void deleteDriver(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new IllegalArgumentException("Kierowca o ID " + id + " nie istnieje");
        }
        driverRepository.deleteById(id);
    }

    // --- Phones ---

    // Returns all phone numbers of a given driver.
    public List<DriverPhone> getPhonesByDriver(Long driverId) {
        return phoneRepository.findByDriverId(driverId);
    }

    // Saves a new phone number for a driver.
    public DriverPhone addPhone(DriverPhone phone) {
        return phoneRepository.save(phone);
    }

    // Persists changes to an existing driver phone number.
    public DriverPhone updatePhone(DriverPhone phone) {
        return phoneRepository.save(phone);
    }

    // Deletes a driver phone number by id.
    public void deletePhone(Long phoneId) {
        phoneRepository.deleteById(phoneId);
    }
}
