package com.redcell.soat.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redcell.soat.entity.ConfiguracionTarifa;
import com.redcell.soat.repository.ConfiguracionTarifaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionTarifaService {

    private final ConfiguracionTarifaRepository repository;

    public List<ConfiguracionTarifa> obtenerTodas() {
        return repository.findAll();
    }

    public ConfiguracionTarifa obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));
    }

    @Transactional
    public ConfiguracionTarifa crear(ConfiguracionTarifa tarifa) {
        if (Boolean.TRUE.equals(tarifa.getActivo())) {
            desactivarTodas();
        }
        return repository.save(tarifa);
    }

    @Transactional
    public ConfiguracionTarifa actualizar(Long id, ConfiguracionTarifa datosNuevos) {
        ConfiguracionTarifa tarifa = obtenerPorId(id);
        
        if (datosNuevos.getNombre() != null) tarifa.setNombre(datosNuevos.getNombre());
        if (datosNuevos.getValor() != null) tarifa.setValor(datosNuevos.getValor());
        
        if (datosNuevos.getActivo() != null) {
            if (Boolean.TRUE.equals(datosNuevos.getActivo())) {
                desactivarTodas();
            }
            tarifa.setActivo(datosNuevos.getActivo());
        }

        return repository.save(tarifa);
    }

    @Transactional
    public void eliminar(Long id) {
        ConfiguracionTarifa tarifa = obtenerPorId(id);
        repository.delete(tarifa);
    }

    private void desactivarTodas() {
        List<ConfiguracionTarifa> todas = repository.findAll();
        for (ConfiguracionTarifa t : todas) {
            if (Boolean.TRUE.equals(t.getActivo())) {
                t.setActivo(false);
                repository.save(t);
            }
        }
    }
}