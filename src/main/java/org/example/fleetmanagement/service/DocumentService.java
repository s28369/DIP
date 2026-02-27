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
 * Сервис для операций с документами
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
     * Возвращает все документы
     * @return список всех документов
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    /**
     * Ищет документ по ID
     * @param id идентификатор документа
     * @return Optional с документом, если существует
     */
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    /**
     * Возвращает все документы, привязанные к грузовику
     * @param truck грузовик
     * @return список документов
     */
    public List<Document> getDocumentsByTruck(Truck truck) {
        return documentRepository.findByTruck(truck);
    }
    
    /**
     * Добавляет новый документ в систему
     * @param document документ для добавления
     * @return сохранённый документ
     */
    public Document addDocument(Document document) {
        return documentRepository.save(document);
    }
    
    /**
     * Обновляет данные документа
     * @param document документ с обновлёнными данными
     * @return обновлённый документ
     */
    public Document updateDocument(Document document) {
        return documentRepository.save(document);
    }
    
    /**
     * Удаляет документ из системы
     * @param id идентификатор документа для удаления
     */
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
    
    /**
     * Возвращает документы, истекающие в течение ближайших 30 дней
     * @return список истекающих документов
     */
    public List<Document> getExpiringDocuments() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return documentRepository.findExpiringDocuments(now, thirtyDaysLater);
    }
    
    /**
     * Возвращает просроченные документы
     * @return список просроченных документов
     */
    public List<Document> getExpiredDocuments() {
        return documentRepository.findByExpiryDateBefore(LocalDate.now());
    }
}
