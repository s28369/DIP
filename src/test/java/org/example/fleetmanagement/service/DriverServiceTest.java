package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverPhone;
import org.example.fleetmanagement.repository.DriverPhoneRepository;
import org.example.fleetmanagement.repository.DriverRepository;
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
 * Unit tests for DriverService.
 */
@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverPhoneRepository phoneRepository;

    @InjectMocks
    private DriverService driverService;

    private Driver testDriver;

    @BeforeEach
    void setUp() {
        testDriver = new Driver();
        testDriver.setId(1L);
        testDriver.setFullName("Jan Kowalski");
        testDriver.setStatus(Driver.STATUS_AVAILABLE);
    }

    @Test
    void getAllDrivers_ShouldReturnDriversWithDetails() {
        when(driverRepository.findAllWithDetails()).thenReturn(Arrays.asList(testDriver));

        List<Driver> result = driverService.getAllDrivers();

        assertEquals(1, result.size());
        assertEquals("Jan Kowalski", result.get(0).getFullName());
        verify(driverRepository).findAllWithDetails();
    }

    @Test
    void getDriverById_ShouldReturnDriver() {
        when(driverRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testDriver));

        Optional<Driver> result = driverService.getDriverById(1L);

        assertTrue(result.isPresent());
        verify(driverRepository).findByIdWithDetails(1L);
    }

    @Test
    void getAvailableDrivers_ShouldDelegateToRepository() {
        when(driverRepository.findAvailable()).thenReturn(Arrays.asList(testDriver));

        List<Driver> result = driverService.getAvailableDrivers();

        assertEquals(1, result.size());
        verify(driverRepository).findAvailable();
    }

    @Test
    void getDriversByStatus_ShouldDelegateToRepository() {
        when(driverRepository.findByStatus(Driver.STATUS_AVAILABLE)).thenReturn(Arrays.asList(testDriver));

        List<Driver> result = driverService.getDriversByStatus(Driver.STATUS_AVAILABLE);

        assertEquals(1, result.size());
        verify(driverRepository).findByStatus(Driver.STATUS_AVAILABLE);
    }

    @Test
    void addDriver_ShouldSaveDriver() {
        when(driverRepository.save(any(Driver.class))).thenReturn(testDriver);

        Driver result = driverService.addDriver(testDriver);

        assertNotNull(result);
        verify(driverRepository).save(testDriver);
    }

    @Test
    void deleteDriver_WhenDriverExists_ShouldDelete() {
        when(driverRepository.existsById(1L)).thenReturn(true);

        driverService.deleteDriver(1L);

        verify(driverRepository).deleteById(1L);
    }

    @Test
    void deleteDriver_WhenDriverDoesNotExist_ShouldThrow() {
        when(driverRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> driverService.deleteDriver(1L));
        verify(driverRepository, never()).deleteById(any());
    }

    @Test
    void getPhonesByDriver_ShouldDelegateToPhoneRepository() {
        DriverPhone phone = new DriverPhone();
        when(phoneRepository.findByDriverId(1L)).thenReturn(Arrays.asList(phone));

        List<DriverPhone> result = driverService.getPhonesByDriver(1L);

        assertEquals(1, result.size());
        verify(phoneRepository).findByDriverId(1L);
    }

    @Test
    void addPhone_ShouldSavePhone() {
        DriverPhone phone = new DriverPhone();
        when(phoneRepository.save(phone)).thenReturn(phone);

        DriverPhone result = driverService.addPhone(phone);

        assertNotNull(result);
        verify(phoneRepository).save(phone);
    }

    @Test
    void deletePhone_ShouldDelegateToPhoneRepository() {
        driverService.deletePhone(5L);

        verify(phoneRepository).deleteById(5L);
    }
}
