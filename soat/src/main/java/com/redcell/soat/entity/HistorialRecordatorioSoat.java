package com.redcell.soat.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
    name = "historial_recordatorios_soat",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"poliza_id", "tipo_recordatorio", "fecha_vencimiento"})
    }
)
public class HistorialRecordatorioSoat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poliza_id", nullable = false)
    private Long polizaId;

    @Column(name = "placa", nullable = false)
    private String placa;

    // MES_1, DIAS_5, DIA_1, HOY
    @Column(name = "tipo_recordatorio", nullable = false)
    private String tipoRecordatorio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "enviado_whatsapp", nullable = false)
    private Boolean enviadoWhatsapp = false;

    @Column(name = "enviado_email", nullable = false)
    private Boolean enviadoEmail = false;

    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn = LocalDateTime.now();
}