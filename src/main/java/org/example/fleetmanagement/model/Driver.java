package org.example.fleetmanagement.model;

import jakarta.persistence.*;

/**
 * Klasa reprezentująca kierowcę w systemie zarządzania flotą
 */
@Entity
@Table(name = "driver")
public class Driver {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "license_number", nullable = false, unique = true, length = 30)
    private String licenseNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverStatus status = DriverStatus.AVAILABLE;
    
    /**
     * Enum określający status kierowcy
     */
    public enum DriverStatus {
        AVAILABLE,
        ON_TRIP,
        ON_LEAVE,
        UNAVAILABLE
    }
    

    
    public Driver() {
    }
    
    public Driver(String firstName, String lastName, String phoneNumber, String licenseNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.licenseNumber = licenseNumber;
        this.status = DriverStatus.AVAILABLE;
    }
    

    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    
    public DriverStatus getStatus() {
        return status;
    }
    
    public void setStatus(DriverStatus status) {
        this.status = status;
    }
    
    /**
     * Zwraca pełne imię i nazwisko kierowcy
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    @Override
    public String toString() {
        return getFullName() + " (" + licenseNumber + ")";
    }
}
