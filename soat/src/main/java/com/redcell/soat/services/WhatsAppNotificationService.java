package com.redcell.soat.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WhatsAppNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.service.url:http://localhost:8095}")
    private String whatsappServiceUrl;

    @Value("${whatsapp.default-country-code:57}")
    private String defaultCountryCode;

    @SuppressWarnings("unchecked")
    public boolean enviarMensaje(String telefonoRaw, String mensaje) {
        String telefonoNormalizado = normalizarTelefono(telefonoRaw);
        if (telefonoNormalizado == null) {
            log.warn("Teléfono inválido o vacío para WhatsApp: {}", telefonoRaw);
            return false;
        }

        try {
            String url = whatsappServiceUrl.replaceAll("/+$", "") + "/enviar";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> payload = new HashMap<>();
            payload.put("telefono", telefonoNormalizado);
            payload.put("mensaje", mensaje);

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);
            ResponseEntity<?> response = restTemplate.postForEntity(url, request, Map.class);

            HttpStatusCode status = response.getStatusCode();
            Object rawBody = response.getBody();
            Map<String, Object> body = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;

            boolean ok = status.is2xxSuccessful()
                    && body != null
                    && Boolean.TRUE.equals(body.get("success"));

            if (ok) {
                log.info("WhatsApp enviado a {}", telefonoNormalizado);
            } else {
                log.warn("WhatsApp no enviado a {} (status={}, body={})", telefonoNormalizado, status, body);
            }
            return ok;
        } catch (Exception e) {
            log.error("Error comunicándose con pg-ms-whatsapp-web para {}: {}", telefonoNormalizado, e.getMessage());
            return false;
        }
    }

    public String normalizarTelefono(String telefonoRaw) {
        if (telefonoRaw == null || telefonoRaw.isBlank()) {
            return null;
        }
        String digits = telefonoRaw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() == 10 && digits.startsWith("3")) {
            return defaultCountryCode + digits;
        }
        return digits;
    }
}