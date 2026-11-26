package com.redcell.soat.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import com.redcell.soat.dto.ApiResponse;
import com.redcell.soat.dto.NotificacionRequest;
import com.redcell.soat.dto.PolizaUpdateDTO;
import com.redcell.soat.entity.Poliza;
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

    private final PdfExtractorService pdfExtractorService;
    private final PolizaService polizaService;
    private final NotificacionSoatService notificacionSoatService;
    private final RecordatorioSoatScheduler recordatorioSoatScheduler;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> subirPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse("El archivo está vacío"));
            }

            Poliza poliza = pdfExtractorService.extraerDatosPdf(file);

            if (poliza.getPlaca() == null) {
                return ResponseEntity.badRequest().body(new ApiResponse("No se pudo extraer la placa del PDF. Verifica el formato."));
            }

            polizaService.guardarOActualizarPoliza(poliza);

            return ResponseEntity.ok(new ApiResponse("Póliza guardada. Placa: " + poliza.getPlaca() + ". Las pólizas anteriores fueron desactivadas si existían."));

        } catch (Exception e) {
            log.error("Error al procesar el PDF", e);
            return ResponseEntity.badRequest().body(new ApiResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Poliza>> listarTodas() {
        return ResponseEntity.ok(polizaService.obtenerTodas());
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

    // --- NOTIFICACIONES ---

    @PostMapping("/{id}/notificar")
    public ResponseEntity<Map<String, Object>> notificarPoliza(
            @PathVariable Long id,
            @RequestBody(required = false) NotificacionRequest req) {

        Poliza poliza = polizaService.obtenerPorId(id);

        String tel = (req != null) ? req.getTelefono() : null;
        String mail = (req != null) ? req.getEmail() : null;
        boolean sendWs = (req == null) || req.getWhatsapp() == null || req.getWhatsapp();
        boolean sendMail = (req == null) || req.getGmail() == null || req.getGmail();

        // Encolamos la notificación para que se envíe en segundo plano
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