package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для операций с пользователями
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
     * Возвращает всех пользователей
     * @return список всех пользователей
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Ищет пользователя по ID
     * @param id идентификатор пользователя
     * @return Optional с пользователем, если существует
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Ищет пользователя по имени
     * @param username имя пользователя
     * @return Optional с пользователем, если существует
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Добавляет нового пользователя в систему
     * @param user пользователь для добавления
     * @return сохранённый пользователь
     * @throws IllegalArgumentException если пользователь с указанным именем уже существует
     */
    public User addUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Пользователь с именем " 
                + user.getUsername() + " уже существует в системе");
        }
        return userRepository.save(user);
    }
    
    /**
     * Обновляет данные пользователя
     * @param user пользователь с обновлёнными данными
     * @return обновлённый пользователь
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Удаляет пользователя из системы
     * @param id идентификатор пользователя для удаления
     * @throws IllegalArgumentException если пользователь не существует
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Пользователь с ID " + id + " не существует");
        }
        userRepository.deleteById(id);
    }
    
    /**
     * Проверяет, существует ли пользователь с указанным именем
     * @param username имя пользователя
     * @return true, если существует
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
