package com.redcell.soat.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertaRecordatorioDTO {
    private Long polizaId;
    private String placa;
    private String propietario;
    private String telefono;
    private String email;
    private LocalDate fechaVencimiento;
    private String tipoRecordatorio;
    private String etiqueta;
    private LocalDate fechaProgramada;
    private long diasTranscurridos;
    private boolean enviadoEmail;
    private boolean enviadoWhatsapp;
    private String mensajeAlerta;
    private String whatsappUrl;
}