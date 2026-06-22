package org.example.fleetmanagement.config;

import org.example.fleetmanagement.model.User;
import org.example.fleetmanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicjalizator danych testowych.
 * Działa z dowolną bazą danych (H2, MySQL, PostgreSQL).
 * Dodaje użytkowników testowych tylko jeśli tabela jest pusta.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection of the user repository and the password encoder.
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Seeds two default users with BCrypt-hashed passwords only when the user table is empty.
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User(null, "admin", passwordEncoder.encode("admin123"),
                    User.UserRole.ADMINISTRATOR, "Jan Kowalski"));
            userRepository.save(new User(null, "logistyk", passwordEncoder.encode("logistyk123"),
                    User.UserRole.LOGISTICIAN, "Anna Nowak"));
            System.out.println("[DataInitializer] Utworzono użytkowników testowych (admin / logistyk).");
        }
    }
}
