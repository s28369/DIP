package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a driver, with their phone numbers and document attachments.
 */
@Entity
@Table(name = "driver")
public class Driver {

    public static final String STATUS_AVAILABLE = "Dostępny";
    public static final String STATUS_ON_TRIP = "W trasie";
    public static final String STATUS_MAINTENANCE = "W naprawie";

    public static final String COMPANY_MTG = "MTG";
    public static final String COMPANY_APA = "APA";
    public static final String COMPANY_ABSOLUT = "Absolut";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 100)
    private String company;

    @Column(nullable = false, length = 50)
    private String status = STATUS_AVAILABLE;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DriverPhone> phones = new HashSet<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DriverAttachment> attachments = new HashSet<>();

    // Default constructor required by JPA.
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<DriverPhone> getPhones() {
        return phones;
    }

    public void setPhones(Set<DriverPhone> phones) {
        this.phones = phones;
    }

    // Returns the number of phone numbers for this driver.
    public int getPhoneCount() {
        return phones != null ? phones.size() : 0;
    }

    public Set<DriverAttachment> getAttachments() { return attachments; }
    public void setAttachments(Set<DriverAttachment> attachments) { this.attachments = attachments; }

    // Adds an attachment and sets its back-reference to this driver.
    public void addAttachment(DriverAttachment attachment) {
        attachments.add(attachment);
        attachment.setDriver(this);
    }

    // Removes an attachment and clears its back-reference.
    public void removeAttachment(DriverAttachment attachment) {
        attachments.remove(attachment);
        attachment.setDriver(null);
    }

    // Returns the driver's full name (used as the label in combo boxes).
    @Override
    public String toString() {
        return fullName;
    }
}
