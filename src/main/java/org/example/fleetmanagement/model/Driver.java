package org.example.fleetmanagement.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "driver")
public class Driver {

    public static final String STATUS_AVAILABLE = "Доступен";
    public static final String STATUS_ON_TRIP = "в рейсе";
    public static final String STATUS_MAINTENANCE = "на ремонте";

    public static final String COMPANY_MTG = "МТГ";
    public static final String COMPANY_APA = "АПА";
    public static final String COMPANY_ABSOLUT = "Абсолют";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 100)
    private String company;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false, length = 50)
    private String status = STATUS_AVAILABLE;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DriverPhone> phones = new HashSet<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DriverAttachment> attachments = new HashSet<>();

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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /** Возвращает true, если сегодня день рождения водителя */
    public boolean isBirthdayToday() {
        if (birthDate == null) return false;
        LocalDate today = LocalDate.now();
        return birthDate.getMonth() == today.getMonth() && birthDate.getDayOfMonth() == today.getDayOfMonth();
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

    public int getPhoneCount() {
        return phones != null ? phones.size() : 0;
    }

    public Set<DriverAttachment> getAttachments() { return attachments; }
    public void setAttachments(Set<DriverAttachment> attachments) { this.attachments = attachments; }

    public void addAttachment(DriverAttachment attachment) {
        attachments.add(attachment);
        attachment.setDriver(this);
    }

    public void removeAttachment(DriverAttachment attachment) {
        attachments.remove(attachment);
        attachment.setDriver(null);
    }

    @Override
    public String toString() {
        return fullName;
    }
}
