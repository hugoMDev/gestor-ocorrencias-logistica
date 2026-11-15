package com.logistica.ocorrencias.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OcorrenciaTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Deve inicializar atributos com valores padrão ao criar nova ocorrência")
    void deveInicializarValoresPadrao() {
        Ocorrencia oco = new Ocorrencia();

        assertNotNull(oco.getCodigoOco());
        assertTrue(oco.getCodigoOco().startsWith("OCO-"));
        assertNotNull(oco.getDataHora());
        assertEquals("PENDENTE", oco.getStatus());
        assertEquals("MEDIA", oco.getGravidade());
        assertEquals("Operador Central", oco.getMonitorResponsavel());
        assertNull(oco.getPoloRegional());
        assertNull(oco.getMotorista());
        assertNull(oco.getPlacaVeiculo());
        assertNull(oco.getDescricaoFatos());
    }

    @Test
    @DisplayName("Deve preencher valores padrão no @PrePersist caso sejam nulos")
    void devePreencherValoresPadraoNoPrePersist() {
        Ocorrencia oco = new Ocorrencia();
        oco.setCodigoOco(null);
        oco.setDataHora(null);
        oco.setStatus(null);
        oco.setGravidade(null);
        oco.setMonitorResponsavel(null);

        oco.prePersist();

        assertNotNull(oco.getCodigoOco());
        assertTrue(oco.getCodigoOco().startsWith("OCO-"));
        assertNotNull(oco.getDataHora());
        assertEquals("PENDENTE", oco.getStatus());
        assertEquals("MEDIA", oco.getGravidade());
        assertEquals("Operador Central", oco.getMonitorResponsavel());
    }

    @Test
    @DisplayName("Deve desserializar JSON parcial com aliases (snake_case e legados)")
    void deveDesserializarJsonParcialComAliases() throws Exception {
        String json = """
            {
                "placa_veiculo": "BRA2E19",
                "motorista": "Carlos Silva",
                "monitor": "Juliana Rocha",
                "descricao": "Falha na entrega"
            }
            """;

        Ocorrencia oco = objectMapper.readValue(json, Ocorrencia.class);

        assertEquals("BRA2E19", oco.getPlacaVeiculo());
        assertEquals("Carlos Silva", oco.getMotorista());
        assertEquals("Juliana Rocha", oco.getMonitorResponsavel());
        assertEquals("Falha na entrega", oco.getDescricaoFatos());
        assertEquals("PENDENTE", oco.getStatus());
        assertEquals("MEDIA", oco.getGravidade());
        assertNotNull(oco.getCodigoOco());
        assertNotNull(oco.getDataHora());
    }
}
