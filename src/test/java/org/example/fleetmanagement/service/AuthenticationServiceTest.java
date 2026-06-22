package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService, covering hashed login and legacy plaintext upgrade.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, passwordEncoder);
    }

    private User userWithPassword(String storedPassword, User.UserRole role) {
        return new User(1L, "admin", storedPassword, role, "Jan Kowalski");
    }

    @Test
    void login_WithCorrectHashedPassword_ShouldSucceed() {
        User user = userWithPassword(passwordEncoder.encode("admin123"), User.UserRole.ADMINISTRATOR);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        boolean result = authenticationService.login("admin", "admin123");

        assertTrue(result);
        assertTrue(authenticationService.isLoggedIn());
        assertEquals(user, authenticationService.getCurrentUser());
    }

    @Test
    void login_WithWrongPassword_ShouldFail() {
        User user = userWithPassword(passwordEncoder.encode("admin123"), User.UserRole.ADMINISTRATOR);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        boolean result = authenticationService.login("admin", "wrong");

        assertFalse(result);
        assertFalse(authenticationService.isLoggedIn());
    }

    @Test
    void login_WithUnknownUser_ShouldFail() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        boolean result = authenticationService.login("ghost", "any");

        assertFalse(result);
        assertNull(authenticationService.getCurrentUser());
    }

    @Test
    void login_WithLegacyPlaintextPassword_ShouldSucceedAndUpgradeToHash() {
        User user = userWithPassword("admin123", User.UserRole.ADMINISTRATOR);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = authenticationService.login("admin", "admin123");

        assertTrue(result);
        // The plaintext password must have been replaced by a BCrypt hash and persisted.
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String upgraded = captor.getValue().getPassword();
        assertTrue(upgraded.startsWith("$2"));
        assertTrue(passwordEncoder.matches("admin123", upgraded));
    }

    @Test
    void logout_ShouldClearCurrentUser() {
        User user = userWithPassword(passwordEncoder.encode("admin123"), User.UserRole.ADMINISTRATOR);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        authenticationService.login("admin", "admin123");

        authenticationService.logout();

        assertFalse(authenticationService.isLoggedIn());
        assertNull(authenticationService.getCurrentUser());
    }

    @Test
    void isAdmin_ShouldBeTrueForAdministratorAndFalseForLogistician() {
        User admin = userWithPassword(passwordEncoder.encode("p"), User.UserRole.ADMINISTRATOR);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        authenticationService.login("admin", "p");
        assertTrue(authenticationService.isAdmin());

        authenticationService.logout();
        assertFalse(authenticationService.isAdmin());

        User logistician = new User(2L, "logistyk", passwordEncoder.encode("p"),
                User.UserRole.LOGISTICIAN, "Anna Nowak");
        when(userRepository.findByUsername("logistyk")).thenReturn(Optional.of(logistician));
        authenticationService.login("logistyk", "p");
        assertFalse(authenticationService.isAdmin());
    }

    @Test
    void isLoggedIn_ShouldBeFalseInitially() {
        assertFalse(authenticationService.isLoggedIn());
    }
}
