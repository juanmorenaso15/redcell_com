package com.redcell.soat.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redcell.soat.repository.PolizaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimpiezaPolizasScheduler {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final PolizaRepository polizaRepository;

    @Scheduled(cron = "${soat.limpieza.cron:0 59 23 * * SUN}", zone = "America/Bogota")
    @Transactional
    public void limpiarPolizasInactivasProgramado() {
        limpiarPolizasInactivas();
    }

    @Transactional
    public Map<String, Object> limpiarPolizasInactivas() {
        LocalDateTime ejecucion = LocalDateTime.now(ZONA_COLOMBIA);
        int polizasEliminadas = polizaRepository.deletePolizasInactivas();

        log.info("Limpieza de tabla polizas (activo=false) ejecutada ({}) -> registros eliminados: {}",
                ejecucion, polizasEliminadas);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("fechaEjecucion", ejecucion);
        resumen.put("polizasEliminadas", polizasEliminadas);
        return resumen;
    }
}