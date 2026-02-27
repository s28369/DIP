package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Truck;
import org.example.fleetmanagement.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для операций с грузовиками
 */
@Service
@Transactional
public class TruckService {
    
    private final TruckRepository truckRepository;
    
    @Autowired
    public TruckService(TruckRepository truckRepository) {
        this.truckRepository = truckRepository;
    }
    
    /**
     * Возвращает все грузовики
     * @return список всех грузовиков
     */
    public List<Truck> getAllTrucks() {
        return truckRepository.findAll();
    }
    
    /**
     * Ищет грузовик по ID
     * @param id идентификатор грузовика
     * @return Optional с грузовиком, если существует
     */
    public Optional<Truck> getTruckById(Long id) {
        return truckRepository.findById(id);
    }
    
    /**
     * Добавляет новый грузовик в систему
     * @param truck грузовик для добавления
     * @return сохранённый грузовик
     * @throws IllegalArgumentException если грузовик с указанным регистрационным номером уже существует
     */
    public Truck addTruck(Truck truck) {
        if (truckRepository.existsByRegistrationNumber(truck.getRegistrationNumber())) {
            throw new IllegalArgumentException("Грузовик с регистрационным номером " 
                + truck.getRegistrationNumber() + " уже существует в системе");
        }
        return truckRepository.save(truck);
    }
    
    /**
     * Обновляет данные грузовика
     * @param truck грузовик с обновлёнными данными
     * @return обновлённый грузовик
     */
    public Truck updateTruck(Truck truck) {
        return truckRepository.save(truck);
    }
    
    /**
     * Удаляет грузовик из системы
     * @param id идентификатор грузовика для удаления
     * @throws IllegalArgumentException если грузовик не существует
     */
    public void deleteTruck(Long id) {
        if (!truckRepository.existsById(id)) {
            throw new IllegalArgumentException("Грузовик с ID " + id + " не существует");
        }
        truckRepository.deleteById(id);
    }
    
    /**
     * Ищет грузовики по статусу
     * @param status статус грузовика
     * @return список грузовиков с указанным статусом
     */
    public List<Truck> getTrucksByStatus(String status) {
        return truckRepository.findByStatus(status);
    }
}
