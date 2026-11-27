package com.redcell.soat.services;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.redcell.soat.entity.HistorialRecordatorioSoat;
import com.redcell.soat.entity.Poliza;
import com.redcell.soat.repository.HistorialRecordatorioSoatRepository;
import com.redcell.soat.repository.PolizaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacionSoatService {

    private final PolizaRepository polizaRepository;
    private final HistorialRecordatorioSoatRepository historialRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;
    private final EmailService emailService;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String EMOJI_SPARKLE = emoji(0x2728);
    private static final String EMOJI_HOURGLASS = emoji(0x23F3);
    private static final String EMOJI_USER = emoji(0x1F464);
    private static final String EMOJI_ID = emoji(0x1F194);
    private static final String EMOJI_POINT_RIGHT = emoji(0x1F449);
    private static final String EMOJI_CALENDAR = emoji(0x1F4C5);
    private static final String EMOJI_MONEY_BAG = emoji(0x1F4B0);
    private static final String EMOJI_GEAR = emoji(0x2699);
    private static final String EMOJI_DOLLAR = emoji(0x1F4B5);
    private static final String EMOJI_RED_CIRCLE = emoji(0x1F534);
    private static final String EMOJI_MAN_OFFICE = emoji(0x1F935);
    private static final String EMOJI_PHONE = emoji(0x1F4DE);
    private static final String EMOJI_PIN = emoji(0x1F4CD);
    private static final String EMOJI_CITY = emoji(0x1F3D9);

    private static String emoji(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    public Map<String, Object> notificarPoliza(
            Poliza poliza,
            String telefonoOverride,
            String emailOverride,
            boolean enviarWhatsapp,
            boolean enviarEmail) {
        return notificarPolizaConEtapa(poliza, telefonoOverride, emailOverride, enviarWhatsapp, enviarEmail, null);
    }

    public Map<String, Object> notificarPolizaConEtapa(
            Poliza poliza,
            String telefonoOverride,
            String emailOverride,
            boolean enviarWhatsapp,
            boolean enviarEmail,
            String etiquetaPlazo) {

        String telefono = (telefonoOverride != null && !telefonoOverride.isBlank())
                ? telefonoOverride
                : poliza.getTelefono();
        String email = (emailOverride != null && !emailOverride.isBlank())
                ? emailOverride
                : poliza.getEmail();

        String mensaje = construirMensajePoliza(poliza, etiquetaPlazo);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("polizaId", poliza.getId());
        resultado.put("placa", poliza.getPlaca());

        if (enviarWhatsapp) {
            boolean wsOk = whatsAppNotificationService.enviarMensaje(telefono, mensaje);
            resultado.put("whatsappEnviado", wsOk);
        }

        if (enviarEmail) {
            boolean mailOk = emailService.enviarRecordatorioSoat(email, poliza, mensaje);
            resultado.put("emailEnviado", mailOk);
        }

        return resultado;
    }

    public String construirUrlWhatsapp(Poliza poliza, String etiquetaPlazo) {
        if (poliza == null) {
            return null;
        }
        String telefonoNormalizado = whatsAppNotificationService.normalizarTelefono(poliza.getTelefono());
        if (telefonoNormalizado == null) {
            return null;
        }
        String mensaje = construirMensajePoliza(poliza, etiquetaPlazo);
        String encoded = URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://api.whatsapp.com/send?phone=" + telefonoNormalizado + "&text=" + encoded;
    }

    public String construirMensajePoliza(Poliza poliza) {
        return construirMensajePoliza(poliza, null);
    }

    public String construirMensajePoliza(Poliza poliza, String etiquetaPlazo) {
        BigDecimal totalPagar = poliza.getTotalPagar() != null ? poliza.getTotalPagar() : BigDecimal.ZERO;
        BigDecimal usoPlataforma = poliza.getUsoPlataforma() != null ? poliza.getUsoPlataforma() : BigDecimal.ZERO;
        BigDecimal valorSoat = totalPagar.subtract(usoPlataforma);
        if (valorSoat.compareTo(BigDecimal.ZERO) < 0) {
            valorSoat = BigDecimal.ZERO;
        }

        String propietario = valorSeguro(poliza.getPropietario(), "No registrado");
        String tipoDoc = valorSeguro(poliza.getTipoDocumento(), "");
        String numDoc = poliza.getDocumento() != null ? String.valueOf(poliza.getDocumento()) : "";
        String documentoCompleto = (tipoDoc + " " + numDoc).trim();
        if (documentoCompleto.isBlank()) {
            documentoCompleto = "No registrado";
        }

        String placa = valorSeguro(poliza.getPlaca(), "Sin placa");
        String vehiculoDetalle = construirDescripcionVehiculo(poliza);
        String venceEl = poliza.getHasta() != null ? poliza.getHasta().format(FORMATO_FECHA) : "Sin fecha";

        StringBuilder sb = new StringBuilder();
        sb.append(EMOJI_SPARKLE).append(" *RECORDATORIO DE SOAT* ").append(EMOJI_SPARKLE).append("\n\n");

        if (etiquetaPlazo != null && !etiquetaPlazo.isBlank()) {
            sb.append(EMOJI_HOURGLASS).append(" *Aviso:* ").append(etiquetaPlazo).append("\n\n");
        }

        sb.append("Muy buenos dias. Le informamos que el SOAT para el vehículo de:\n\n");
        sb.append(EMOJI_USER).append(" *PROPIETARIO:* ").append(propietario).append("\n");
        sb.append(EMOJI_ID).append(" *DOCUMENTO:* ").append(documentoCompleto).append("\n");
        sb.append(EMOJI_POINT_RIGHT).append(" *VEHÍCULO:* (").append(placa).append(") - ").append(vehiculoDetalle).append("\n\n");
        sb.append(EMOJI_CALENDAR).append(" *VENCE EL:* ").append(venceEl).append("\n");
        sb.append(EMOJI_MONEY_BAG).append(" *VALOR SOAT:* $").append(formatearMonedaExcel(valorSoat)).append("\n");
        sb.append(EMOJI_GEAR).append(" *USO PLATAFORMA:* $").append(formatearMonedaExcel(usoPlataforma)).append("\n");
        sb.append(EMOJI_DOLLAR).append(" *TOTAL A PAGAR:* $").append(formatearMonedaExcel(totalPagar)).append("\n\n");
        sb.append("--------------------------------------------\n\n");
        sb.append(EMOJI_RED_CIRCLE).append(" *Más info:* REDCELL.COM\n\n");
        sb.append(EMOJI_MAN_OFFICE).append(" *Atendido por:* Yhan Alejandro Moreno Gonzalez\n\n");
        sb.append(EMOJI_PHONE).append(" *WhatsApp:* 3117034142\n\n");
        sb.append(EMOJI_PIN).append(" *Ubicación:* Calle 23 No 15-35 (Por Ventanilla verde)\n\n");
        sb.append(EMOJI_CITY).append(" Armenia, Quindío");

        return sb.toString();
    }

    private String construirDescripcionVehiculo(Poliza poliza) {
        String marcaLinea = poliza.getMarcaLinea();
        String tipoVehiculo = poliza.getTipoVehiculo();

        if (marcaLinea != null && !marcaLinea.isBlank() && tipoVehiculo != null && !tipoVehiculo.isBlank()) {
            return marcaLinea.trim() + " (" + tipoVehiculo.trim() + ")";
        }
        if (marcaLinea != null && !marcaLinea.isBlank()) {
            return marcaLinea.trim();
        }
        if (tipoVehiculo != null && !tipoVehiculo.isBlank()) {
            return tipoVehiculo.trim();
        }
        return "Vehículo";
    }

    private String formatearMonedaExcel(BigDecimal valor) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        df.setMaximumFractionDigits(0);
        return df.format(valor);
    }

    private String valorSeguro(String valor, String porDefecto) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : porDefecto;
    }

    public static String obtenerFechaFormateada(LocalDate fecha) {
        return fecha != null ? fecha.format(FORMATO_FECHA) : "";
    }

    @Async("notificacionesExecutor")
    public void encolarNotificacionPoliza(
            Long polizaId,
            String telefonoOverride,
            String emailOverride,
            boolean enviarWhatsapp,
            boolean enviarEmail,
            String etiquetaPlazo,
            String tipoRecordatorio,
            LocalDate fechaVencimiento) {

        try {
            Poliza poliza = polizaRepository.findById(polizaId).orElse(null);
            if (poliza == null) {
                log.warn("No se encontró la póliza {} para notificación asíncrona", polizaId);
                return;
            }

            Map<String, Object> res = notificarPolizaConEtapa(
                    poliza,
                    telefonoOverride,
                    emailOverride,
                    enviarWhatsapp,
                    enviarEmail,
                    etiquetaPlazo
            );

            if (tipoRecordatorio != null && fechaVencimiento != null) {
                HistorialRecordatorioSoat historial = historialRepository
                        .findByPolizaIdAndTipoRecordatorioAndFechaVencimiento(
                                poliza.getId(),
                                tipoRecordatorio,
                                fechaVencimiento
                        )
                        .orElseGet(HistorialRecordatorioSoat::new);

                boolean wsActual = Boolean.TRUE.equals(historial.getEnviadoWhatsapp());
                boolean mailActual = Boolean.TRUE.equals(historial.getEnviadoEmail());

                boolean wsFinal = enviarWhatsapp
                        ? (Boolean.TRUE.equals(res.get("whatsappEnviado")) || wsActual)
                        : wsActual;
                boolean mailFinal = enviarEmail
                        ? (Boolean.TRUE.equals(res.get("emailEnviado")) || mailActual)
                        : mailActual;

                historial.setPolizaId(poliza.getId());
                historial.setPlaca(poliza.getPlaca());
                historial.setTipoRecordatorio(tipoRecordatorio);
                historial.setFechaVencimiento(fechaVencimiento);
                historial.setEnviadoWhatsapp(wsFinal);
                historial.setEnviadoEmail(mailFinal);
                historial.setEnviadoEn(LocalDateTime.now());
                historialRepository.save(historial);
            }
        } catch (Exception e) {
            log.error("Error procesando notificación asíncrona para póliza {}: {}", polizaId, e.getMessage(), e);
        }
    }
}