package com.redcell.soat.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "polizas")
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_vehiculo")
    private String tipoVehiculo;

    @Column(name = "placa", nullable = false)
    private String placa;

    @Column(name = "propietario")
    private String propietario;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "documento")
    private Long documento;

    @Column(name = "desde")
    private LocalDate desde;

    @Column(name = "hasta")
    private LocalDate hasta;

    @Column(name = "marca_linea")
    private String marcaLinea;

    @Column(name = "cilindraje")
    private Integer cilindraje;

    @Column(name = "aseguradora")
    private String aseguradora;

    // --- NUEVOS CAMPOS PARA CÁLCULO DE TARIFAS ---
    
    @Column(name = "edad_vehiculo")
    private Integer edadVehiculo;

    @Column(name = "capacidad_toneladas", precision = 10, scale = 2)
    private BigDecimal capacidadToneladas;

    @Column(name = "precio_soat", precision = 10, scale = 2)
    private BigDecimal precioSoat;

    @Column(name = "uso_plataforma", precision = 10, scale = 2)
    private BigDecimal usoPlataforma;

    @Column(name = "total_pagar", precision = 10, scale = 2)
    private BigDecimal totalPagar;

    // ---------------------------------------------

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

        // --- NUEVO CAMPO ---
    @Column(name = "activo")
    private Boolean activo = true;
    // -------------------

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;
}