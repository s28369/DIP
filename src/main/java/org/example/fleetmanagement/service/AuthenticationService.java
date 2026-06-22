package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service authenticating users and holding the currently logged-in user for the session.
 */
@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private User currentUser;
    
    // Constructor injection of the user repository and the BCrypt password encoder.
    @Autowired
    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    // Authenticates a user by username and raw password; returns true on success.
    public boolean login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String stored = user.getPassword();
            if (isBCryptHash(stored)) {
                // Normal path: verify the raw password against the stored BCrypt hash.
                if (passwordEncoder.matches(password, stored)) {
                    currentUser = user;
                    return true;
                }
            } else if (stored != null && stored.equals(password)) {
                // Legacy path: a plaintext password from old data - accept once, then upgrade to a hash.
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                currentUser = user;
                return true;
            }
        }
        return false;
    }

    // Returns true if the stored value is already a BCrypt hash rather than legacy plaintext.
    private boolean isBCryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
    
    // Clears the current session (logs the user out).
    public void logout() {
        currentUser = null;
    }
    
    // Returns the currently logged-in user, or null if nobody is logged in.
    public User getCurrentUser() {
        return currentUser;
    }
    
    // Returns true if a user is currently logged in.
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    // Returns true if the current user has the administrator role.
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.UserRole.ADMINISTRATOR;
    }
}
