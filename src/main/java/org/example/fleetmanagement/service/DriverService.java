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

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverPhoneRepository phoneRepository;

    @Autowired
    public DriverService(DriverRepository driverRepository, DriverPhoneRepository phoneRepository) {
        this.driverRepository = driverRepository;
        this.phoneRepository = phoneRepository;
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAllWithDetails();
    }

    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findByIdWithDetails(id);
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findAvailable();
    }

    public List<Driver> getDriversByStatus(String status) {
        return driverRepository.findByStatus(status);
    }

    public Driver addDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    public Driver updateDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    public void deleteDriver(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new IllegalArgumentException("Водитель с ID " + id + " не существует");
        }
        driverRepository.deleteById(id);
    }

    // --- Phones ---

    public List<DriverPhone> getPhonesByDriver(Long driverId) {
        return phoneRepository.findByDriverId(driverId);
    }

    public DriverPhone addPhone(DriverPhone phone) {
        return phoneRepository.save(phone);
    }

    public DriverPhone updatePhone(DriverPhone phone) {
        return phoneRepository.save(phone);
    }

    public void deletePhone(Long phoneId) {
        phoneRepository.deleteById(phoneId);
    }
}
