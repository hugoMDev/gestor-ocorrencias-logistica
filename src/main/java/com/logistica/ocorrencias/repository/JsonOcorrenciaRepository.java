package com.logistica.ocorrencias.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementação de persistência local 100% offline em arquivo JSON com Jackson.
 * Garante thread-safety e gravação atômica para evitar corrupção de dados.
 */
public class JsonOcorrenciaRepository implements OcorrenciaRepository {

    private static final String DEFAULT_FILE_PATH = "dados/ocorrencias.json";
    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public JsonOcorrenciaRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public JsonOcorrenciaRepository(String customPath) {
        this.filePath = Paths.get(customPath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        inicializarArquivo();
    }

    /**
     * Garante a criação do diretório e do arquivo inicial de persistência.
     */
    private void inicializarArquivo() {
        try {
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                List<Ocorrencia> dadosIniciais = criarDadosIniciais();
                gravarArquivo(dadosIniciais);
            }
        } catch (IOException e) {
            System.err.println("Erro ao inicializar arquivo de persistência: " + e.getMessage());
        }
    }

    @Override
    public List<Ocorrencia> findAll() {
        lock.readLock().lock();
        try {
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(filePath.toFile(), new TypeReference<List<Ocorrencia>>() {});
        } catch (IOException e) {
            System.err.println("Erro ao ler dados de ocorrências: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Ocorrencia> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(o -> id.equalsIgnoreCase(o.getId()))
                .findFirst();
    }

    @Override
    public Ocorrencia save(Ocorrencia ocorrencia) {
        if (ocorrencia == null) {
            throw new IllegalArgumentException("Ocorrência não pode ser nula.");
        }

        lock.writeLock().lock();
        try {
            List<Ocorrencia> lista = new ArrayList<>(findAll());
            int index = -1;
            for (int i = 0; i < lista.size(); i++) {
                if (lista.get(i).getId().equalsIgnoreCase(ocorrencia.getId())) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                lista.set(index, ocorrencia);
            } else {
                lista.add(0, ocorrencia); // adiciona no início para ficar em destaque
            }

            gravarArquivo(lista);
            return ocorrencia;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar ocorrência no arquivo local: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            List<Ocorrencia> lista = new ArrayList<>(findAll());
            boolean removed = lista.removeIf(o -> id.equalsIgnoreCase(o.getId()));
            if (removed) {
                gravarArquivo(lista);
            }
            return removed;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao remover ocorrência: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public int count() {
        return findAll().size();
    }

    /**
     * Gravação segura e atômica utilizando arquivo temporário.
     */
    private void gravarArquivo(List<Ocorrencia> lista) throws IOException {
        Path tempFile = Files.createTempFile(filePath.getParent(), "ocorrencias_", ".tmp");
        objectMapper.writeValue(tempFile.toFile(), lista);
        Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Gera dados operacionais simulados realistas para a primeira inicialização.
     */
    private List<Ocorrencia> criarDadosIniciais() {
        List<Ocorrencia> dados = new ArrayList<>();

        Ocorrencia o1 = new Ocorrencia(
                "OCO-1001",
                LocalDateTime.now().minusHours(2).minusMinutes(15),
                PoloRegional.ITUPEVA,
                TipoOcorrencia.AVARIA,
                "BRA2E19",
                "Carlos Eduardo Santos",
                Gravidade.ALTA,
                StatusOcorrencia.PENDENTE,
                "Queda de 3 paletes de eletroeletrônicos durante manobra de descarga na doca 04 do CD Central."
        );

        Ocorrencia o2 = new Ocorrencia(
                "OCO-1002",
                LocalDateTime.now().minusHours(4).minusMinutes(40),
                PoloRegional.PAULINIA,
                TipoOcorrencia.FALHA_VEICULO,
                "RKL4F82",
                "Marcos Vinicius Lima",
                Gravidade.CRITICA,
                StatusOcorrencia.EM_ANALISE,
                "Superaquecimento do motor e vazamento de fluido hidráulico na Rodovia Anhanguera km 122. Veículo com carga química."
        );

        Ocorrencia o3 = new Ocorrencia(
                "OCO-1003",
                LocalDateTime.now().minusHours(6),
                PoloRegional.SAO_JOSE_DOS_CAMPOS,
                TipoOcorrencia.DESVIO_ROTA,
                "ABC7890",
                "Fernando Souza Ribeiro",
                Gravidade.MEDIA,
                StatusOcorrencia.EM_ANALISE,
                "Alerta de telemetria apontou desvio do corredor de segurança na Rodovia Presidente Dutra sem aviso prévio."
        );

        Ocorrencia o4 = new Ocorrencia(
                "OCO-1004",
                LocalDateTime.now().minusHours(18),
                PoloRegional.CAPITAL_SP,
                TipoOcorrencia.ATRASO_CARGA,
                "GHT3J45",
                "Roberto Albuquerque",
                Gravidade.BAIXA,
                StatusOcorrencia.RESOLVIDO,
                "Atraso de 90 minutos devido a congestionamento intenso na Marginal Tietê. Cliente notificado e entrega concluída."
        );
        o4.setDataResolucao(LocalDateTime.now().minusHours(16));
        o4.setObservacaoResolucao("Entrega realizada com comprovante digital assinado.");

        Ocorrencia o5 = new Ocorrencia(
                "OCO-1005",
                LocalDateTime.now().minusDays(1).minusHours(3),
                PoloRegional.OUTRO,
                TipoOcorrencia.SINISTRO,
                "MNO9012",
                "Luciano Mendes",
                Gravidade.CRITICA,
                StatusOcorrencia.RESOLVIDO,
                "Colisão lateral leve com defensa metálica em trecho de pista molhada na BR-116. Sem vítimas, guincho acionado."
        );
        o5.setDataResolucao(LocalDateTime.now().minusDays(1));
        o5.setObservacaoResolucao("Boletim de ocorrência lavrado e carga transferida para caminhão reserva.");

        dados.add(o1);
        dados.add(o2);
        dados.add(o3);
        dados.add(o4);
        dados.add(o5);

        return dados;
    }
}
