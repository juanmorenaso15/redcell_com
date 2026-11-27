package com.redcell.soat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redcell.soat.dto.ApiResponse;
import com.redcell.soat.entity.ConfiguracionTarifa;
import com.redcell.soat.services.ConfiguracionTarifaService;

import jakarta.validation.Valid;
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
    public ResponseEntity<ConfiguracionTarifa> crear(@Valid @RequestBody ConfiguracionTarifa tarifa) {
        return ResponseEntity.ok(service.crear(tarifa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracionTarifa> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracionTarifa tarifa) {
        return ResponseEntity.ok(service.actualizar(id, tarifa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(new ApiResponse("Tarifa eliminada exitosamente"));
    }
}