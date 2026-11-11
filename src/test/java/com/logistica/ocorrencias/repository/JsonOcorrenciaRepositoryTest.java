package com.logistica.ocorrencias.repository;

import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonOcorrenciaRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve inicializar repositório e criar arquivo com dados iniciais")
    void deveInicializarComDadosIniciais() {
        Path jsonPath = tempDir.resolve("teste_ocorrencias.json");
        JsonOcorrenciaRepository repo = new JsonOcorrenciaRepository(jsonPath.toString());

        List<Ocorrencia> list = repo.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Repositório inicial deve conter dados simulados");
        assertTrue(repo.count() >= 5);
    }

    @Test
    @DisplayName("Deve salvar, recuperar, atualizar e excluir ocorrência")
    void deveManipularCicloDeVidaOcorrencia() {
        Path jsonPath = tempDir.resolve("ciclo_vida.json");
        JsonOcorrenciaRepository repo = new JsonOcorrenciaRepository(jsonPath.toString());

        Ocorrencia o = new Ocorrencia(
                "TEST-9999",
                LocalDateTime.now(),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "TEST1234",
                "Motorista Teste",
                Gravidade.ALTA,
                StatusOcorrencia.PENDENTE,
                "Descrição de teste unitário"
        );

        // Salvar
        repo.save(o);
        assertTrue(repo.existsById("TEST-9999"));

        // Buscar por ID
        Optional<Ocorrencia> encontrada = repo.findById("TEST-9999");
        assertTrue(encontrada.isPresent());
        assertEquals("Motorista Teste", encontrada.get().getMotorista());

        // Atualizar
        o.setStatus(StatusOcorrencia.RESOLVIDO);
        o.setObservacaoResolucao("Problema solucionado com sucesso");
        repo.save(o);

        Optional<Ocorrencia> atualizada = repo.findById("TEST-9999");
        assertTrue(atualizada.isPresent());
        assertEquals(StatusOcorrencia.RESOLVIDO, atualizada.get().getStatus());
        assertEquals("Problema solucionado com sucesso", atualizada.get().getObservacaoResolucao());

        // Excluir
        boolean deleted = repo.deleteById("TEST-9999");
        assertTrue(deleted);
        assertFalse(repo.existsById("TEST-9999"));
    }
}
