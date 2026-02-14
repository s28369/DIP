package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Przykładowe testy jednostkowe dla TruckService
 */
@ExtendWith(MockitoExtension.class)
class TruckServiceTest {

    @Mock
    private TruckRepository truckRepository;

    @InjectMocks
    private TruckService truckService;

    private Truck testTruck;

    @BeforeEach
    void setUp() {
        testTruck = new Truck();
        testTruck.setId(1L);
        testTruck.setBrand("Volvo FH16");
        testTruck.setRegistrationNumber("WW12345");
        testTruck.setStatus(Truck.TruckStatus.ACTIVE);
    }

    @Test
    void getAllTrucks_ShouldReturnListOfTrucks() {
        List<Truck> expectedTrucks = Arrays.asList(testTruck);
        when(truckRepository.findAll()).thenReturn(expectedTrucks);

        List<Truck> actualTrucks = truckService.getAllTrucks();

        assertNotNull(actualTrucks);
        assertEquals(1, actualTrucks.size());
        assertEquals(testTruck.getBrand(), actualTrucks.get(0).getBrand());
        verify(truckRepository, times(1)).findAll();
    }

    @Test
    void getTruckById_WhenTruckExists_ShouldReturnTruck() {
        when(truckRepository.findById(1L)).thenReturn(Optional.of(testTruck));

        Optional<Truck> result = truckService.getTruckById(1L);

        assertTrue(result.isPresent());
        assertEquals(testTruck.getBrand(), result.get().getBrand());
        verify(truckRepository, times(1)).findById(1L);
    }

    @Test
    void addTruck_WhenRegistrationNumberIsUnique_ShouldSaveTruck() {
        when(truckRepository.existsByRegistrationNumber(testTruck.getRegistrationNumber())).thenReturn(false);
        when(truckRepository.save(any(Truck.class))).thenReturn(testTruck);

        Truck savedTruck = truckService.addTruck(testTruck);

        assertNotNull(savedTruck);
        assertEquals(testTruck.getBrand(), savedTruck.getBrand());
        verify(truckRepository, times(1)).existsByRegistrationNumber(testTruck.getRegistrationNumber());
        verify(truckRepository, times(1)).save(testTruck);
    }

    @Test
    void addTruck_WhenRegistrationNumberExists_ShouldThrowException() {
        when(truckRepository.existsByRegistrationNumber(testTruck.getRegistrationNumber())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            truckService.addTruck(testTruck);
        });
        verify(truckRepository, times(1)).existsByRegistrationNumber(testTruck.getRegistrationNumber());
        verify(truckRepository, never()).save(any());
    }

    @Test
    void deleteTruck_WhenTruckExists_ShouldDeleteTruck() {
        when(truckRepository.existsById(1L)).thenReturn(true);
        doNothing().when(truckRepository).deleteById(1L);

        truckService.deleteTruck(1L);

        verify(truckRepository, times(1)).existsById(1L);
        verify(truckRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTruck_WhenTruckDoesNotExist_ShouldThrowException() {
        when(truckRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            truckService.deleteTruck(1L);
        });
        verify(truckRepository, times(1)).existsById(1L);
        verify(truckRepository, never()).deleteById(any());
    }

    @Test
    void getTrucksByStatus_ShouldReturnTrucksWithGivenStatus() {
        List<Truck> activeTrucks = Arrays.asList(testTruck);
        when(truckRepository.findByStatus(Truck.TruckStatus.ACTIVE)).thenReturn(activeTrucks);

        List<Truck> result = truckService.getTrucksByStatus(Truck.TruckStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Truck.TruckStatus.ACTIVE, result.get(0).getStatus());
        verify(truckRepository, times(1)).findByStatus(Truck.TruckStatus.ACTIVE);
    }
}
