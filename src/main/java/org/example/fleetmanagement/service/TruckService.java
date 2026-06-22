package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for truck (tractor unit) operations.
 */
@Service
@Transactional
public class TruckService {
    
    private final TruckRepository truckRepository;
    
    // Constructor injection of the truck repository.
    @Autowired
    public TruckService(TruckRepository truckRepository) {
        this.truckRepository = truckRepository;
    }
    
    // Returns all trucks with their related details loaded.
    public List<Truck> getAllTrucks() {
        return truckRepository.findAllWithDetails();
    }
    
    // Finds a single truck by id with its details loaded.
    public Optional<Truck> getTruckById(Long id) {
        return truckRepository.findByIdWithDetails(id);
    }
    
    // Adds a new truck, rejecting duplicate registration numbers.
    public Truck addTruck(Truck truck) {
        if (truckRepository.existsByRegistrationNumber(truck.getRegistrationNumber())) {
            throw new IllegalArgumentException("Ciągnik o numerze rejestracyjnym " 
                + truck.getRegistrationNumber() + " już istnieje w systemie");
        }
        return truckRepository.save(truck);
    }
    
    // Persists changes to an existing truck.
    public Truck updateTruck(Truck truck) {
        return truckRepository.save(truck);
    }
    
    // Deletes a truck by id, failing if it does not exist.
    public void deleteTruck(Long id) {
        if (!truckRepository.existsById(id)) {
            throw new IllegalArgumentException("Ciągnik o ID " + id + " nie istnieje");
        }
        truckRepository.deleteById(id);
    }
    
    // Returns trucks filtered by status.
    public List<Truck> getTrucksByStatus(String status) {
        return truckRepository.findByStatus(status);
    }
}
