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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService, focused on password hashing behaviour.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    // A real encoder is used so hashing/verification behaves exactly as in production.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
        testUser = new User(null, "jan.kowalski", "secret123",
                User.UserRole.LOGISTICIAN, "Jan Kowalski");
    }

    @Test
    void addUser_WhenUsernameIsUnique_ShouldHashPasswordAndSave() {
        when(userRepository.existsByUsername("jan.kowalski")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.addUser(testUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String storedPassword = captor.getValue().getPassword();

        assertNotEquals("secret123", storedPassword, "Password must not be stored in plaintext");
        assertTrue(storedPassword.startsWith("$2"), "Password must be a BCrypt hash");
        assertTrue(passwordEncoder.matches("secret123", storedPassword), "Hash must verify the original password");
    }

    @Test
    void addUser_WhenUsernameExists_ShouldThrowAndNotSave() {
        when(userRepository.existsByUsername("jan.kowalski")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.addUser(testUser));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WhenRawPasswordProvided_ShouldHashIt() {
        testUser.setPassword("newRawPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(testUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(passwordEncoder.matches("newRawPassword", captor.getValue().getPassword()));
    }

    @Test
    void updateUser_WhenPasswordAlreadyHashed_ShouldNotRehash() {
        String existingHash = passwordEncoder.encode("alreadyHashed");
        testUser.setPassword(existingHash);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(testUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(existingHash, captor.getValue().getPassword(), "Existing hash must remain unchanged");
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WhenUserDoesNotExist_ShouldThrow() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

        List<User> users = userService.getAllUsers();

        assertEquals(1, users.size());
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_ShouldDelegateToRepository() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserById(1L);

        assertTrue(result.isPresent());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByUsername_ShouldDelegateToRepository() {
        when(userRepository.findByUsername("jan.kowalski")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserByUsername("jan.kowalski");

        assertTrue(result.isPresent());
        verify(userRepository).findByUsername("jan.kowalski");
    }

    @Test
    void existsByUsername_ShouldDelegateToRepository() {
        when(userRepository.existsByUsername("jan.kowalski")).thenReturn(true);

        assertTrue(userService.existsByUsername("jan.kowalski"));
        verify(userRepository).existsByUsername("jan.kowalski");
    }
}
