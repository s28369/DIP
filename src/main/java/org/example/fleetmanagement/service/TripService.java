package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.*;
import org.example.fleetmanagement.repository.TripNoteRepository;
import org.example.fleetmanagement.repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TripService {
    
    private final TripRepository tripRepository;
    private final TripNoteRepository tripNoteRepository;
    private final DriverService driverService;
    private final TruckService truckService;
    private final TrailerService trailerService;
    
    @Autowired
    public TripService(TripRepository tripRepository,
                       TripNoteRepository tripNoteRepository,
                       DriverService driverService,
                       TruckService truckService,
                       TrailerService trailerService) {
        this.tripRepository = tripRepository;
        this.tripNoteRepository = tripNoteRepository;
        this.driverService = driverService;
        this.truckService = truckService;
        this.trailerService = trailerService;
    }
    
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }
    
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }
    
    public List<Trip> getActiveTrips() {
        return tripRepository.findByStatusIn(
            Arrays.asList(Trip.TripStatus.PLANNED, Trip.TripStatus.IN_PROGRESS)
        );
    }
    
    public List<Trip> getTripsByStatus(Trip.TripStatus status) {
        return tripRepository.findByStatus(status);
    }
    
    public Trip createTrip(Trip trip) {
        Driver driver = trip.getDriver();
        driver.setStatus(Driver.STATUS_ON_TRIP);
        driverService.updateDriver(driver);

        Truck truck = trip.getTruck();
        truck.setStatus(Truck.STATUS_ON_TRIP);
        truck.setCurrentLocation(trip.getOrigin());
        truck.setCargoDescription(trip.getCargoDescription());
        truckService.updateTruck(truck);

        Trailer trailer = trip.getTrailer();
        if (trailer != null) {
            trailer.setStatus(Trailer.STATUS_ON_TRIP);
            trailer.setCurrentLocation(trip.getOrigin());
            trailerService.updateTrailer(trailer);
        }
        
        return tripRepository.save(trip);
    }
    
    public Trip updateTrip(Trip trip) {
        return tripRepository.save(trip);
    }
    
    public Trip startTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Рейс не существует"));
        
        trip.setStatus(Trip.TripStatus.IN_PROGRESS);
        trip.setStartTime(LocalDateTime.now());
        
        return tripRepository.save(trip);
    }
    
    public Trip completeTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Рейс не существует"));
        
        trip.setStatus(Trip.TripStatus.COMPLETED);
        trip.setActualArrival(LocalDateTime.now());

        Driver driver = trip.getDriver();
        driver.setStatus(Driver.STATUS_AVAILABLE);
        driverService.updateDriver(driver);

        Truck truck = trip.getTruck();
        truck.setStatus(Truck.STATUS_AVAILABLE);
        truck.setCurrentLocation(trip.getDestination());
        truck.setCargoDescription(null);
        truckService.updateTruck(truck);

        Trailer trailer = trip.getTrailer();
        if (trailer != null) {
            trailer.setStatus(Trailer.STATUS_AVAILABLE);
            trailer.setCurrentLocation(trip.getDestination());
            trailerService.updateTrailer(trailer);
        }
        
        return tripRepository.save(trip);
    }
    
    public Trip cancelTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Рейс не существует"));
        
        trip.setStatus(Trip.TripStatus.CANCELLED);

        Driver driver = trip.getDriver();
        driver.setStatus(Driver.STATUS_AVAILABLE);
        driverService.updateDriver(driver);

        Truck truck = trip.getTruck();
        truck.setStatus(Truck.STATUS_AVAILABLE);
        truckService.updateTruck(truck);

        Trailer trailer = trip.getTrailer();
        if (trailer != null) {
            trailer.setStatus(Trailer.STATUS_AVAILABLE);
            trailerService.updateTrailer(trailer);
        }
        
        return tripRepository.save(trip);
    }
    
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Рейс не существует"));

        if (trip.getStatus() == Trip.TripStatus.PLANNED ||
            trip.getStatus() == Trip.TripStatus.IN_PROGRESS) {
            Driver driver = trip.getDriver();
            driver.setStatus(Driver.STATUS_AVAILABLE);
            driverService.updateDriver(driver);

            Truck truck = trip.getTruck();
            truck.setStatus(Truck.STATUS_AVAILABLE);
            truckService.updateTruck(truck);

            Trailer trailer = trip.getTrailer();
            if (trailer != null) {
                trailer.setStatus(Trailer.STATUS_AVAILABLE);
                trailerService.updateTrailer(trailer);
            }
        }
        
        tripRepository.deleteById(id);
    }

    // --- Notes ---

    public List<TripNote> getNotesByTrip(Long tripId) {
        return tripNoteRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }

    public TripNote addNote(TripNote note) {
        return tripNoteRepository.save(note);
    }

    public TripNote updateNote(TripNote note) {
        return tripNoteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        tripNoteRepository.deleteById(noteId);
    }
}
