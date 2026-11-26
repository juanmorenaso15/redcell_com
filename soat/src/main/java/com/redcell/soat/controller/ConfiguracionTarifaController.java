package com.redcell.soat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.redcell.soat.dto.ApiResponse;
import com.redcell.soat.entity.ConfiguracionTarifa;
import com.redcell.soat.services.ConfiguracionTarifaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/configuracion-tarifas")
@RequiredArgsConstructor
public class ConfiguracionTarifaController {

    private final ConfiguracionTarifaService service;

    @GetMapping
    public ResponseEntity<List<ConfiguracionTarifa>> listar() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfiguracionTarifa> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ConfiguracionTarifa> crear(@RequestBody ConfiguracionTarifa tarifa) {
        return ResponseEntity.ok(service.crear(tarifa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracionTarifa> actualizar(@PathVariable Long id, @RequestBody ConfiguracionTarifa tarifa) {
        return ResponseEntity.ok(service.actualizar(id, tarifa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(new ApiResponse("Tarifa eliminada exitosamente"));
    }
}