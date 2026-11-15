package com.logistica.ocorrencias.controller;

import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.repository.OcorrenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
