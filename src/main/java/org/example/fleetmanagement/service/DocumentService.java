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
 * Serwis obsługujący operacje na dokumentach
 */
@Service
@Transactional
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    
    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }
    
    /**
     * Zwraca wszystkie dokumenty
     * @return lista wszystkich dokumentów
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    /**
     * Wyszukuje dokument po ID
     * @param id identyfikator dokumentu
     * @return Optional z dokumentem jeśli istnieje
     */
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    /**
     * Zwraca wszystkie dokumenty przypisane do ciężarówki
     * @param truck ciężarówka
     * @return lista dokumentów
     */
    public List<Document> getDocumentsByTruck(Truck truck) {
        return documentRepository.findByTruck(truck);
    }
    
    /**
     * Dodaje nowy dokument do systemu
     * @param document dokument do dodania
     * @return zapisany dokument
     */
    public Document addDocument(Document document) {
        return documentRepository.save(document);
    }
    
    /**
     * Aktualizuje dane dokumentu
     * @param document dokument z zaktualizowanymi danymi
     * @return zaktualizowany dokument
     */
    public Document updateDocument(Document document) {
        return documentRepository.save(document);
    }
    
    /**
     * Usuwa dokument z systemu
     * @param id identyfikator dokumentu do usunięcia
     */
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
    
    /**
     * Zwraca dokumenty wygasające w ciągu najbliższych 30 dni
     * @return lista wygasających dokumentów
     */
    public List<Document> getExpiringDocuments() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return documentRepository.findExpiringDocuments(now, thirtyDaysLater);
    }
    
    /**
     * Zwraca przeterminowane dokumenty
     * @return lista przeterminowanych dokumentów
     */
    public List<Document> getExpiredDocuments() {
        return documentRepository.findByExpiryDateBefore(LocalDate.now());
    }
}
