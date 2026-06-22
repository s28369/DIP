package org.example.fleetmanagement.model;

import jakarta.persistence.*;

/**
 * JPA entity representing a customer (the party a trip is carried out for).
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    // Default constructor required by JPA.
    public Customer() {
    }

    // Convenience constructor with the customer name.
    public Customer(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Returns the customer name (used as the label in combo boxes).
    @Override
    public String toString() {
        return name;
    }
}
