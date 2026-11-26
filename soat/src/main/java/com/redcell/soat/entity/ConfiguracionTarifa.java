package com.redcell.soat.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "configuracion_tarifas")
public class ConfiguracionTarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre para identificar la tarifa (Ej: "Uso Plataforma 2026")
    @Column(nullable = false)
    private String nombre;

    // El valor monetario (Ej: 15000.00)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    // Si está activa, se usará por defecto en los SOATs nuevos
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}