package com.logistica.ocorrencias.controller;

import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.repository.OcorrenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocorrencias")
@CrossOrigin(origins = "*")
public class OcorrenciaController {

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
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String status) {

        String novoStatus = null;
        if (body != null && body.containsKey("status")) {
            novoStatus = body.get("status");
        } else if (status != null && !status.isBlank()) {
            novoStatus = status;
        }

        if (novoStatus == null || novoStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Ocorrencia> optionalOcorrencia = buscarPorIdOuCodigo(id);
        if (optionalOcorrencia.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ocorrencia ocorrencia = optionalOcorrencia.get();
        ocorrencia.setStatus(novoStatus.trim().toUpperCase());
        Ocorrencia salva = repository.save(ocorrencia);
        return ResponseEntity.ok(salva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ocorrencia> atualizarCompleto(
            @PathVariable String id,
            @RequestBody Ocorrencia dados) {

        Optional<Ocorrencia> optionalOcorrencia = buscarPorIdOuCodigo(id);
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

    private Optional<Ocorrencia> buscarPorIdOuCodigo(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID uuid = UUID.fromString(identificador);
            Optional<Ocorrencia> porUuid = repository.findById(uuid);
            if (porUuid.isPresent()) {
                return porUuid;
            }
        } catch (IllegalArgumentException ignored) {
            // Não é um UUID, tenta pelo código da ocorrência
        }
        return repository.findByCodigoOco(identificador);
    }
}
