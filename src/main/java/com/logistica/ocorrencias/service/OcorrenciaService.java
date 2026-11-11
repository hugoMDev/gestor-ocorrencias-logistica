package com.logistica.ocorrencias.service;

import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;
import com.logistica.ocorrencias.model.TipoOrdenacao;
import com.logistica.ocorrencias.repository.OcorrenciaRepository;
import com.logistica.ocorrencias.util.PlacaValidator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Camada de serviço contendo as regras de negócio, validações e cálculos do sistema.
 */
public class OcorrenciaService {

    private final OcorrenciaRepository repository;
    private final Random random = new Random();

    public OcorrenciaService(OcorrenciaRepository repository) {
        this.repository = repository;
    }

    /**
     * DTO de resumo dos Indicadores Chave de Desempenho (KPIs).
     */
    public record KpiResumo(int total, int abertas, int criticas, int resolvidas) {
    }

    /**
     * Valida e persiste uma nova ocorrência ou atualização.
     */
    public Ocorrencia salvar(Ocorrencia ocorrencia) {
        validar(ocorrencia);

        // Gera ID amigável se não existir
        if (ocorrencia.getId() == null || ocorrencia.getId().isBlank()) {
            ocorrencia.setId(gerarNovoId());
        }

        // Define data e hora atual caso não tenha sido informada
        if (ocorrencia.getDataHora() == null) {
            ocorrencia.setDataHora(LocalDateTime.now());
        }

        // Define status inicial como PENDENTE caso seja novo
        if (ocorrencia.getStatus() == null) {
            ocorrencia.setStatus(StatusOcorrencia.PENDENTE);
        }

        // Normaliza placa veicular para maiúsculas sem traços
        ocorrencia.setPlacaVeiculo(PlacaValidator.normalizar(ocorrencia.getPlacaVeiculo()));
        ocorrencia.setMotorista(ocorrencia.getMotorista().trim());
        if (ocorrencia.getMonitor() != null) {
            ocorrencia.setMonitor(ocorrencia.getMonitor().trim());
        }
        ocorrencia.setDescricao(ocorrencia.getDescricao().trim());

        return repository.save(ocorrencia);
    }

    /**
     * Validações de integridade e regras de negócio.
     */
    public void validar(Ocorrencia ocorrencia) {
        if (ocorrencia == null) {
            throw new ValidacaoException("Ocorrência não pode ser nula.");
        }

        if (ocorrencia.getPoloRegional() == null) {
            throw new ValidacaoException("Selecione o Polo Regional de origem da ocorrência.");
        }

        if (ocorrencia.getTipoOcorrencia() == null) {
            throw new ValidacaoException("Selecione o Tipo de Ocorrência.");
        }

        if (ocorrencia.getPlacaVeiculo() == null || ocorrencia.getPlacaVeiculo().isBlank()) {
            throw new ValidacaoException("A placa do veículo é obrigatória.");
        }

        if (!PlacaValidator.isValid(ocorrencia.getPlacaVeiculo())) {
            throw new ValidacaoException("Placa inválida! Informe no padrão Mercosul (ex: ABC1D23) ou Tradicional (ex: ABC-1234).");
        }

        if (ocorrencia.getMotorista() == null || ocorrencia.getMotorista().trim().length() < 3) {
            throw new ValidacaoException("O nome do motorista é obrigatório e deve ter pelo menos 3 caracteres.");
        }

        if (ocorrencia.getMonitor() == null || ocorrencia.getMonitor().trim().length() < 3) {
            throw new ValidacaoException("O nome do monitor responsável é obrigatório e deve ter pelo menos 3 caracteres.");
        }

        if (ocorrencia.getGravidade() == null) {
            throw new ValidacaoException("Selecione o nível de Gravidade da ocorrência.");
        }

        if (ocorrencia.getDescricao() == null || ocorrencia.getDescricao().trim().length() < 5) {
            throw new ValidacaoException("A descrição detalhada é obrigatória (mínimo de 5 caracteres).");
        }
    }

    /**
     * Altera o status da ocorrência e registra a data/observação de resolução se aplicável.
     */
    public Ocorrencia alterarStatus(String id, StatusOcorrencia novoStatus, String observacao) {
        Optional<Ocorrencia> op = repository.findById(id);
        if (op.isEmpty()) {
            throw new ValidacaoException("Ocorrência não encontrada com o ID: " + id);
        }

        Ocorrencia o = op.get();
        o.setStatus(novoStatus);

        if (novoStatus == StatusOcorrencia.RESOLVIDO) {
            o.setDataResolucao(LocalDateTime.now());
            if (observacao != null && !observacao.isBlank()) {
                o.setObservacaoResolucao(observacao.trim());
            }
        }

        return repository.save(o);
    }

    /**
     * Altera a gravidade da ocorrência respeitando a regra de trava temporal de carência de 10 segundos.
     */
    public Ocorrencia alterarGravidade(String id, Gravidade novaGravidade) {
        Optional<Ocorrencia> op = repository.findById(id);
        if (op.isEmpty()) {
            throw new ValidacaoException("Ocorrência não encontrada com o ID: " + id);
        }

        Ocorrencia o = op.get();
        long segundosRestantes = obterSegundosRestantesCarenciaGravidade(o);
        if (segundosRestantes > 0) {
            throw new ValidacaoException("Aguarde o período de carência de 10 segundos após o registro para alterar a gravidade (restam " + segundosRestantes + " segundos)");
        }

        if (novaGravidade == null) {
            throw new ValidacaoException("Selecione um nível de gravidade válido.");
        }

        o.setGravidade(novaGravidade);
        return repository.save(o);
    }

    /**
     * Calcula quantos segundos restam no período de carência de 10 segundos para alteração da gravidade.
     */
    public long obterSegundosRestantesCarenciaGravidade(Ocorrencia ocorrencia) {
        if (ocorrencia == null || ocorrencia.getDataHora() == null) {
            return 0;
        }
        long segundosDecorridos = Duration.between(ocorrencia.getDataHora(), LocalDateTime.now()).toSeconds();
        return (segundosDecorridos < 10) ? Math.max(1, 10 - segundosDecorridos) : 0;
    }

    /**
     * Exclui uma ocorrência pelo ID.
     */
    public boolean excluir(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return repository.deleteById(id);
    }

    /**
     * Busca uma ocorrência pelo ID.
     */
    public Optional<Ocorrencia> buscarPorId(String id) {
        return repository.findById(id);
    }

    /**
     * Retorna todas as ocorrências ordenadas da mais recente para a mais antiga.
     */
    public List<Ocorrencia> listarTodas() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Ocorrencia::getDataHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Filtra ocorrências por termo textual e filtros opcionais de categoria (ordenação padrão por Mais Recentes).
     */
    public List<Ocorrencia> filtrar(String termo, PoloRegional polo, StatusOcorrencia status, Gravidade gravidade) {
        return filtrar(termo, polo, status, gravidade, TipoOrdenacao.MAIS_RECENTES);
    }

    /**
     * Filtra ocorrências por termo textual, filtros opcionais e critério de ordenação customizado.
     */
    public List<Ocorrencia> filtrar(String termo, PoloRegional polo, StatusOcorrencia status, Gravidade gravidade, TipoOrdenacao ordenacao) {
        String termoBusca = (termo != null) ? termo.trim().toLowerCase(Locale.ROOT) : "";
        TipoOrdenacao ord = (ordenacao != null) ? ordenacao : TipoOrdenacao.MAIS_RECENTES;

        Comparator<Ocorrencia> comparator = switch (ord) {
            case MAIS_RECENTES -> Comparator.comparing(Ocorrencia::getDataHora, Comparator.nullsLast(Comparator.reverseOrder()));
            case MAIS_ANTIGAS -> Comparator.comparing(Ocorrencia::getDataHora, Comparator.nullsLast(Comparator.naturalOrder()));
            case GRAVIDADE_DECRESCENTE -> Comparator.comparingInt((Ocorrencia o) -> o.getGravidade().getNivel()).reversed()
                    .thenComparing(Ocorrencia::getDataHora, Comparator.nullsLast(Comparator.reverseOrder()));
            case GRAVIDADE_CRESCENTE -> Comparator.comparingInt((Ocorrencia o) -> o.getGravidade().getNivel())
                    .thenComparing(Ocorrencia::getDataHora, Comparator.nullsLast(Comparator.reverseOrder()));
        };

        return repository.findAll().stream()
                .filter(o -> {
                    // Filtro de texto (busca em ID, Placa, Motorista, Monitor, Descrição e Tipo)
                    if (!termoBusca.isEmpty()) {
                        boolean matchId = o.getId() != null && o.getId().toLowerCase().contains(termoBusca);
                        boolean matchPlaca = o.getPlacaVeiculo() != null && o.getPlacaVeiculo().toLowerCase().contains(termoBusca);
                        boolean matchMotorista = o.getMotorista() != null && o.getMotorista().toLowerCase().contains(termoBusca);
                        boolean matchMonitor = o.getMonitor() != null && o.getMonitor().toLowerCase().contains(termoBusca);
                        boolean matchDesc = o.getDescricao() != null && o.getDescricao().toLowerCase().contains(termoBusca);
                        boolean matchTipo = o.getTipoOcorrencia() != null && o.getTipoOcorrencia().getDescricao().toLowerCase().contains(termoBusca);

                        if (!matchId && !matchPlaca && !matchMotorista && !matchMonitor && !matchDesc && !matchTipo) {
                            return false;
                        }
                    }

                    // Filtro de Polo
                    if (polo != null && o.getPoloRegional() != polo) {
                        return false;
                    }

                    // Filtro de Status
                    if (status != null && o.getStatus() != status) {
                        return false;
                    }

                    // Filtro de Gravidade
                    if (gravidade != null && o.getGravidade() != gravidade) {
                        return false;
                    }

                    return true;
                })
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Calcula e consolida os indicadores operacionais em tempo real.
     */
    public KpiResumo obterResumoKpis() {
        List<Ocorrencia> todas = repository.findAll();
        int total = todas.size();
        int abertas = 0;
        int criticas = 0;
        int resolvidas = 0;

        for (Ocorrencia o : todas) {
            if (o.getStatus() == StatusOcorrencia.RESOLVIDO) {
                resolvidas++;
            } else {
                abertas++;
                if (o.getGravidade() == Gravidade.CRITICA) {
                    criticas++;
                }
            }
        }

        return new KpiResumo(total, abertas, criticas, resolvidas);
    }

    /**
     * Gera um identificador único legível no padrão logístico "OCO-XXXX".
     */
    private String gerarNovoId() {
        int numero = 1000 + random.nextInt(9000);
        String idProposto = "OCO-" + numero;
        while (repository.existsById(idProposto)) {
            numero = 1000 + random.nextInt(9000);
            idProposto = "OCO-" + numero;
        }
        return idProposto;
    }
}
