package org.example.fleetmanagement.repository;

import org.example.fleetmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для операций с сущностью User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Ищет пользователя по имени пользователя
     * @param username имя пользователя
     * @return Optional с пользователем, если существует
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Проверяет, существует ли пользователь с указанным именем
     * @param username имя пользователя
     * @return true, если существует, false в противном случае
     */
    boolean existsByUsername(String username);
}
