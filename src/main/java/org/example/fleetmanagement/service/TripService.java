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

/**
 * Service handling trip (route) business logic and the related status changes
 * of the assigned driver, truck and trailer.
 */
@Service
@Transactional
public class TripService {
    
    private final TripRepository tripRepository;
    private final TripNoteRepository tripNoteRepository;
    private final DriverService driverService;
    private final TruckService truckService;
    private final TrailerService trailerService;
    
    // Constructor injection of the trip repositories and the related domain services.
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
    
    // Returns all trips with their related entities eagerly loaded.
    public List<Trip> getAllTrips() {
        return tripRepository.findAllWithDetails();
    }
    
    // Finds a single trip by id with its related entities loaded.
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findByIdWithDetails(id);
    }
    
    // Returns only the active trips (planned or in progress).
    public List<Trip> getActiveTrips() {
        return tripRepository.findByStatusIn(
            Arrays.asList(Trip.TripStatus.PLANNED, Trip.TripStatus.IN_PROGRESS)
        );
    }
    
    // Returns trips filtered by a given status.
    public List<Trip> getTripsByStatus(Trip.TripStatus status) {
        return tripRepository.findByStatus(status);
    }
    
    // Creates a new trip and marks the assigned driver, truck and trailer as "on trip".
    public Trip createTrip(Trip trip) {
        Driver driver = trip.getDriver();
        if (driver != null) {
            driver.setStatus(Driver.STATUS_ON_TRIP);
            driverService.updateDriver(driver);
        }

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
    
    // Persists changes to an existing trip.
    public Trip updateTrip(Trip trip) {
        return tripRepository.save(trip);
    }
    
    // Marks a trip as in progress and records its start time.
    public Trip startTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trasa nie istnieje"));
        
        trip.setStatus(Trip.TripStatus.IN_PROGRESS);
        trip.setStartTime(LocalDateTime.now());
        
        return tripRepository.save(trip);
    }
    
    // Completes a trip, records arrival time and frees the driver, truck and trailer.
    public Trip completeTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trasa nie istnieje"));
        
        trip.setStatus(Trip.TripStatus.COMPLETED);
        trip.setActualArrival(LocalDateTime.now());

        Driver driver = trip.getDriver();
        if (driver != null) {
            driver.setStatus(Driver.STATUS_AVAILABLE);
            driverService.updateDriver(driver);
        }

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
    
    // Cancels a trip and releases the assigned driver, truck and trailer.
    public Trip cancelTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trasa nie istnieje"));
        
        trip.setStatus(Trip.TripStatus.CANCELLED);

        Driver driver = trip.getDriver();
        if (driver != null) {
            driver.setStatus(Driver.STATUS_AVAILABLE);
            driverService.updateDriver(driver);
        }

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
    
    // Deletes a trip; if it was still active, the freed resources are released first.
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Trasa nie istnieje"));

        if (trip.getStatus() == Trip.TripStatus.PLANNED ||
            trip.getStatus() == Trip.TripStatus.IN_PROGRESS) {
            Driver driver = trip.getDriver();
            if (driver != null) {
                driver.setStatus(Driver.STATUS_AVAILABLE);
                driverService.updateDriver(driver);
            }

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

    // Returns the notes of a trip, newest first.
    public List<TripNote> getNotesByTrip(Long tripId) {
        return tripNoteRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }

    // Saves a new note attached to a trip.
    public TripNote addNote(TripNote note) {
        return tripNoteRepository.save(note);
    }

    // Persists changes to an existing trip note.
    public TripNote updateNote(TripNote note) {
        return tripNoteRepository.save(note);
    }

    // Deletes a trip note by id.
    public void deleteNote(Long noteId) {
        tripNoteRepository.deleteById(noteId);
    }
}
