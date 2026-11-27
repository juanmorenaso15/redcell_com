package com.redcell.soat.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PolizaUpdateDTO {
    private String tipoVehiculo;
    private String placa;
    private String propietario;
    private String tipoDocumento;
    private Long documento;
    private LocalDate desde;
    private LocalDate hasta;
    private String marcaLinea;
    private Integer cilindraje;
    private String aseguradora;
    private Integer edadVehiculo;
    private BigDecimal capacidadToneladas;
    private BigDecimal usoPlataforma;
    private String telefono;
    private String email;
}