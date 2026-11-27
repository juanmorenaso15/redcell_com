package com.redcell.soat.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.redcell.soat.dto.AlertaRecordatorioDTO;
import com.redcell.soat.dto.ApiResponse;
import com.redcell.soat.dto.NotificacionRequest;
import com.redcell.soat.dto.PolizaUpdateDTO;
import com.redcell.soat.entity.HistorialRecordatorioSoat;
import com.redcell.soat.entity.Poliza;
import com.redcell.soat.services.AlertaRecordatorioService;
import com.redcell.soat.services.NotificacionSoatService;
import com.redcell.soat.services.PdfExtractorService;
import com.redcell.soat.services.PolizaService;
import com.redcell.soat.services.RecordatorioSoatScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/polizas")
@RequiredArgsConstructor
public class PolizaController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final PdfExtractorService pdfExtractorService;
    private final PolizaService polizaService;
    private final NotificacionSoatService notificacionSoatService;
    private final RecordatorioSoatScheduler recordatorioSoatScheduler;
    private final AlertaRecordatorioService alertaRecordatorioService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> subirPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse("El archivo está vacío"));
            }

            String nombre = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().toLowerCase(Locale.ROOT)
                    : "";
            String contentType = file.getContentType() != null
                    ? file.getContentType().toLowerCase(Locale.ROOT)
                    : "";

            if (!nombre.endsWith(".pdf") && !contentType.contains("pdf")) {
                return ResponseEntity.badRequest().body(new ApiResponse("Solo se permiten archivos en formato PDF"));
            }

            Poliza poliza = pdfExtractorService.extraerDatosPdf(file);

            if (poliza.getPlaca() == null || poliza.getPlaca().isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse("No se pudo extraer la placa del PDF. Verifica el formato."));
            }

            Poliza guardada = polizaService.guardarOActualizarPoliza(poliza);

            String avisoTarifa = (guardada.getPrecioSoat() == null)
                    ? " (Revisa los datos del vehículo para completar el cálculo de tarifa)."
                    : ". Las pólizas anteriores fueron desactivadas si existían.";

            return ResponseEntity.ok(new ApiResponse("Póliza guardada. Placa: " + guardada.getPlaca() + avisoTarifa));

        } catch (Exception e) {
            log.error("Error al procesar el PDF", e);
            return ResponseEntity.badRequest().body(new ApiResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Listado paginado (10 por defecto) y filtrado desde el backend.
     * Ejemplo: GET /api/polizas?page=0&size=10&q=ABC&aseguradora=PREVISORA&tipoVehiculo=MOTO&vigencia=VIGENTE
     */
@GetMapping
    public ResponseEntity<Page<Poliza>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String aseguradora,
            @RequestParam(required = false) String tipoVehiculo,
            @RequestParam(required = false) String vigencia,
            @RequestParam(defaultValue = "true") boolean soloActivas,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 100);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String propOrden = validarCampoOrden(sortBy);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, propOrden));

        Page<Poliza> resultado = polizaService.buscarPaginado(
                q, aseguradora, tipoVehiculo, vigencia, soloActivas, pageable
        );

        return ResponseEntity.ok(resultado);
    }

    private String validarCampoOrden(String sortBy) {
        if (sortBy == null) return "id";
        return switch (sortBy) {
            case "id", "placa", "propietario", "aseguradora", "tipoVehiculo", "desde", "hasta", "precioSoat", "totalPagar" -> sortBy;
            default -> "id";
        };
    }

    @GetMapping("/{id}")
    public ResponseEntity<Poliza> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(polizaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Poliza> actualizar(@PathVariable Long id, @RequestBody PolizaUpdateDTO dto) {
        Poliza actualizada = polizaService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> eliminar(@PathVariable Long id) {
        polizaService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse("Póliza eliminada exitosamente"));
    }

    @GetMapping("/alertas-recordatorios")
    public ResponseEntity<List<AlertaRecordatorioDTO>> obtenerAlertasRecordatorios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(alertaRecordatorioService.obtenerAlertasPendientes(fecha));
    }

    @PostMapping("/{id}/recordatorios/marcar-whatsapp-enviado")
    public ResponseEntity<Map<String, Object>> marcarWhatsappEnviado(
            @PathVariable Long id,
            @RequestParam String tipo) {

        HistorialRecordatorioSoat actualizado = alertaRecordatorioService.marcarWhatsappEnviado(id, tipo);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "WHATSAPP_MARCADO_ENVIADO");
        respuesta.put("polizaId", actualizado.getPolizaId());
        respuesta.put("placa", actualizado.getPlaca());
        respuesta.put("tipoRecordatorio", actualizado.getTipoRecordatorio());
        respuesta.put("enviadoWhatsapp", actualizado.getEnviadoWhatsapp());
        respuesta.put("enviadoEmail", actualizado.getEnviadoEmail());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/notificar")
    public ResponseEntity<Map<String, Object>> notificarPoliza(
            @PathVariable Long id,
            @RequestBody(required = false) NotificacionRequest req) {

        Poliza poliza = polizaService.obtenerPorId(id);

        String tel = (req != null) ? req.getTelefono() : null;
        String mail = (req != null) ? req.getEmail() : null;
        boolean sendWs = (req != null) && Boolean.TRUE.equals(req.getWhatsapp());
        boolean sendMail = (req == null) || req.getGmail() == null || req.getGmail();

        notificacionSoatService.encolarNotificacionPoliza(
                poliza.getId(), tel, mail, sendWs, sendMail, null, null, null
        );

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "ENCOLADO");
        respuesta.put("mensaje", "Notificación encolada para envío en segundo plano");
        respuesta.put("polizaId", poliza.getId());
        respuesta.put("placa", poliza.getPlaca());
        respuesta.put("whatsapp", sendWs);
        respuesta.put("gmail", sendMail);

        return ResponseEntity.accepted().body(respuesta);
    }

    @PostMapping("/recordatorios/ejecutar")
    public ResponseEntity<Map<String, Object>> ejecutarRecordatoriosManual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "false") boolean ignorarHistorial) {

        Map<String, Object> resultado = recordatorioSoatScheduler.ejecutarRecordatorios(fecha, tipo, ignorarHistorial);
        return ResponseEntity.ok(resultado);
    }
}