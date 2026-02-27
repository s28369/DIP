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
 * Сервис для операций с документами водителей
 */
@Service
@Transactional
public class DriverDocumentService {

    private final DriverDocumentRepository driverDocumentRepository;

    @Autowired
    public DriverDocumentService(DriverDocumentRepository driverDocumentRepository) {
        this.driverDocumentRepository = driverDocumentRepository;
    }

    public List<DriverDocument> getAllDocuments() {
        return driverDocumentRepository.findAll();
    }

    public Optional<DriverDocument> getDocumentById(Long id) {
        return driverDocumentRepository.findById(id);
    }

    public List<DriverDocument> getDocumentsByDriver(Driver driver) {
        return driverDocumentRepository.findByDriver(driver);
    }

    public DriverDocument addDocument(DriverDocument document) {
        return driverDocumentRepository.save(document);
    }

    public DriverDocument updateDocument(DriverDocument document) {
        return driverDocumentRepository.save(document);
    }

    public void deleteDocument(Long id) {
        driverDocumentRepository.deleteById(id);
    }

    public List<DriverDocument> getExpiringDocuments() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return driverDocumentRepository.findExpiringDocuments(now, thirtyDaysLater);
    }

    public List<DriverDocument> getExpiredDocuments() {
        return driverDocumentRepository.findByExpiryDateBefore(LocalDate.now());
    }
}
