package com.redcell.soat.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redcell.soat.dto.AlertaRecordatorioDTO;
import com.redcell.soat.entity.HistorialRecordatorioSoat;
import com.redcell.soat.entity.Poliza;
import com.redcell.soat.repository.HistorialRecordatorioSoatRepository;
import com.redcell.soat.repository.PolizaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertaRecordatorioService {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");
    private static final List<String> ORDEN_ETAPAS = List.of("MES_1", "DIAS_5", "DIA_1", "HOY");

    private final PolizaRepository polizaRepository;
    private final HistorialRecordatorioSoatRepository historialRepository;
    private final RecordatorioSoatScheduler recordatorioSoatScheduler;
    private final NotificacionSoatService notificacionSoatService;

    public List<AlertaRecordatorioDTO> obtenerAlertasPendientes(LocalDate fechaReferencia) {
        LocalDate hoy = (fechaReferencia != null) ? fechaReferencia : LocalDate.now(ZONA_COLOMBIA);
        List<Poliza> polizasActivas = polizaRepository.findByActivoTrue();
        List<AlertaRecordatorioDTO> alertas = new ArrayList<>();

        for (Poliza poliza : polizasActivas) {
            if (poliza.getHasta() == null) {
                continue;
            }

            // Evita mostrar alertas de pólizas vencidas hace más de 15 días
            if (hoy.isAfter(poliza.getHasta().plusDays(15))) {
                continue;
            }

            for (String tipo : ORDEN_ETAPAS) {
                LocalDate fechaProgramada = recordatorioSoatScheduler.fechaProgramadaEtapa(tipo, poliza.getHasta());
                if (fechaProgramada == null || hoy.isBefore(fechaProgramada)) {
                    continue;
                }

                Optional<HistorialRecordatorioSoat> histOpt = historialRepository
                        .findByPolizaIdAndTipoRecordatorioAndFechaVencimiento(
                                poliza.getId(),
                                tipo,
                                poliza.getHasta()
                        );

                boolean enviadoEmail = histOpt.map(h -> Boolean.TRUE.equals(h.getEnviadoEmail())).orElse(false);
                boolean enviadoWhatsapp = histOpt.map(h -> Boolean.TRUE.equals(h.getEnviadoWhatsapp())).orElse(false);

                // Si el WhatsApp ya fue enviado, no mostramos alerta pendiente para esta etapa
                if (enviadoWhatsapp) {
                    continue;
                }

                long diasTranscurridos = Math.max(0, ChronoUnit.DAYS.between(fechaProgramada, hoy));
                String etiqueta = recordatorioSoatScheduler.etiquetaParaMensaje(tipo);
                String mensajeAlerta = construirTextoAlerta(etiqueta, diasTranscurridos, enviadoEmail);
                String whatsappUrl = notificacionSoatService.construirUrlWhatsapp(poliza, etiqueta);

                alertas.add(AlertaRecordatorioDTO.builder()
                        .polizaId(poliza.getId())
                        .placa(poliza.getPlaca())
                        .propietario(poliza.getPropietario())
                        .telefono(poliza.getTelefono())
                        .email(poliza.getEmail())
                        .fechaVencimiento(poliza.getHasta())
                        .tipoRecordatorio(tipo)
                        .etiqueta(etiqueta)
                        .fechaProgramada(fechaProgramada)
                        .diasTranscurridos(diasTranscurridos)
                        .enviadoEmail(enviadoEmail)
                        .enviadoWhatsapp(false)
                        .mensajeAlerta(mensajeAlerta)
                        .whatsappUrl(whatsappUrl)
                        .build());
            }
        }

        alertas.sort(Comparator
                .comparingLong(AlertaRecordatorioDTO::getDiasTranscurridos).reversed()
                .thenComparing(AlertaRecordatorioDTO::getFechaVencimiento));

        return alertas;
    }

    @Transactional
    public HistorialRecordatorioSoat marcarWhatsappEnviado(Long polizaId, String tipoRecordatorio) {
        if (tipoRecordatorio == null || tipoRecordatorio.isBlank()) {
            throw new RuntimeException("El parámetro 'tipo' es obligatorio (MES_1, DIAS_5, DIA_1, HOY)");
        }

        String tipoNormalizado = tipoRecordatorio.trim().toUpperCase();
        if (!ORDEN_ETAPAS.contains(tipoNormalizado)) {
            throw new RuntimeException("Tipo de recordatorio inválido: " + tipoNormalizado);
        }

        Poliza poliza = polizaRepository.findById(polizaId)
                .orElseThrow(() -> new RuntimeException("Póliza no encontrada con el ID proporcionado."));

        if (poliza.getHasta() == null) {
            throw new RuntimeException("La póliza no tiene fecha de vencimiento ('hasta') registrada.");
        }

        HistorialRecordatorioSoat historial = historialRepository
                .findByPolizaIdAndTipoRecordatorioAndFechaVencimiento(
                        poliza.getId(),
                        tipoNormalizado,
                        poliza.getHasta()
                )
                .orElseGet(() -> {
                    HistorialRecordatorioSoat nuevo = new HistorialRecordatorioSoat();
                    nuevo.setPolizaId(poliza.getId());
                    nuevo.setPlaca(poliza.getPlaca());
                    nuevo.setTipoRecordatorio(tipoNormalizado);
                    nuevo.setFechaVencimiento(poliza.getHasta());
                    nuevo.setEnviadoEmail(false);
                    return nuevo;
                });

        historial.setEnviadoWhatsapp(true);
        historial.setEnviadoEn(LocalDateTime.now(ZONA_COLOMBIA));
        return historialRepository.save(historial);
    }

    private String construirTextoAlerta(String etiqueta, long diasTranscurridos, boolean enviadoEmail) {
        String estadoEmail = enviadoEmail ? "Email enviado" : "Email aún no enviado";
        if (diasTranscurridos == 0) {
            return "Hoy toca enviar (" + etiqueta + ") y no has enviado el mensaje de WhatsApp (" + estadoEmail + ").";
        }
        String unidad = diasTranscurridos == 1 ? "día" : "días";
        return "Pasaron " + diasTranscurridos + " " + unidad + " (" + etiqueta + ") y no has enviado el mensaje de WhatsApp (" + estadoEmail + ").";
    }
}