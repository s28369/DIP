package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for user operations, responsible for storing passwords as BCrypt hashes.
 */
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Constructor injection of the user repository and the BCrypt password encoder.
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Returns true if the given value already looks like a BCrypt hash (so we never hash it twice).
    private boolean isBCryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
    
    // Returns all users.
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // Finds a single user by id.
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // Finds a single user by username.
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // Adds a new user with a hashed password; rejects duplicate usernames.
    public User addUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Użytkownik o nazwie " 
                + user.getUsername() + " już istnieje w systemie");
        }
        // Always store a hashed password, never the raw one entered by the admin.
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    // Updates a user; hashes the password only when a new raw one was supplied.
    public User updateUser(User user) {
        // Only hash when a new raw password was provided; an unchanged value is already a BCrypt hash.
        if (!isBCryptHash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }
    
    // Deletes a user by id, failing if it does not exist.
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Użytkownik o ID " + id + " nie istnieje");
        }
        userRepository.deleteById(id);
    }
    
    // Returns true if a user with the given username already exists.
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
