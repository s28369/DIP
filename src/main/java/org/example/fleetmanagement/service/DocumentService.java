package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Document;
import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for truck document operations (including expiry checks).
 */
@Service
@Transactional
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    
    // Constructor injection of the document repository.
    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }
    
    // Returns all documents.
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    // Finds a single document by id.
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    // Returns all documents assigned to a given truck.
    public List<Document> getDocumentsByTruck(Truck truck) {
        return documentRepository.findByTruck(truck);
    }
    
    // Saves a new document.
    public Document addDocument(Document document) {
        return documentRepository.save(document);
    }
    
    // Persists changes to an existing document.
    public Document updateDocument(Document document) {
        return documentRepository.save(document);
    }
    
    // Deletes a document by id.
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
    
    // Returns documents expiring within the next 30 days.
    public List<Document> getExpiringDocuments() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return documentRepository.findExpiringDocuments(now, thirtyDaysLater);
    }
    
    // Returns documents that have already expired.
    public List<Document> getExpiredDocuments() {
        return documentRepository.findByExpiryDateBefore(LocalDate.now());
    }
}
