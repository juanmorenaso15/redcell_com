package com.redcell.soat.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.redcell.soat.entity.HistorialRecordatorioSoat;
import com.redcell.soat.entity.Poliza;
import com.redcell.soat.repository.HistorialRecordatorioSoatRepository;
import com.redcell.soat.repository.PolizaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordatorioSoatScheduler {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final PolizaRepository polizaRepository;
    private final HistorialRecordatorioSoatRepository historialRepository;
    private final NotificacionSoatService notificacionSoatService;

    @Scheduled(cron = "${soat.recordatorios.cron:0 0 8 * * *}", zone = "America/Bogota")
    public void ejecutarRecordatoriosProgramados() {
        LocalDate hoy = LocalDate.now(ZONA_COLOMBIA);
        ejecutarRecordatorios(hoy, null, false);
    }

    public Map<String, Object> ejecutarRecordatorios(
            LocalDate fechaReferencia,
            String tipoForzado,
            boolean ignorarHistorial) {

        LocalDate fechaBase = (fechaReferencia != null) ? fechaReferencia : LocalDate.now(ZONA_COLOMBIA);
        String tipoNormalizado = (tipoForzado != null && !tipoForzado.isBlank())
                ? tipoForzado.trim().toUpperCase()
                : null;

        log.info("Evaluando recordatorios SOAT (email automático) -> fechaBase={}, tipoForzado={}, ignorarHistorial={}",
                fechaBase, tipoNormalizado, ignorarHistorial);

        List<Poliza> polizasActivas = polizaRepository.findByActivoTrue();
        List<Map<String, Object>> detalles = new ArrayList<>();

        int encolados = 0;
        int omitidos = 0;

        for (Poliza poliza : polizasActivas) {
            if (poliza.getHasta() == null) {
                continue;
            }

            String tipoRecordatorio = (tipoNormalizado != null)
                    ? tipoNormalizado
                    : resolverEtapaRecordatorio(fechaBase, poliza.getHasta());

            if (tipoRecordatorio == null) {
                continue;
            }

            HistorialRecordatorioSoat historial = historialRepository
                    .findByPolizaIdAndTipoRecordatorioAndFechaVencimiento(
                            poliza.getId(),
                            tipoRecordatorio,
                            poliza.getHasta()
                    )
                    .orElseGet(() -> {
                        HistorialRecordatorioSoat nuevo = new HistorialRecordatorioSoat();
                        nuevo.setPolizaId(poliza.getId());
                        nuevo.setPlaca(poliza.getPlaca());
                        nuevo.setTipoRecordatorio(tipoRecordatorio);
                        nuevo.setFechaVencimiento(poliza.getHasta());
                        nuevo.setEnviadoWhatsapp(false);
                        nuevo.setEnviadoEmail(false);
                        return historialRepository.save(nuevo);
                    });

            boolean emailYaEnviado = Boolean.TRUE.equals(historial.getEnviadoEmail());

            if (!ignorarHistorial && emailYaEnviado) {
                omitidos++;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("polizaId", poliza.getId());
                item.put("placa", poliza.getPlaca());
                item.put("tipoRecordatorio", tipoRecordatorio);
                item.put("enviadoEmail", true);
                item.put("enviadoWhatsapp", Boolean.TRUE.equals(historial.getEnviadoWhatsapp()));
                item.put("estado", "OMITIDO_EMAIL_YA_ENVIADO");
                detalles.add(item);
                continue;
            }

            String etiquetaLegible = etiquetaParaMensaje(tipoRecordatorio);
            notificacionSoatService.encolarNotificacionPoliza(
                    poliza.getId(),
                    null,
                    null,
                    false,
                    true,
                    etiquetaLegible,
                    tipoRecordatorio,
                    poliza.getHasta()
            );

            encolados++;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("polizaId", poliza.getId());
            item.put("placa", poliza.getPlaca());
            item.put("tipoRecordatorio", tipoRecordatorio);
            item.put("estado", "ENCOLADO_EMAIL");
            detalles.add(item);
        }

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("fechaEvaluada", fechaBase);
        resumen.put("tipoForzado", tipoNormalizado);
        resumen.put("ignorarHistorial", ignorarHistorial);
        resumen.put("totalEvaluadas", detalles.size());
        resumen.put("encolados", encolados);
        resumen.put("omitidos", omitidos);
        resumen.put("detalles", detalles);
        return resumen;
    }

    public String resolverEtapaRecordatorio(LocalDate hoy, LocalDate vence) {
        if (hoy.equals(vence.minusMonths(1))) {
            return "MES_1";
        }
        if (hoy.equals(vence.minusDays(5))) {
            return "DIAS_5";
        }
        if (hoy.equals(vence.minusDays(1))) {
            return "DIA_1";
        }
        if (hoy.equals(vence)) {
            return "HOY";
        }
        return null;
    }

    public LocalDate fechaProgramadaEtapa(String tipoRecordatorio, LocalDate vence) {
        if (tipoRecordatorio == null || vence == null) {
            return null;
        }
        return switch (tipoRecordatorio.toUpperCase()) {
            case "MES_1" -> vence.minusMonths(1);
            case "DIAS_5" -> vence.minusDays(5);
            case "DIA_1" -> vence.minusDays(1);
            case "HOY" -> vence;
            default -> null;
        };
    }

    public String etiquetaParaMensaje(String tipoRecordatorio) {
        if (tipoRecordatorio == null) {
            return "";
        }
        return switch (tipoRecordatorio.toUpperCase()) {
            case "MES_1" -> "Falta 1 mes para el vencimiento";
            case "DIAS_5" -> "Faltan 5 días para el vencimiento";
            case "DIA_1" -> "Falta 1 día para el vencimiento";
            case "HOY" -> "Vence hoy";
            default -> "";
        };
    }
}