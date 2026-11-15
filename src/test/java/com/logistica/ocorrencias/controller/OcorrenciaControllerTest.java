package com.logistica.ocorrencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.repository.OcorrenciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OcorrenciaController.class)
class OcorrenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OcorrenciaRepository repository;

    @Test
    @DisplayName("Deve atualizar status da ocorrência com sucesso via PATCH")
    void deveAtualizarStatusComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        Ocorrencia oco = new Ocorrencia();
        oco.setId(id);
        oco.setCodigoOco("OCO-1001");
        oco.setStatus("ABERTO");

        when(repository.findById(id)).thenReturn(Optional.of(oco));
        when(repository.save(any(Ocorrencia.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(patch("/api/ocorrencias/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONCLUIDO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));
    }

    @Test
    @DisplayName("Deve retornar 404 se a ocorrência não existir ao atualizar status")
    void deveRetornar404SeNaoExistir() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/ocorrencias/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONCLUIDO"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 se o status for nulo ou vazio")
    void deveRetornar400SeStatusVazio() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/ocorrencias/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 se o status for inválido")
    void deveRetornar400SeStatusInvalido() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/ocorrencias/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "STATUS_DESCONHECIDO"))))
                .andExpect(status().isBadRequest());
    }
}
