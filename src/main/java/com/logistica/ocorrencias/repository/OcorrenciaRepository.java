package com.logistica.ocorrencias.repository;

import com.logistica.ocorrencias.model.Ocorrencia;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência para as ocorrências logísticas.
 */
public interface OcorrenciaRepository {

    /**
     * Retorna todas as ocorrências cadastradas.
     */
    List<Ocorrencia> findAll();

    /**
     * Busca uma ocorrência pelo seu identificador único.
     */
    Optional<Ocorrencia> findById(String id);

    /**
     * Salva uma nova ocorrência ou atualiza uma existente.
     */
    Ocorrencia save(Ocorrencia ocorrencia);

    /**
     * Remove uma ocorrência pelo seu ID.
     */
    boolean deleteById(String id);

    /**
     * Verifica se existe ocorrência com o ID informado.
     */
    boolean existsById(String id);

    /**
     * Retorna a quantidade total de ocorrências registradas.
     */
    int count();
}
