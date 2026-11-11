# Gestor de Ocorrências Logísticas 🚛📦

Aplicação desktop standalone desenvolvida 100% em **Java 17+**, **Swing** e estilizada com o tema escuro moderno **FlatLaf Dark**. O sistema foi concebido para centros de distribuição (CDs) e torres de controle de tráfego realizarem o monitoramento operacional, registro de incidentes na via ou doca e acompanhamento de rotas de transporte.

---

## 🌟 Principais Recursos

- **Interface Gráfica Moderna (Dark Theme)**: Estilização nativa com FlatLaf Dark, curvas suaves, badges com códigos de cor para níveis de gravidade e status do incidente.
- **Painel de Indicadores (KPIs em Tempo Real)**: Cards dinâmicos no topo apresentando o *Total de Ocorrências*, *Abertas*, *Incidentes Críticos* e *Resolvidas*.
- **Formulário de Cadastro Ágil**: Inserção rápida com validações de campos obrigatórios, seleção de Polos Regionais, Tipos de Ocorrência, Graus de Severidade e identificação do **Monitor Operacional Responsável**.
- **Validador de Placas Veiculares**: Suporte completo a placas no **Padrão Mercosul** (`ABC1D23`) e **Padrão Tradicional/Cinza** (`ABC-1234`), com formatação automática e conversão em caixa alta.
- **Tabela Interativa com Filtros e Ordenação**:
  - Busca textual em tempo real (placa, motorista, monitor, descrição, ID).
  - Filtros por Polo Regional, Status e Gravidade.
  - Ordenação por **Mais Recentes (Data/Hora Decrescente)** como padrão no filtro e controle dedicado de ordenação.
  - Coluna dedicada para o **Monitor Responsável** imediatamente ao lado da descrição.
- **Gestão de Gravidade com Trava Temporal (Carência de 10 segundos)**:
  - Botão de ação rápida **Alterar Gravidade** na barra superior para alternar entre `BAIXA`, `MEDIA`, `ALTA` e `CRITICA`.
  - **Regra de Trava Temporal**: Bloqueio de edição com carência de 10 segundos após a criação do registro, notificando o operador com os segundos restantes.
- **Ciclo de Vida e Tratativa Operacional**: Alteração de status direta (`PENDENTE` ➔ `EM ANÁLISE` ➔ `RESOLVIDO`), permitindo registrar anotações de fechamento e data/hora de resolução.
- **Exportação para CSV**: Exportação dos dados filtrados para arquivos `.csv` (com BOM UTF-8) incluindo o campo do Monitor para o Microsoft Excel e relatórios de auditoria.
- **Persistência 100% Offline e Local**: Gravação dos dados em formato JSON (`dados/ocorrencias.json`) através do Jackson, com criação automática de pastas e gravação atômica segura contra corrupção.

---

## 🏗️ Arquitetura do Software (MVC / Service)

O projeto adota uma arquitetura desacoplada e limpa:

```text
com.logistica.ocorrencias
├── model/                 # Entidades e enums de domínio
│   ├── Gravidade.java         # Enum: BAIXA, MEDIA, ALTA, CRITICA (com cores de badge)
│   ├── Ocorrencia.java        # Entidade central com anotações Jackson e campo monitor
│   ├── PoloRegional.java      # Enum: ITUPEVA, PAULINIA, SAO_JOSE_DOS_CAMPOS, CAPITAL_SP, OUTRO
│   ├── StatusOcorrencia.java  # Enum: PENDENTE, EM_ANALISE, RESOLVIDO
│   ├── TipoOcorrencia.java    # Enum: AVARIA, DESVIO_ROTA, ATRASO_CARGA, SINISTRO, FALHA_VEICULO
│   └── TipoOrdenacao.java     # Enum: MAIS_RECENTES, MAIS_ANTIGAS, GRAVIDADE_DECRESCENTE, etc.
├── repository/            # Camada de persistência desacoplada
│   ├── OcorrenciaRepository.java      # Interface do repositório
│   └── JsonOcorrenciaRepository.java  # Implementação local JSON offline (Jackson + Lock)
├── service/               # Regras de negócio e operações
│   ├── OcorrenciaService.java         # Validações, trava temporal de 10s, KPIs e filtros
│   └── ValidacaoException.java        # Exceção customizada de negócio
├── util/                  # Utilitários auxiliares
│   ├── DateUtils.java                 # Formatação de datas (dd/MM/yyyy HH:mm)
│   └── PlacaValidator.java            # Validação e normalização de placas Mercosul/BR
├── view/                  # Interface visual Swing + FlatLaf
│   ├── BadgeCellRenderer.java         # Renderizador de pílulas coloridas para status/gravidade
│   ├── DateTimeCellRenderer.java      # Renderizador formatado de datas
│   ├── PlacaCellRenderer.java         # Renderizador monoespaçado para placas
│   ├── KpiCard.java                   # Cartão moderno para métricas do topo
│   ├── OcorrenciaFormPanel.java       # Formulário lateral para cadastro rápido (com monitor)
│   ├── OcorrenciaDetailDialog.java    # Modal com histórico, monitor e detalhes completos
│   ├── OcorrenciaTableModel.java      # TableModel customizado (incluindo coluna Monitor)
│   └── MainFrame.java                 # Janela principal do sistema
└── App.java               # Ponto de entrada (FlatLaf Dark + inicialização na EDT)
```

---

## 📋 Pré-requisitos

- **Java JDK 17 ou superior** (testado e homologado no Java 21 LTS).
- **Maven** (O projeto já inclui o **Maven Wrapper** `mvnw` / `mvnw.cmd`, dispensando instalação global do Maven).

---

## 🚀 Como Executar no VS Code

### Opção 1: Pelo Painel "Run and Debug" (Recomendado)
1. Abra a pasta do projeto no **VS Code**.
2. Certifique-se de ter a extensão **Extension Pack for Java** da Microsoft instalada.
3. Pressione `F5` ou vá até a aba **Run & Debug** (`Ctrl + Shift + D`) e selecione a opção:
   `Executar Gestor de Ocorrências (App)`.
4. A aplicação inicializará no modo escuro na tela principal.

### Opção 2: Pelo Terminal Integrado do VS Code
Abra o terminal do VS Code (`Ctrl + '`) e execute:

```powershell
# Execução direta via Maven Wrapper
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.logistica.ocorrencias.App"
```

---

## 📦 Como Compilar e Gerar o JAR Standalone Executável

O projeto utiliza o `maven-shade-plugin` para empacotar todas as dependências em um único arquivo `.jar` executável ("fat JAR").

1. No terminal do projeto, execute o comando de compilação e empacotamento:

```powershell
# Se estiver usando o Maven Wrapper incluído no projeto:
.\mvnw.cmd clean package

# Ou se tiver o Maven configurado no PATH do sistema:
mvn clean package
```

2. Ao término da compilação, o arquivo executável standalone estará disponível em:
   `target/gestor-ocorrencias-logistica-1.0.0.jar`

---

## 🖥️ Como Executar o JAR Standalone

### Modo 1: Duplo Clique
Se o Java estiver associado a arquivos `.jar` no seu Windows, basta dar **duplo clique** no arquivo `gestor-ocorrencias-logistica-1.0.0.jar` dentro da pasta `target`.

### Modo 2: Linha de Comando (PowerShell / Prompt de Comando)
```powershell
java -jar target/gestor-ocorrencias-logistica-1.0.0.jar
```

---

## 🧪 Execução de Testes Automatizados

O projeto conta com bateria de testes unitários desenvolvidos com **JUnit 5**:

```powershell
.\mvnw.cmd test
```

Testes inclusos:
- Validação estrita de placas Mercosul e Tradicional (`PlacaValidatorTest`).
- Ciclo de vida completo de persistência JSON local e atômica (`JsonOcorrenciaRepositoryTest`).
- Regras de validação obrigatória (incluindo Monitor Responsável) e cálculo de KPIs (`OcorrenciaServiceTest`).
- Regra de trava temporal de carência de 10 segundos para alteração de gravidade (`OcorrenciaServiceTest`).
- Ordenação e filtros por Mais Recentes e Mais Antigas (`OcorrenciaServiceTest`).

---

## 💾 Armazenamento dos Dados (`dados/ocorrencias.json`)

- Os dados das ocorrências são persistidos automaticamente no arquivo `dados/ocorrencias.json`.
- Na primeira execução do sistema, caso o arquivo não exista, o software o cria automaticamente e o popula com incidentes realistas simulados com identificação dos monitores de torre.
- As gravações subsequentes utilizam arquivos temporários com substituição atômica (`StandardCopyOption.ATOMIC_MOVE`), garantindo segurança contra corrupção em caso de encerramento inesperado.
