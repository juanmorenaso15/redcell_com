package com.redcell.soat.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.redcell.soat.entity.ConfiguracionTarifa;

public interface ConfiguracionTarifaRepository extends JpaRepository<ConfiguracionTarifa, Long> {
    
    Optional<ConfiguracionTarifa> findByActivoTrue();
}