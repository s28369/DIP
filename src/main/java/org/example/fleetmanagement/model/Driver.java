package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "driver")
public class Driver {

    public static final String STATUS_AVAILABLE = "Доступен";
    public static final String STATUS_ON_TRIP = "в рейсе";
    public static final String STATUS_MAINTENANCE = "на ремонте";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 50)
    private String status = STATUS_AVAILABLE;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DriverPhone> phones = new ArrayList<>();

    public Driver() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DriverPhone> getPhones() {
        return phones;
    }

    public void setPhones(List<DriverPhone> phones) {
        this.phones = phones;
    }

    public int getPhoneCount() {
        return phones != null ? phones.size() : 0;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
