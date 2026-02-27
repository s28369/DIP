package org.example.fleetmanagement.model;

import jakarta.persistence.*;

@Entity
@Table(name = "driver_phone")
public class DriverPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(length = 50)
    private String country;

    public DriverPhone() {
    }

    public DriverPhone(String phoneNumber, String country, Driver driver) {
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.driver = driver;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
