package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.Trip;
import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Serwis obsługujący operacje na rejsach/trasach
 */
@Service
@Transactional
public class TripService {
    
    private final TripRepository tripRepository;
    private final DriverService driverService;
    private final TruckService truckService;
    
    @Autowired
    public TripService(TripRepository tripRepository, DriverService driverService, TruckService truckService) {
        this.tripRepository = tripRepository;
        this.driverService = driverService;
        this.truckService = truckService;
    }
    
    /**
     * Zwraca wszystkie rejsy
     */
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }
    
    /**
     * Wyszukuje rejs po ID
     */
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }
    
    /**
     * Zwraca aktywne rejsy (zaplanowane i w trakcie)
     */
    public List<Trip> getActiveTrips() {
        return tripRepository.findByStatusIn(
            Arrays.asList(Trip.TripStatus.PLANNED, Trip.TripStatus.IN_PROGRESS)
        );
    }
    
    /**
     * Zwraca rejsy po statusie
     */
    public List<Trip> getTripsByStatus(Trip.TripStatus status) {
        return tripRepository.findByStatus(status);
    }
    
    /**
     * Tworzy nowy rejs
     */
    public Trip createTrip(Trip trip) {

        Driver driver = trip.getDriver();
        driver.setStatus(Driver.DriverStatus.ON_TRIP);
        driverService.updateDriver(driver);
        

        Truck truck = trip.getTruck();
        truck.setStatus(Truck.TruckStatus.ACTIVE);
        truck.setCurrentLocation(trip.getOrigin());
        truck.setCargoDescription(trip.getCargoDescription());
        truckService.updateTruck(truck);
        
        return tripRepository.save(trip);
    }
    
    /**
     * Aktualizuje rejs
     */
    public Trip updateTrip(Trip trip) {
        return tripRepository.save(trip);
    }
    
    /**
     * Rozpoczyna rejs
     */
    public Trip startTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Rejs nie istnieje"));
        
        trip.setStatus(Trip.TripStatus.IN_PROGRESS);
        trip.setStartTime(LocalDateTime.now());
        
        return tripRepository.save(trip);
    }
    
    /**
     * Kończy rejs
     */
    public Trip completeTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Rejs nie istnieje"));
        
        trip.setStatus(Trip.TripStatus.COMPLETED);
        trip.setActualArrival(LocalDateTime.now());
        

        Driver driver = trip.getDriver();
        driver.setStatus(Driver.DriverStatus.AVAILABLE);
        driverService.updateDriver(driver);
        

        Truck truck = trip.getTruck();
        truck.setCurrentLocation(trip.getDestination());
        truck.setCargoDescription(null);
        truckService.updateTruck(truck);
        
        return tripRepository.save(trip);
    }
    
    /**
     * Anuluje rejs
     */
    public Trip cancelTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Rejs nie istnieje"));
        
        trip.setStatus(Trip.TripStatus.CANCELLED);
        

        Driver driver = trip.getDriver();
        driver.setStatus(Driver.DriverStatus.AVAILABLE);
        driverService.updateDriver(driver);
        
        return tripRepository.save(trip);
    }
    
    /**
     * Usuwa rejs
     */
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rejs nie istnieje"));

        if (trip.getStatus() == Trip.TripStatus.PLANNED ||
            trip.getStatus() == Trip.TripStatus.IN_PROGRESS) {
            Driver driver = trip.getDriver();
            driver.setStatus(Driver.DriverStatus.AVAILABLE);
            driverService.updateDriver(driver);
        }
        
        tripRepository.deleteById(id);
    }
}
