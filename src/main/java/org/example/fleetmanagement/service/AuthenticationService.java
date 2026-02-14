package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Serwis obsługujący logowanie użytkowników
 */
@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private User currentUser;
    
    @Autowired
    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Loguje użytkownika do systemu
     * @param username nazwa użytkownika
     * @param password hasło
     * @return true jeśli logowanie się powiodło, false w przeciwnym wypadku
     */
    public boolean login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) {
                currentUser = user;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Wylogowuje aktualnie zalogowanego użytkownika
     */
    public void logout() {
        currentUser = null;
    }
    
    /**
     * Zwraca aktualnie zalogowanego użytkownika
     * @return zalogowany użytkownik lub null jeśli nikt nie jest zalogowany
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Sprawdza czy użytkownik jest zalogowany
     * @return true jeśli użytkownik jest zalogowany
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Sprawdza czy zalogowany użytkownik ma rolę administratora
     * @return true jeśli użytkownik jest administratorem
     */
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.UserRole.ADMINISTRATOR;
    }
}
