package com.logistica.ocorrencias.repository;

import com.logistica.ocorrencias.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {
    Optional<Ocorrencia> findByCodigoOco(String codigoOco);
}
