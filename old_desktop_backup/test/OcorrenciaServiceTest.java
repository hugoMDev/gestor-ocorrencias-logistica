package com.logistica.ocorrencias.service;

import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;
import com.logistica.ocorrencias.model.TipoOrdenacao;
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
    @DisplayName("Deve cadastrar ocorrência com sucesso e atribuir ID, monitor e status inicial")
    void deveCadastrarComSucesso() {
        Ocorrencia o = new Ocorrencia(
                null,
                null,
                PoloRegional.PAULINIA,
                TipoOcorrencia.FALHA_VEICULO,
                "BRA2E19",
                "João da Silva",
                "Mariana Rocha",
                Gravidade.CRITICA,
                null,
                "Veículo travou diferencial na rodovia"
        );

        Ocorrencia salva = service.salvar(o);

        assertNotNull(salva.getId(), "ID deve ser gerado automaticamente");
        assertNotNull(salva.getDataHora(), "Data/Hora deve ser gerada");
        assertEquals(StatusOcorrencia.PENDENTE, salva.getStatus(), "Status inicial deve ser PENDENTE");
        assertEquals("BRA2E19", salva.getPlacaVeiculo());
        assertEquals("Mariana Rocha", salva.getMonitor());
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
                "Monitor Valido",
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "Descrição válida do ocorrido"
        );

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.salvar(o));
        assertTrue(ex.getMessage().contains("Placa inválida"));
    }

    @Test
    @DisplayName("Deve disparar ValidacaoException para motorista curto, monitor curto ou descrição vazia")
    void deveRejeitarCamposObrigatoriosInvalidos() {
        Ocorrencia o1 = new Ocorrencia(
                null,
                LocalDateTime.now(),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "BRA2E19",
                "Jo", // menos de 3 caracteres
                "Monitor Valido",
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
                "Mo", // monitor com menos de 3 caracteres
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "Descrição válida"
        );
        assertThrows(ValidacaoException.class, () -> service.salvar(o2));

        Ocorrencia o3 = new Ocorrencia(
                null,
                LocalDateTime.now(),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "BRA2E19",
                "José Carlos",
                "Monitor Valido",
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "123" // menos de 5 caracteres
        );
        assertThrows(ValidacaoException.class, () -> service.salvar(o3));
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
                "Lucas CCO",
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
    @DisplayName("Deve bloquear alteração de gravidade durante o período de carência de 10 segundos")
    void deveBloquearAlteracaoGravidadeAntesDe10Segundos() {
        Ocorrencia o = new Ocorrencia(
                null,
                LocalDateTime.now(), // criada agora
                PoloRegional.PAULINIA,
                TipoOcorrencia.AVARIA,
                "ABC1234",
                "Motorista Teste",
                "Monitor Teste",
                Gravidade.BAIXA,
                StatusOcorrencia.PENDENTE,
                "Descrição detalhada do teste"
        );
        Ocorrencia salva = service.salvar(o);

        // Tentar alterar gravidade imediatamente deve disparar exceção com mensagem de carência
        ValidacaoException ex = assertThrows(ValidacaoException.class, () ->
                service.alterarGravidade(salva.getId(), Gravidade.ALTA)
        );
        assertTrue(ex.getMessage().contains("período de carência de 10 segundos"));
        assertTrue(service.obterSegundosRestantesCarenciaGravidade(salva) > 0);
    }

    @Test
    @DisplayName("Deve permitir alteração de gravidade após decorridos mais de 10 segundos")
    void devePermitirAlteracaoGravidadeApos10Segundos() {
        Ocorrencia o = new Ocorrencia(
                null,
                LocalDateTime.now().minusSeconds(15), // criada há 15 segundos
                PoloRegional.ITUPEVA,
                TipoOcorrencia.FALHA_VEICULO,
                "BRA2E19",
                "Motorista Seguro",
                "Monitor Central",
                Gravidade.MEDIA,
                StatusOcorrencia.PENDENTE,
                "Descrição detalhada com mais de 5 caracteres"
        );
        Ocorrencia salva = service.salvar(o);

        assertEquals(0, service.obterSegundosRestantesCarenciaGravidade(salva));

        Ocorrencia atualizada = service.alterarGravidade(salva.getId(), Gravidade.CRITICA);
        assertEquals(Gravidade.CRITICA, atualizada.getGravidade());
    }

    @Test
    @DisplayName("Deve ordenar ocorrências por Mais Recentes (Decrescente) e Mais Antigas (Crescente)")
    void deveOrdenarPorMaisRecentesEAntigas() {
        List<Ocorrencia> maisRecentes = service.filtrar(null, null, null, null, TipoOrdenacao.MAIS_RECENTES);
        assertFalse(maisRecentes.isEmpty());

        for (int i = 0; i < maisRecentes.size() - 1; i++) {
            LocalDateTime d1 = maisRecentes.get(i).getDataHora();
            LocalDateTime d2 = maisRecentes.get(i + 1).getDataHora();
            assertTrue(d1.isAfter(d2) || d1.isEqual(d2), "Deve estar em ordem decrescente");
        }

        List<Ocorrencia> maisAntigas = service.filtrar(null, null, null, null, TipoOrdenacao.MAIS_ANTIGOS);
        for (int i = 0; i < maisAntigas.size() - 1; i++) {
            LocalDateTime d1 = maisAntigas.get(i).getDataHora();
            LocalDateTime d2 = maisAntigas.get(i + 1).getDataHora();
            assertTrue(d1.isBefore(d2) || d1.isEqual(d2), "Deve estar em ordem crescente");
        }
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
    @DisplayName("Deve filtrar ocorrências por placa, motorista, monitor, polo e status")
    void deveFiltrarOcorrencias() {
        // Filtrar por termo de placa "BRA2E19"
        List<Ocorrencia> porPlaca = service.filtrar("BRA2E19", null, null, null);
        assertFalse(porPlaca.isEmpty());
        assertTrue(porPlaca.stream().allMatch(o -> o.getPlacaVeiculo().contains("BRA2E19")));

        // Filtrar por termo de monitor
        List<Ocorrencia> porMonitor = service.filtrar("Juliana", null, null, null);
        assertFalse(porMonitor.isEmpty());

        // Filtrar por Polo Paulínia
        List<Ocorrencia> porPolo = service.filtrar(null, PoloRegional.PAULINIA, null, null);
        assertFalse(porPolo.isEmpty());
        assertTrue(porPolo.stream().allMatch(o -> o.getPoloRegional() == PoloRegional.PAULINIA));
    }
}
