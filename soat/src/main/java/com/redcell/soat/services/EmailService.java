package com.redcell.soat.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redcell.soat.entity.Poliza;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${resend.api-url:https://api.resend.com/emails}")
    private String resendApiUrl;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from:RedCell SOAT <onboarding@resend.dev>}")
    private String fromEmail;

    public EmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public void enviarCorreoRecuperacion(String destino, String username, String token) {
        if (destino == null || destino.isBlank()) {
            throw new RuntimeException("El email del destinatario no puede estar vacío");
        }

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Restablecer contraseña - RedCell SOAT</title>
                            <style>
                                body {
                                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                    line-height: 1.6;
                                    color: #0F172A;
                                    margin: 0;
                                    padding: 28px 16px;
                                    background-color: #F8FAFC;
                                }
                                .container {
                                    max-width: 550px;
                                    margin: 0 auto;
                                    background-color: #FFFFFF;
                                    border-radius: 16px;
                                    border: 1px solid #E2E8F0;
                                    box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
                                    overflow: hidden;
                                }
                                .header {
                                    background: linear-gradient(135deg, #DC2626 0%%, #B91C1C 62%%, #16A34A 100%%);
                                    color: #FFFFFF;
                                    padding: 32px 24px;
                                    text-align: center;
                                }
                                .badge {
                                    display: inline-block;
                                    background-color: #FFFFFF;
                                    color: #DC2626;
                                    font-size: 11px;
                                    font-weight: 700;
                                    letter-spacing: 1.2px;
                                    text-transform: uppercase;
                                    padding: 4px 12px;
                                    border-radius: 999px;
                                    margin-bottom: 12px;
                                }
                                .header h1 {
                                    margin: 0;
                                    font-size: 26px;
                                    font-weight: 700;
                                    letter-spacing: 0.5px;
                                    color: #FFFFFF;
                                }
                                .header p {
                                    margin: 8px 0 0;
                                    color: #FFFFFF;
                                    opacity: 0.95;
                                    font-size: 14px;
                                }
                                .content {
                                    padding: 36px 32px;
                                    background-color: #FFFFFF;
                                }
                                .greeting {
                                    font-size: 18px;
                                    color: #0F172A;
                                    margin-bottom: 16px;
                                    font-weight: 600;
                                }
                                .message {
                                    color: #475569;
                                    margin-bottom: 24px;
                                    font-size: 15px;
                                    line-height: 1.55;
                                }
                                .code-container {
                                    background: #F8FAFC;
                                    padding: 26px 20px;
                                    border-radius: 14px;
                                    text-align: center;
                                    margin: 26px 0;
                                    border: 1px solid #E2E8F0;
                                    border-top: 4px solid #16A34A;
                                }
                                .code-label {
                                    font-size: 12px;
                                    color: #15803D;
                                    text-transform: uppercase;
                                    letter-spacing: 2.5px;
                                    margin-bottom: 14px;
                                    font-weight: 700;
                                }
                                .code {
                                    font-size: 42px;
                                    font-weight: 800;
                                    color: #DC2626;
                                    letter-spacing: 10px;
                                    background: #FFFFFF;
                                    display: inline-block;
                                    padding: 14px 26px;
                                    border-radius: 12px;
                                    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
                                    border: 1px solid #E2E8F0;
                                }
                                .expiry {
                                    font-size: 12px;
                                    color: #475569;
                                    margin-top: 14px;
                                }
                                .expiry strong {
                                    color: #15803D;
                                }
                                .warning-box {
                                    background-color: #FEF2F2;
                                    border-left: 4px solid #DC2626;
                                    padding: 14px 18px;
                                    margin: 22px 0 0;
                                    border-radius: 8px;
                                }
                                .warning-text {
                                    color: #334155;
                                    font-size: 12.5px;
                                    margin: 0;
                                    line-height: 1.45;
                                }
                                .warning-text strong {
                                    color: #DC2626;
                                }
                                .footer {
                                    background-color: #F8FAFC;
                                    padding: 20px 28px;
                                    text-align: center;
                                    border-top: 1px solid #E2E8F0;
                                }
                                .footer-text {
                                    color: #64748B;
                                    font-size: 11.5px;
                                    margin: 4px 0;
                                }
                                .highlight-red {
                                    color: #DC2626;
                                    font-weight: 700;
                                }
                                .highlight-green {
                                    color: #16A34A;
                                    font-weight: 700;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <div class="badge">Seguridad de cuenta</div>
                                    <h1>RedCell SOAT</h1>
                                    <p>Gestión inteligente de seguros</p>
                                </div>

                                <div class="content">
                                    <div class="greeting">
                                        Hola, <strong>%s</strong>
                                    </div>

                                    <div class="message">
                                        Recibimos una solicitud para restablecer la contraseña de tu cuenta en
                                        <span class="highlight-red">RedCell</span> <span class="highlight-green">SOAT</span>.
                                        Ingresa el siguiente código de verificación para continuar:
                                    </div>

                                    <div class="code-container">
                                        <div class="code-label">Código de verificación</div>
                                        <div class="code">%s</div>
                                        <div class="expiry">
                                            Válido por <strong>15 minutos</strong>
                                        </div>
                                    </div>

                                    <div class="warning-box">
                                        <div class="warning-text">
                                            <strong>Importante:</strong> Si no solicitaste este cambio, ignora este correo.
                                            Tu contraseña actual seguirá protegida sin modificaciones.
                                        </div>
                                    </div>
                                </div>

                                <div class="footer">
                                    <div class="footer-text">
                                        © 2026 <span class="highlight-red">RedCell</span> <span class="highlight-green">SOAT</span> · Todos los derechos reservados
                                    </div>
                                    <div class="footer-text">
                                        Mensaje automático del sistema · Por favor no respondas a este correo
                                    </div>
                                    <div class="footer-text">
                                        Armenia, Quindío
                                    </div>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                username, token);

        boolean enviado = enviarHtmlPorApi(destino, "Restablecimiento de contraseña - RedCell SOAT", htmlContent);
        if (!enviado) {
            throw new RuntimeException("No se pudo enviar el email de recuperación");
        }
        log.info("Email de recuperación enviado exitosamente a: {}", destino);
    }

    public boolean enviarRecordatorioSoat(String destino, Poliza poliza, String mensajePlano) {
        if (destino == null || destino.isBlank()) {
            return false;
        }

        String propietario = poliza.getPropietario() != null && !poliza.getPropietario().isBlank()
                ? poliza.getPropietario().trim()
                : "Cliente";
        String documento = ((poliza.getTipoDocumento() != null ? poliza.getTipoDocumento().trim() : "") + " "
                + (poliza.getDocumento() != null ? poliza.getDocumento() : "")).trim();
        if (documento.isBlank()) {
            documento = "No registrado";
        }

        String vehiculo = poliza.getMarcaLinea() != null && !poliza.getMarcaLinea().isBlank()
                ? poliza.getMarcaLinea().trim()
                : (poliza.getTipoVehiculo() != null && !poliza.getTipoVehiculo().isBlank()
                        ? poliza.getTipoVehiculo().trim()
                        : "Vehículo");
        String vigencia = (poliza.getDesde() != null ? poliza.getDesde() : "-")
                + " al "
                + (poliza.getHasta() != null ? poliza.getHasta() : "-");

        BigDecimal totalPagar = poliza.getTotalPagar() != null ? poliza.getTotalPagar() : BigDecimal.ZERO;
        BigDecimal usoPlataforma = poliza.getUsoPlataforma() != null ? poliza.getUsoPlataforma() : BigDecimal.ZERO;
        BigDecimal valorSoat = totalPagar.subtract(usoPlataforma).max(BigDecimal.ZERO);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                                    background-color: #F8FAFC;
                                    color: #0F172A;
                                    margin: 0;
                                    padding: 28px 16px;
                                    line-height: 1.55;
                                }
                                .wrapper {
                                    max-width: 580px;
                                    margin: 0 auto;
                                    background-color: #FFFFFF;
                                    border: 1px solid #E2E8F0;
                                    border-radius: 14px;
                                    overflow: hidden;
                                    box-shadow: 0 10px 25px rgba(15, 23, 42, 0.06);
                                }
                                .header {
                                    background: linear-gradient(135deg, #DC2626 0%%, #B91C1C 62%%, #16A34A 100%%);
                                    color: #FFFFFF;
                                    padding: 26px 32px;
                                    border-bottom: 3px solid #15803D;
                                }
                                .header-brand {
                                    font-size: 21px;
                                    font-weight: 800;
                                    letter-spacing: 0.4px;
                                    margin: 0;
                                    color: #FFFFFF;
                                }
                                .header-sub {
                                    font-size: 13px;
                                    color: #FFFFFF;
                                    opacity: 0.95;
                                    margin: 4px 0 0;
                                }
                                .content {
                                    padding: 28px 32px;
                                    background-color: #FFFFFF;
                                }
                                .greeting {
                                    font-size: 16px;
                                    margin: 0 0 12px;
                                    color: #0F172A;
                                }
                                .lead {
                                    font-size: 14px;
                                    color: #475569;
                                    margin: 0 0 22px;
                                }
                                .summary-table {
                                    width: 100%%;
                                    border-collapse: collapse;
                                    margin-bottom: 22px;
                                    border: 1px solid #E2E8F0;
                                    border-radius: 10px;
                                    overflow: hidden;
                                }
                                .summary-table td {
                                    padding: 10px 14px;
                                    font-size: 13.5px;
                                    border-bottom: 1px solid #E2E8F0;
                                }
                                .summary-table tr:last-child td {
                                    border-bottom: none;
                                }
                                .label {
                                    color: #64748B;
                                    font-weight: 500;
                                    width: 38%%;
                                    background-color: #F8FAFC;
                                }
                                .value {
                                    color: #0F172A;
                                    font-weight: 600;
                                    text-align: right;
                                }
                                .amounts-box {
                                    background-color: #F8FAFC;
                                    border: 1px solid #E2E8F0;
                                    border-left: 4px solid #16A34A;
                                    border-radius: 10px;
                                    padding: 14px 18px;
                                    margin-bottom: 22px;
                                }
                                .amount-row {
                                    display: flex;
                                    justify-content: space-between;
                                    font-size: 13.5px;
                                    color: #475569;
                                    padding: 5px 0;
                                }
                                .total-row {
                                    border-top: 1px solid #E2E8F0;
                                    margin-top: 8px;
                                    padding-top: 10px;
                                    font-size: 15px;
                                    font-weight: 700;
                                    color: #DC2626;
                                }
                                .contact-note {
                                    font-size: 13px;
                                    color: #475569;
                                    margin: 0;
                                }
                                .footer {
                                    background-color: #F8FAFC;
                                    border-top: 1px solid #E2E8F0;
                                    padding: 18px 32px;
                                    font-size: 12px;
                                    color: #64748B;
                                }
                                .footer p {
                                    margin: 3px 0;
                                }
                                .brand-red {
                                    color: #DC2626;
                                    font-weight: 700;
                                }
                                .brand-green {
                                    color: #16A34A;
                                    font-weight: 700;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="wrapper">
                                <div class="header">
                                    <p class="header-brand">REDCELL SOAT</p>
                                    <p class="header-sub">Información y recordatorio de póliza</p>
                                </div>
                                <div class="content">
                                    <p class="greeting">Hola, <strong>%s</strong></p>
                                    <p class="lead">Compartimos los datos registrados de tu póliza SOAT y el resumen de valores:</p>

                                    <table class="summary-table">
                                        <tr>
                                            <td class="label">Placa</td>
                                            <td class="value">%s</td>
                                        </tr>
                                        <tr>
                                            <td class="label">Vehículo</td>
                                            <td class="value">%s</td>
                                        </tr>
                                        <tr>
                                            <td class="label">Documento</td>
                                            <td class="value">%s</td>
                                        </tr>
                                        <tr>
                                            <td class="label">Aseguradora</td>
                                            <td class="value">%s</td>
                                        </tr>
                                        <tr>
                                            <td class="label">Vigencia</td>
                                            <td class="value">%s</td>
                                        </tr>
                                    </table>

                                    <div class="amounts-box">
                                        <div class="amount-row">
                                            <span>Valor SOAT: </span>
                                            <strong> $%s</strong>
                                        </div>
                                        <div class="amount-row">
                                            <span>Uso de plataforma: </span>
                                            <strong style="color: #15803D;"> $%s</strong>
                                        </div>
                                        <div class="amount-row total-row">
                                            <span>Total a pagar: </span>
                                            <span> $%s</span>
                                        </div>
                                    </div>

                                    <p class="contact-note">
                                        Atendido por: Yhan Alejandro Moreno Gonzalez <br><br>WhatsApp: 311 703 4142 <br><br>Calle 23 No 15-35 (Por Ventanilla verde) - Armenia, Quindío.
                                    </p>
                                </div>
                                <div class="footer">
                                    <p><span class="brand-red">RedCell</span> <span class="brand-green">SOAT</span> · REDCELL.COM</p>
                                    <p>Mensaje informativo generado automáticamente.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                propietario,
                poliza.getPlaca() != null ? poliza.getPlaca() : "-",
                vehiculo,
                documento,
                poliza.getAseguradora() != null ? poliza.getAseguradora() : "-",
                vigencia,
                df.format(valorSoat),
                df.format(usoPlataforma),
                df.format(totalPagar)
        );

        String subject = "Información de tu SOAT - Placa " + (poliza.getPlaca() != null ? poliza.getPlaca() : "") + " | RedCell SOAT";
        boolean ok = enviarHtmlPorApi(destino, subject, htmlContent);
        if (ok) {
            log.info("Correo SOAT enviado a {} para placa {}", destino, poliza.getPlaca());
        }
        return ok;
    }

    private boolean enviarHtmlPorApi(String destino, String subject, String htmlContent) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("RESEND_API_KEY no está configurada; no se puede enviar correo a {}", destino);
            return false;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", fromEmail);
            payload.put("to", new String[]{destino.trim()});
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resendApiUrl))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }

            log.error("Error API correo ({}) enviando a {}: {}", response.statusCode(), destino, response.body());
            return false;
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error enviando correo SOAT a {}: {}", destino, e.getMessage());
            return false;
        }
    }
}