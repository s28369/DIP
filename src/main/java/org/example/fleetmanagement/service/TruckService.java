package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serwis obsługujący operacje na ciężarówkach
 */
@Service
@Transactional
public class TruckService {
    
    private final TruckRepository truckRepository;
    
    @Autowired
    public TruckService(TruckRepository truckRepository) {
        this.truckRepository = truckRepository;
    }
    
    /**
     * Zwraca wszystkie ciężarówki
     * @return lista wszystkich ciężarówek
     */
    public List<Truck> getAllTrucks() {
        return truckRepository.findAll();
    }
    
    /**
     * Wyszukuje ciężarówkę po ID
     * @param id identyfikator ciężarówki
     * @return Optional z ciężarówką jeśli istnieje
     */
    public Optional<Truck> getTruckById(Long id) {
        return truckRepository.findById(id);
    }
    
    /**
     * Dodaje nową ciężarówkę do systemu
     * @param truck ciężarówka do dodania
     * @return zapisana ciężarówka
     * @throws IllegalArgumentException jeśli ciężarówka o podanym numerze rejestracyjnym już istnieje
     */
    public Truck addTruck(Truck truck) {
        if (truckRepository.existsByRegistrationNumber(truck.getRegistrationNumber())) {
            throw new IllegalArgumentException("Ciężarówka o numerze rejestracyjnym " 
                + truck.getRegistrationNumber() + " już istnieje w systemie");
        }
        return truckRepository.save(truck);
    }
    
    /**
     * Aktualizuje dane ciężarówki
     * @param truck ciężarówka z zaktualizowanymi danymi
     * @return zaktualizowana ciężarówka
     */
    public Truck updateTruck(Truck truck) {
        return truckRepository.save(truck);
    }
    
    /**
     * Usuwa ciężarówkę z systemu
     * @param id identyfikator ciężarówki do usunięcia
     * @throws IllegalArgumentException jeśli ciężarówka nie istnieje
     */
    public void deleteTruck(Long id) {
        if (!truckRepository.existsById(id)) {
            throw new IllegalArgumentException("Ciężarówka o ID " + id + " nie istnieje");
        }
        truckRepository.deleteById(id);
    }
    
    /**
     * Wyszukuje ciężarówki po statusie
     * @param status status ciężarówki
     * @return lista ciężarówek o danym statusie
     */
    public List<Truck> getTrucksByStatus(Truck.TruckStatus status) {
        return truckRepository.findByStatus(status);
    }
}
