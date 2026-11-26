package com.redcell.soat.services;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.redcell.soat.entity.Poliza;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionSoatService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WhatsAppNotificationService whatsAppNotificationService;
    private final EmailService emailService;

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
        sb.append("✨ *RECORDATORIO DE SOAT* ✨\n\n");

        if (etiquetaPlazo != null && !etiquetaPlazo.isBlank()) {
            sb.append("⏳ *Aviso:* ").append(etiquetaPlazo).append("\n\n");
        }

        sb.append("Muy buenos dias. Le informamos que el SOAT para el vehículo de:\n\n");
        sb.append("👤 *PROPIETARIO:* ").append(propietario).append("\n");
        sb.append("🆔 *DOCUMENTO:* ").append(documentoCompleto).append("\n");
        sb.append("👉 *VEHÍCULO:* (").append(placa).append(") - ").append(vehiculoDetalle).append("\n\n");
        sb.append("🗓️ *VENCE EL:* ").append(venceEl).append("\n");
        sb.append("💰 *VALOR SOAT:* $").append(formatearMonedaExcel(valorSoat)).append("\n");
        sb.append("⚙️ *USO PLATAFORMA:* $").append(formatearMonedaExcel(usoPlataforma)).append("\n");
        sb.append("💵 *TOTAL A PAGAR:* $").append(formatearMonedaExcel(totalPagar)).append("\n\n");
        sb.append("--------------------------------------------\n\n\n");
        sb.append("🔴 *Más info:* REDCELL.COM\n\n");
        sb.append("🤵 *Atendido por:* Yhan Alejandro Moreno Gonzalez\n\n");
        sb.append("📞 *WhatsApp:* 3117034142\n\n");
        sb.append("📍 *Ubicación:* Calle 23 No 15-35 (Por Ventanilla verde)\n\n");
        sb.append("🏙️ Armenia, Quindío");

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
}