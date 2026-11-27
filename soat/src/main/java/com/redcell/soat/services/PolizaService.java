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

    public List<Poliza> obtenerTodas() {
        return polizaRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();
    }

    public Poliza obtenerPorId(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Póliza no encontrada con el ID proporcionado."));
    }

    @Transactional
    public Poliza guardarOActualizarPoliza(Poliza nuevaPoliza) {
        List<Poliza> polizasAnteriores = polizaRepository.findByPlaca(nuevaPoliza.getPlaca());

        if (!polizasAnteriores.isEmpty()) {
            for (Poliza antigua : polizasAnteriores) {
                antigua.setActivo(false);
                polizaRepository.save(antigua);
            }
        }

        nuevaPoliza.setActivo(true);
        if (nuevaPoliza.getEdadVehiculo() == null) {
            nuevaPoliza.setEdadVehiculo(0);
        }
        if (nuevaPoliza.getCapacidadToneladas() == null) {
            nuevaPoliza.setCapacidadToneladas(BigDecimal.ZERO);
        }

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

    @Transactional
    public Poliza actualizar(Long id, PolizaUpdateDTO dto) {
        Poliza poliza = obtenerPorId(id);

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

        if (dto.getEdadVehiculo() != null) poliza.setEdadVehiculo(Math.max(dto.getEdadVehiculo(), 0));
        if (dto.getCapacidadToneladas() != null) poliza.setCapacidadToneladas(dto.getCapacidadToneladas());
        if (dto.getTelefono() != null) poliza.setTelefono(dto.getTelefono());
        if (dto.getEmail() != null) poliza.setEmail(dto.getEmail());

        if (dto.getUsoPlataforma() != null) {
            poliza.setUsoPlataforma(dto.getUsoPlataforma().max(BigDecimal.ZERO));
        } else if (poliza.getUsoPlataforma() == null) {
            BigDecimal tarifaDefault = tarifaRepository.findByActivoTrue()
                    .map(ConfiguracionTarifa::getValor)
                    .orElse(BigDecimal.ZERO);
            poliza.setUsoPlataforma(tarifaDefault);
        }

        if (poliza.getTipoVehiculo() != null) {
            try {
                if (poliza.getEdadVehiculo() == null) poliza.setEdadVehiculo(0);
                if (poliza.getCapacidadToneladas() == null) poliza.setCapacidadToneladas(BigDecimal.ZERO);

                BigDecimal totalSoat = calculadoraSoatService.calcularTotalPagar(poliza);
                BigDecimal usoPlataforma = poliza.getUsoPlataforma() != null
                        ? poliza.getUsoPlataforma()
                        : BigDecimal.ZERO;

                poliza.setUsoPlataforma(usoPlataforma);
                poliza.setTotalPagar(totalSoat.add(usoPlataforma));

                log.info("Precio recalculado para póliza {}: SOAT={}, UsoPlataforma={}, Total={}",
                        poliza.getPlaca(), totalSoat, usoPlataforma, poliza.getTotalPagar());
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