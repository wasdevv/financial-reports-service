package com.financialreports.repository;

import com.financialreports.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.email) = lower(?1)")
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u where lower(u.username) = lower(?1)")
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);
}
