package com.redcell.soat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table(name = "users")
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String rol;
    
    private Boolean estado = true;

    private Integer failedAttempts = 0;
    private LocalDateTime lockTime;

    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private String resetToken;
    
    private LocalDateTime resetTokenExpiration;
}