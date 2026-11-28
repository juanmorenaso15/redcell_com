package com.redcell.soat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.redcell.soat.entity.Poliza;

public interface PolizaRepository extends JpaRepository<Poliza, Long>, JpaSpecificationExecutor<Poliza> {

    List<Poliza> findByPlaca(String placa);

    Optional<Poliza> findByPlacaAndActivoTrue(String placa);

    List<Poliza> findByActivoTrue();

    @Modifying
    @Query("DELETE FROM Poliza p WHERE p.activo = false OR p.activo IS NULL")
    int deletePolizasInactivas();
}