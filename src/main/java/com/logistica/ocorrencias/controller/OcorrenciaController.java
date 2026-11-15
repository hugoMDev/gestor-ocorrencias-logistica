package com.logistica.ocorrencias.controller;

import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.repository.OcorrenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocorrencias")
@CrossOrigin(origins = "*")
public class OcorrenciaController {

    private static final Set<String> STATUS_VALIDOS = Set.of("ABERTO", "EM_ANDAMENTO", "CONCLUIDO");

    @Autowired
    private OcorrenciaRepository repository;

    @GetMapping
    public List<Ocorrencia> listar() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Ocorrencia> criar(@RequestBody Ocorrencia ocorrencia) {
        Ocorrencia novaOcorrencia = repository.save(ocorrencia);
        return ResponseEntity.ok(novaOcorrencia);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Ocorrencia> atualizarStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        return processarAtualizacaoStatus(id, payload);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Ocorrencia> atualizarStatusPut(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        return processarAtualizacaoStatus(id, payload);
    }

    private ResponseEntity<Ocorrencia> processarAtualizacaoStatus(UUID id, Map<String, String> payload) {
        if (payload == null || !payload.containsKey("status")) {
            return ResponseEntity.badRequest().build();
        }

        String novoStatus = payload.get("status");
        if (novoStatus == null || novoStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String statusLimpo = novoStatus.trim().toUpperCase();
        if (statusLimpo.equals("PENDENTE")) statusLimpo = "ABERTO";
        if (statusLimpo.equals("EM_ANALISE")) statusLimpo = "EM_ANDAMENTO";
        if (statusLimpo.equals("RESOLVIDO")) statusLimpo = "CONCLUIDO";

        if (!STATUS_VALIDOS.contains(statusLimpo)) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Ocorrencia> optionalOcorrencia = repository.findById(id);
        if (optionalOcorrencia.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ocorrencia ocorrencia = optionalOcorrencia.get();
        ocorrencia.setStatus(statusLimpo);
        Ocorrencia ocorrenciaAtualizada = repository.save(ocorrencia);
        return ResponseEntity.ok(ocorrenciaAtualizada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ocorrencia> atualizarCompleto(
            @PathVariable UUID id,
            @RequestBody Ocorrencia dados) {

        Optional<Ocorrencia> optionalOcorrencia = repository.findById(id);
        if (optionalOcorrencia.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ocorrencia ocorrencia = optionalOcorrencia.get();
        if (dados.getStatus() != null && !dados.getStatus().isBlank()) {
            ocorrencia.setStatus(dados.getStatus().trim().toUpperCase());
        }
        if (dados.getGravidade() != null && !dados.getGravidade().isBlank()) {
            ocorrencia.setGravidade(dados.getGravidade());
        }
        if (dados.getMotorista() != null) ocorrencia.setMotorista(dados.getMotorista());
        if (dados.getPlacaVeiculo() != null) ocorrencia.setPlacaVeiculo(dados.getPlacaVeiculo());
        if (dados.getPoloRegional() != null) ocorrencia.setPoloRegional(dados.getPoloRegional());
        if (dados.getTipoOcorrencia() != null) ocorrencia.setTipoOcorrencia(dados.getTipoOcorrencia());
        if (dados.getMonitorResponsavel() != null) ocorrencia.setMonitorResponsavel(dados.getMonitorResponsavel());
        if (dados.getDescricaoFatos() != null) ocorrencia.setDescricaoFatos(dados.getDescricaoFatos());

        Ocorrencia salva = repository.save(ocorrencia);
        return ResponseEntity.ok(salva);
    }
}
