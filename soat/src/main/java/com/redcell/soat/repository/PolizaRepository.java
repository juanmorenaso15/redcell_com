package com.redcell.soat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.redcell.soat.entity.Poliza;

public interface PolizaRepository extends JpaRepository<Poliza, Long>, JpaSpecificationExecutor<Poliza> {

    List<Poliza> findByPlaca(String placa);

    Optional<Poliza> findByPlacaAndActivoTrue(String placa);

    List<Poliza> findByActivoTrue();
}