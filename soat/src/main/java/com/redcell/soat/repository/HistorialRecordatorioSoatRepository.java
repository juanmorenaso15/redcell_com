package com.redcell.soat.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.redcell.soat.entity.HistorialRecordatorioSoat;

public interface HistorialRecordatorioSoatRepository extends JpaRepository<HistorialRecordatorioSoat, Long> {

    Optional<HistorialRecordatorioSoat> findByPolizaIdAndTipoRecordatorioAndFechaVencimiento(
            Long polizaId,
            String tipoRecordatorio,
            LocalDate fechaVencimiento
    );
}