package com.redcell.soat.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redcell.soat.dto.PolizaUpdateDTO;
import com.redcell.soat.entity.ConfiguracionTarifa;
import com.redcell.soat.entity.Poliza;
import com.redcell.soat.repository.ConfiguracionTarifaRepository;
import com.redcell.soat.repository.PolizaRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolizaService {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final PolizaRepository polizaRepository;
    private final CalculadoraSoatService calculadoraSoatService;
    private final ConfiguracionTarifaRepository tarifaRepository;

    public List<Poliza> obtenerTodas() {
        return polizaRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();
    }

    public Page<Poliza> buscarPaginado(
            String q,
            String aseguradora,
            String tipoVehiculo,
            String vigencia,
            boolean soloActivas,
            Pageable pageable) {

        Specification<Poliza> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (soloActivas) {
                predicates.add(cb.isTrue(root.get("activo")));
            }

            if (q != null && !q.trim().isEmpty()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate porPlaca = cb.like(cb.lower(cb.coalesce(root.get("placa"), "")), patron);
                Predicate porPropietario = cb.like(cb.lower(cb.coalesce(root.get("propietario"), "")), patron);
                Predicate porMarcaLinea = cb.like(cb.lower(cb.coalesce(root.get("marcaLinea"), "")), patron);
                Predicate porAseguradora = cb.like(cb.lower(cb.coalesce(root.get("aseguradora"), "")), patron);
                Predicate porDocumento = cb.like(
                        cb.lower(root.get("documento").as(String.class)),
                        patron
                );
                predicates.add(cb.or(porPlaca, porPropietario, porMarcaLinea, porAseguradora, porDocumento));
            }

            if (aseguradora != null && !aseguradora.trim().isEmpty()
                    && !"TODAS".equalsIgnoreCase(aseguradora.trim())) {
                String patronAseg = "%" + aseguradora.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("aseguradora"), "")), patronAseg));
            }

            if (tipoVehiculo != null && !tipoVehiculo.trim().isEmpty()
                    && !"TODOS".equalsIgnoreCase(tipoVehiculo.trim())) {
                String patronTipo = "%" + tipoVehiculo.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("tipoVehiculo"), "")), patronTipo));
            }

            if (vigencia != null && !vigencia.trim().isEmpty()
                    && !"TODAS".equalsIgnoreCase(vigencia.trim())) {
                LocalDate hoy = LocalDate.now(ZONA_COLOMBIA);
                String estado = vigencia.trim().toUpperCase(Locale.ROOT);

                switch (estado) {
                    case "VIGENTE" -> {
                        Predicate noVencida = cb.and(
                                cb.isNotNull(root.get("hasta")),
                                cb.greaterThanOrEqualTo(root.get("hasta"), hoy)
                        );
                        Predicate yaInicio = cb.or(
                                cb.isNull(root.get("desde")),
                                cb.lessThanOrEqualTo(root.get("desde"), hoy)
                        );
                        predicates.add(cb.and(noVencida, yaInicio));
                    }
                    case "POR_VENCER" -> {
                        LocalDate limite = hoy.plusDays(30);
                        predicates.add(cb.and(
                                cb.isNotNull(root.get("hasta")),
                                cb.greaterThanOrEqualTo(root.get("hasta"), hoy),
                                cb.lessThanOrEqualTo(root.get("hasta"), limite)
                        ));
                    }
                    case "VENCIDA" -> predicates.add(cb.and(
                            cb.isNotNull(root.get("hasta")),
                            cb.lessThan(root.get("hasta"), hoy)
                    ));
                    default -> {
                        // Si llega un valor no reconocido, no aplica filtro adicional de vigencia
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return polizaRepository.findAll(spec, pageable);
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

    @Transactional
    public void eliminar(Long id) {
        Poliza poliza = obtenerPorId(id);
        polizaRepository.delete(poliza);
    }
}