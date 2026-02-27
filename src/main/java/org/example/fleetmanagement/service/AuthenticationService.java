package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис аутентификации пользователей
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
     * Выполняет вход пользователя в систему
     * @param username имя пользователя
     * @param password пароль
     * @return true при успешном входе, false в противном случае
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
     * Выполняет выход текущего пользователя
     */
    public void logout() {
        currentUser = null;
    }
    
    /**
     * Возвращает текущего авторизованного пользователя
     * @return авторизованный пользователь или null, если никто не авторизован
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     * @return true, если пользователь авторизован
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Проверяет, является ли текущий пользователь администратором
     * @return true, если пользователь администратор
     */
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.UserRole.ADMINISTRATOR;
    }
}
