package com.redcell.soat.services;

import com.redcell.soat.dto.PolizaUpdateDTO;
import com.redcell.soat.entity.ConfiguracionTarifa;
import com.redcell.soat.entity.Poliza;
import com.redcell.soat.repository.ConfiguracionTarifaRepository;
import com.redcell.soat.repository.PolizaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final CalculadoraSoatService calculadoraSoatService;
    private final ConfiguracionTarifaRepository tarifaRepository;

    // LISTAR TODAS (Solo las activas)
    public List<Poliza> obtenerTodas() {
        return polizaRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();
    }

    // BUSCAR POR ID
    public Poliza obtenerPorId(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Póliza no encontrada con el ID proporcionado."));
    }

    // GUARDAR O RENOVAR (Desactiva las antiguas de la misma placa)
    @Transactional
    public Poliza guardarOActualizarPoliza(Poliza nuevaPoliza) {
        // 1. Buscamos si ya existen pólizas con esa placa
        List<Poliza> polizasAnteriores = polizaRepository.findByPlaca(nuevaPoliza.getPlaca());

        // 2. Si existen, las desactivamos a todas
        if (!polizasAnteriores.isEmpty()) {
            for (Poliza antigua : polizasAnteriores) {
                antigua.setActivo(false);
                polizaRepository.save(antigua);
            }
        }

        // 3. Marcamos la nueva como activa
        nuevaPoliza.setActivo(true);
        if (nuevaPoliza.getEdadVehiculo() == null) {
            nuevaPoliza.setEdadVehiculo(0);
        }
        if (nuevaPoliza.getCapacidadToneladas() == null) {
            nuevaPoliza.setCapacidadToneladas(BigDecimal.ZERO);
        }

        // 4. Calculamos el precio de la nueva póliza conservando la edad calculada desde el modelo
        try {
            BigDecimal totalSoat = calculadoraSoatService.calcularTotalPagar(nuevaPoliza);
            BigDecimal tarifaPlataforma = tarifaRepository.findByActivoTrue()
                    .map(ConfiguracionTarifa::getValor)
                    .orElse(BigDecimal.ZERO);

            nuevaPoliza.setUsoPlataforma(tarifaPlataforma);
            nuevaPoliza.setTotalPagar(totalSoat.add(tarifaPlataforma));

        } catch (Exception e) {
            log.warn("No se pudo calcular el precio automáticamente (faltan datos): {}", e.getMessage());
        }

        return polizaRepository.save(nuevaPoliza);
    }

    // ACTUALIZAR PARCIALMENTE Y RECALCULAR PRECIO (Para ediciones manuales)
    @Transactional
    public Poliza actualizar(Long id, PolizaUpdateDTO dto) {
        Poliza poliza = obtenerPorId(id);

        // 1. Actualizar campos básicos
        if (dto.getTipoVehiculo() != null) poliza.setTipoVehiculo(dto.getTipoVehiculo());
        if (dto.getPlaca() != null) poliza.setPlaca(dto.getPlaca());
        if (dto.getPropietario() != null) poliza.setPropietario(dto.getPropietario());
        if (dto.getTipoDocumento() != null) poliza.setTipoDocumento(dto.getTipoDocumento());
        if (dto.getDocumento() != null) poliza.setDocumento(dto.getDocumento());
        if (dto.getDesde() != null) poliza.setDesde(dto.getDesde());
        if (dto.getHasta() != null) poliza.setHasta(dto.getHasta());
        if (dto.getMarcaLinea() != null) poliza.setMarcaLinea(dto.getMarcaLinea());
        if (dto.getCilindraje() != null) poliza.setCilindraje(dto.getCilindraje());
        if (dto.getAseguradora() != null) poliza.setAseguradora(dto.getAseguradora());

        // 2. Actualizar edad y capacidad en toneladas
        // 2. Actualizar edad, capacidad y datos de contacto
if (dto.getEdadVehiculo() != null) poliza.setEdadVehiculo(Math.max(dto.getEdadVehiculo(), 0));
if (dto.getCapacidadToneladas() != null) poliza.setCapacidadToneladas(dto.getCapacidadToneladas());
if (dto.getTelefono() != null) poliza.setTelefono(dto.getTelefono());
if (dto.getEmail() != null) poliza.setEmail(dto.getEmail());

        // 3. RECÁLCULO AUTOMÁTICO AL EDITAR
        if (poliza.getTipoVehiculo() != null) {
            try {
                if (poliza.getEdadVehiculo() == null) poliza.setEdadVehiculo(0);
                if (poliza.getCapacidadToneladas() == null) poliza.setCapacidadToneladas(BigDecimal.ZERO);

                BigDecimal totalSoat = calculadoraSoatService.calcularTotalPagar(poliza);

                BigDecimal tarifaPlataforma = tarifaRepository.findByActivoTrue()
                        .map(ConfiguracionTarifa::getValor)
                        .orElse(BigDecimal.ZERO);

                poliza.setUsoPlataforma(tarifaPlataforma);
                poliza.setTotalPagar(totalSoat.add(tarifaPlataforma));

                log.info("Precio recalculado exitosamente para la póliza: {}", poliza.getPlaca());
            } catch (Exception e) {
                log.warn("No se pudo recalcular el precio al editar: {}", e.getMessage());
            }
        }

        return polizaRepository.save(poliza);
    }

    // ELIMINAR
    @Transactional
    public void eliminar(Long id) {
        Poliza poliza = obtenerPorId(id);
        polizaRepository.delete(poliza);
    }
}