package com.logistica.ocorrencias.service;

import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;
import com.logistica.ocorrencias.repository.JsonOcorrenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OcorrenciaServiceTest {

    @TempDir
    Path tempDir;

    private OcorrenciaService service;

    @BeforeEach
    void setUp() {
        Path jsonPath = tempDir.resolve("service_test.json");
        JsonOcorrenciaRepository repository = new JsonOcorrenciaRepository(jsonPath.toString());
        service = new OcorrenciaService(repository);
    }

    @Test
    @DisplayName("Deve cadastrar ocorrência com sucesso e atribuir ID e status inicial")
    void deveCadastrarComSucesso() {
        Ocorrencia o = new Ocorrencia(
                null,
                null,
                PoloRegional.PAULINIA,
                TipoOcorrencia.FALHA_VEICULO,
                "BRA2E19",
                "João da Silva",
                Gravidade.CRITICA,
                null,
                "Veículo travou diferencial na rodovia"
        );

        Ocorrencia salva = service.salvar(o);

        assertNotNull(salva.getId(), "ID deve ser gerado automaticamente");
        assertNotNull(salva.getDataHora(), "Data/Hora deve ser gerada");
        assertEquals(StatusOcorrencia.PENDENTE, salva.getStatus(), "Status inicial deve ser PENDENTE");
        assertEquals("BRA2E19", salva.getPlacaVeiculo());
    }

    @Test
    @DisplayName("Deve disparar ValidacaoException para placa veicular inválida")
    void deveRejeitarPlacaInvalida() {
        Ocorrencia o = new Ocorrencia(
                null,
                LocalDateTime.now(),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "INVALIDA99",
                "Motorista Valido",
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "Descrição válida do ocorrido"
        );

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.salvar(o));
        assertTrue(ex.getMessage().contains("Placa inválida"));
    }

    @Test
    @DisplayName("Deve disparar ValidacaoException para motorista curto ou descrição vazia")
    void deveRejeitarCamposObrigatoriosInvalidos() {
        Ocorrencia o1 = new Ocorrencia(
                null,
                LocalDateTime.now(),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "BRA2E19",
                "Jo", // menos de 3 caracteres
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "Descrição válida"
        );

        assertThrows(ValidacaoException.class, () -> service.salvar(o1));

        Ocorrencia o2 = new Ocorrencia(
                null,
                LocalDateTime.now(),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "BRA2E19",
                "José Carlos",
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "123" // menos de 5 caracteres
        );

        assertThrows(ValidacaoException.class, () -> service.salvar(o2));
    }

    @Test
    @DisplayName("Deve atualizar status e registrar resolução")
    void deveAtualizarStatusEResolucao() {
        Ocorrencia o = new Ocorrencia(
                null,
                LocalDateTime.now(),
                PoloRegional.CAPITAL_SP,
                TipoOcorrencia.ATRASO_CARGA,
                "ABC1234",
                "Renato Aragão",
                Gravidade.BAIXA,
                StatusOcorrencia.PENDENTE,
                "Entrega atrasada por trânsito"
        );
        Ocorrencia salva = service.salvar(o);

        // Mudar para Em Análise
        Ocorrencia emAnalise = service.alterarStatus(salva.getId(), StatusOcorrencia.EM_ANALISE, null);
        assertEquals(StatusOcorrencia.EM_ANALISE, emAnalise.getStatus());

        // Resolver com observação
        Ocorrencia resolvida = service.alterarStatus(salva.getId(), StatusOcorrencia.RESOLVIDO, "Carga entregue com sucesso");
        assertEquals(StatusOcorrencia.RESOLVIDO, resolvida.getStatus());
        assertNotNull(resolvida.getDataResolucao());
        assertEquals("Carga entregue com sucesso", resolvida.getObservacaoResolucao());
    }

    @Test
    @DisplayName("Deve calcular corretamente os KPIs operacionais")
    void deveCalcularKpisCorretamente() {
        OcorrenciaService.KpiResumo kpis = service.obterResumoKpis();

        assertNotNull(kpis);
        assertEquals(kpis.total(), kpis.abertas() + kpis.resolvidas());
        assertTrue(kpis.criticas() >= 0);
    }

    @Test
    @DisplayName("Deve filtrar ocorrências por placa, polo e status")
    void deveFiltrarOcorrencias() {
        // Filtrar por termo "BRA2E19"
        List<Ocorrencia> porPlaca = service.filtrar("BRA2E19", null, null, null);
        assertFalse(porPlaca.isEmpty());
        assertTrue(porPlaca.stream().allMatch(o -> o.getPlacaVeiculo().contains("BRA2E19")));

        // Filtrar por Polo Paulínia
        List<Ocorrencia> porPolo = service.filtrar(null, PoloRegional.PAULINIA, null, null);
        assertFalse(porPolo.isEmpty());
        assertTrue(porPolo.stream().allMatch(o -> o.getPoloRegional() == PoloRegional.PAULINIA));
    }
}
