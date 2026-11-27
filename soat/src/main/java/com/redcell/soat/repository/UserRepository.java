package com.redcell.soat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.redcell.soat.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailOrUsername(String email, String username);
    Optional<User> findByResetToken(String resetToken);
}