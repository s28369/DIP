package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Driver;
import org.example.fleetmanagement.model.DriverDocument;
import org.example.fleetmanagement.repository.DriverDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for driver document operations (including expiry checks).
 */
@Service
@Transactional
public class DriverDocumentService {

    private final DriverDocumentRepository driverDocumentRepository;

    // Constructor injection of the driver document repository.
    @Autowired
    public DriverDocumentService(DriverDocumentRepository driverDocumentRepository) {
        this.driverDocumentRepository = driverDocumentRepository;
    }

    // Returns all driver documents.
    public List<DriverDocument> getAllDocuments() {
        return driverDocumentRepository.findAll();
    }

    // Finds a single driver document by id.
    public Optional<DriverDocument> getDocumentById(Long id) {
        return driverDocumentRepository.findById(id);
    }

    // Returns all documents assigned to a given driver.
    public List<DriverDocument> getDocumentsByDriver(Driver driver) {
        return driverDocumentRepository.findByDriver(driver);
    }

    // Saves a new driver document.
    public DriverDocument addDocument(DriverDocument document) {
        return driverDocumentRepository.save(document);
    }

    // Persists changes to an existing driver document.
    public DriverDocument updateDocument(DriverDocument document) {
        return driverDocumentRepository.save(document);
    }

    // Deletes a driver document by id.
    public void deleteDocument(Long id) {
        driverDocumentRepository.deleteById(id);
    }

    // Returns driver documents expiring within the next 30 days.
    public List<DriverDocument> getExpiringDocuments() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return driverDocumentRepository.findExpiringDocuments(now, thirtyDaysLater);
    }

    // Returns driver documents that have already expired.
    public List<DriverDocument> getExpiredDocuments() {
        return driverDocumentRepository.findByExpiryDateBefore(LocalDate.now());
    }
}
