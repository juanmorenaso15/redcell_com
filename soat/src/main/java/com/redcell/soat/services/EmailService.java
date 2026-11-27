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
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Restablecer contraseña - RedCell SOAT</title>
                            <style>
                                body {
                                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                    line-height: 1.6;
                                    color: #2c3e50;
                                    margin: 0;
                                    padding: 0;
                                    background-color: #f8fafc;
                                }
                                .container {
                                    max-width: 550px;
                                    margin: 30px auto;
                                    padding: 0;
                                    background-color: #ffffff;
                                    border-radius: 20px;
                                    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
                                    overflow: hidden;
                                    border: 1px solid #e2e8f0;
                                }
                                .header {
                                    background-color: #dc2626;
                                    color: #ffffff;
                                    padding: 35px 20px;
                                    text-align: center;
                                    border-bottom: 4px solid #16a34a;
                                }
                                .header h1 {
                                    margin: 0;
                                    font-size: 28px;
                                    font-weight: 600;
                                    letter-spacing: 1px;
                                    color: #ffffff;
                                }
                                .header p {
                                    margin: 10px 0 0;
                                    opacity: 0.95;
                                    font-size: 14px;
                                    color: #ffffff;
                                }
                                .content {
                                    padding: 40px 35px;
                                    background-color: #ffffff;
                                }
                                .greeting {
                                    font-size: 20px;
                                    color: #1e293b;
                                    margin-bottom: 20px;
                                    font-weight: 500;
                                }
                                .message {
                                    color: #475569;
                                    margin-bottom: 25px;
                                    font-size: 15px;
                                    line-height: 1.5;
                                }
                                .code-container {
                                    background: #f8fafc;
                                    padding: 30px;
                                    border-radius: 16px;
                                    text-align: center;
                                    margin: 30px 0;
                                    border: 1px solid #e2e8f0;
                                }
                                .code-label {
                                    font-size: 12px;
                                    color: #16a34a;
                                    text-transform: uppercase;
                                    letter-spacing: 3px;
                                    margin-bottom: 15px;
                                    font-weight: 700;
                                }
                                .code {
                                    font-size: 48px;
                                    font-weight: 700;
                                    color: #dc2626;
                                    letter-spacing: 10px;
                                    background: #ffffff;
                                    display: inline-block;
                                    padding: 15px 30px;
                                    border-radius: 12px;
                                    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
                                    border: 1px solid #e2e8f0;
                                }
                                .expiry {
                                    font-size: 12px;
                                    color: #16a34a;
                                    margin-top: 15px;
                                }
                                .warning-box {
                                    background-color: #f8fafc;
                                    border-left: 3px solid #dc2626;
                                    padding: 15px 20px;
                                    margin: 25px 0;
                                    border-radius: 8px;
                                }
                                .warning-text {
                                    color: #64748b;
                                    font-size: 12px;
                                    margin: 0;
                                    line-height: 1.4;
                                }
                                .footer {
                                    background-color: #f8fafc;
                                    padding: 20px 30px;
                                    text-align: center;
                                    border-top: 1px solid #e2e8f0;
                                }
                                .footer-text {
                                    color: #64748b;
                                    font-size: 11px;
                                    margin: 5px 0;
                                }
                                .highlight {
                                    color: #16a34a;
                                    text-decoration: none;
                                    font-weight: 600;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1><strong>RedCell SOAT</strong></h1>
                                    <p>Gestión inteligente de seguros</p>
                                </div>

                                <div class="content">
                                    <div class="greeting">
                                        Estimado(a) <strong>%s</strong>,
                                    </div>

                                    <div class="message">
                                        Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en <strong>RedCell SOAT</strong>.
                                        Para garantizar la seguridad de tu cuenta, utiliza el siguiente código de verificación:
                                    </div>

                                    <div class="code-container">
                                        <div class="code-label">CÓDIGO DE VERIFICACIÓN</div>
                                        <div class="code">%s</div>
                                        <div class="expiry">
                                            Válido por <strong>15 minutos</strong>
                                        </div>
                                    </div>

                                    <div class="warning-box">
                                        <div class="warning-text">
                                            <strong>ℹ️ Importante:</strong> Si no solicitaste este cambio, por favor ignora este mensaje.
                                            Tu contraseña permanecerá segura y sin cambios.
                                        </div>
                                    </div>

                                </div>

                                <div class="footer">
                                    <div class="footer-text">
                                        © 2026 RedCell SOAT - Todos los derechos reservados
                                    </div>
                                    <div class="footer-text">
                                        Este es un mensaje automático, por favor no responder a este correo
                                    </div>
                                    <div class="footer-text">
                                        <span class="highlight">RedCell SOAT</span> - Armenia, Quindío
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
                                    background-color: #f8fafc;
                                    color: #1e293b;
                                    margin: 0;
                                    padding: 28px 16px;
                                    line-height: 1.55;
                                }
                                .wrapper {
                                    max-width: 580px;
                                    margin: 0 auto;
                                    background-color: #ffffff;
                                    border: 1px solid #e2e8f0;
                                    border-radius: 10px;
                                    overflow: hidden;
                                }
                                .header {
                                    background-color: #dc2626;
                                    color: #ffffff;
                                    padding: 26px 32px;
                                    border-bottom: 3px solid #16a34a;
                                }
                                .header-brand {
                                    font-size: 20px;
                                    font-weight: 700;
                                    letter-spacing: 0.4px;
                                    margin: 0;
                                    color: #ffffff;
                                }
                                .header-sub {
                                    font-size: 13px;
                                    color: #ffffff;
                                    opacity: 0.95;
                                    margin: 4px 0 0;
                                }
                                .content {
                                    padding: 28px 32px;
                                }
                                .greeting {
                                    font-size: 15px;
                                    margin: 0 0 14px;
                                    color: #1e293b;
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
                                    border: 1px solid #e2e8f0;
                                    border-radius: 8px;
                                }
                                .summary-table td {
                                    padding: 10px 14px;
                                    font-size: 13.5px;
                                    border-bottom: 1px solid #edf2f7;
                                }
                                .summary-table tr:last-child td {
                                    border-bottom: none;
                                }
                                .label {
                                    color: #64748b;
                                    font-weight: 500;
                                    width: 38%%;
                                }
                                .value {
                                    color: #1e293b;
                                    font-weight: 600;
                                    text-align: right;
                                }
                                .amounts-box {
                                    background-color: #f8fafc;
                                    border: 1px solid #e2e8f0;
                                    border-radius: 8px;
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
                                    border-top: 1px solid #cbd5e1;
                                    margin-top: 8px;
                                    padding-top: 10px;
                                    font-size: 15px;
                                    font-weight: 700;
                                    color: #dc2626;
                                }
                                .contact-note {
                                    font-size: 13px;
                                    color: #475569;
                                    margin: 0;
                                }
                                .footer {
                                    background-color: #f8fafc;
                                    border-top: 1px solid #e2e8f0;
                                    padding: 18px 32px;
                                    font-size: 12px;
                                    color: #64748b;
                                }
                                .footer p {
                                    margin: 3px 0;
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
                                            <strong style="color: #16a34a;"> $%s</strong>
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
                                    <p>RedCell SOAT · REDCELL.COM</p>
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