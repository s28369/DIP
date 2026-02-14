package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serwis obsługujący operacje na użytkownikach
 */
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Zwraca wszystkich użytkowników
     * @return lista wszystkich użytkowników
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Wyszukuje użytkownika po ID
     * @param id identyfikator użytkownika
     * @return Optional z użytkownikiem jeśli istnieje
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Wyszukuje użytkownika po nazwie
     * @param username nazwa użytkownika
     * @return Optional z użytkownikiem jeśli istnieje
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Dodaje nowego użytkownika do systemu
     * @param user użytkownik do dodania
     * @return zapisany użytkownik
     * @throws IllegalArgumentException jeśli użytkownik o podanej nazwie już istnieje
     */
    public User addUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Użytkownik o nazwie " 
                + user.getUsername() + " już istnieje w systemie");
        }
        return userRepository.save(user);
    }
    
    /**
     * Aktualizuje dane użytkownika
     * @param user użytkownik z zaktualizowanymi danymi
     * @return zaktualizowany użytkownik
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Usuwa użytkownika z systemu
     * @param id identyfikator użytkownika do usunięcia
     * @throws IllegalArgumentException jeśli użytkownik nie istnieje
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Użytkownik o ID " + id + " nie istnieje");
        }
        userRepository.deleteById(id);
    }
    
    /**
     * Sprawdza czy użytkownik o podanej nazwie istnieje
     * @param username nazwa użytkownika
     * @return true jeśli istnieje
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
