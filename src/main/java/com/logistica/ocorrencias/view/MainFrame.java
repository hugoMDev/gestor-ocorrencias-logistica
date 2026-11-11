package com.logistica.ocorrencias.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.service.OcorrenciaService;
import com.logistica.ocorrencias.util.DateUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Janela principal da aplicação "Gestor de Ocorrências Logísticas".
 * Layout moderno FlatLaf Dark com painel de KPIs, formulário lateral e tabela central com filtros.
 */
public class MainFrame extends JFrame {

    private final OcorrenciaService service;

    // Componentes de Resumo (KPI Cards)
    private KpiCard cardTotal;
    private KpiCard cardAbertas;
    private KpiCard cardCriticas;
    private KpiCard cardResolvidas;

    // Tabela e Modelo
    private JTable tabela;
    private OcorrenciaTableModel tableModel;

    // Formulário lateral
    private OcorrenciaFormPanel formPanel;

    // Componentes de Filtro
    private JTextField txtBusca;
    private JComboBox<Object> cbFiltroPolo;
    private JComboBox<Object> cbFiltroStatus;
    private JComboBox<Object> cbFiltroGravidade;

    // Botões de Ação
    private JButton btnAlterarStatus;
    private JButton btnVerDetalhes;
    private JButton btnExcluir;

    // Barra de Rodapé / Status
    private JLabel lblStatusRodape;

    public MainFrame(OcorrenciaService service) {
        super("Gestor de Ocorrências Logísticas - Centro de Operações de Transporte");
        this.service = service;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1260, 780);
        setLocationRelativeTo(null);

        construirLayout();
        atualizarDadosCompletos();
    }

    private void construirLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));

        // 1. Topo: Header com título e KPIs
        rootPanel.add(criarPainelSuperior(), BorderLayout.NORTH);

        // 2. Centro: Divisor com Formulário à esquerda e Tabela/Filtros à direita
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(360);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // Formulário à esquerda
        formPanel = new OcorrenciaFormPanel(service, this::atualizarDadosCompletos);
        splitPane.setLeftComponent(formPanel);

        // Painel central (Filtros + Tabela + Ações)
        splitPane.setRightComponent(criarPainelCentral());

        rootPanel.add(splitPane, BorderLayout.CENTER);

        // 3. Rodapé: Barra de status
        rootPanel.add(criarBarraRodape(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    /**
     * Constrói o cabeçalho superior e a grade de cartões de KPI.
     */
    private JPanel criarPainelSuperior() {
        JPanel headerPanel = new JPanel(new BorderLayout(0, 14));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(55, 65, 81)),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        headerPanel.setBackground(new Color(24, 28, 36));

        // Barra de Título Superior
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Central de Monitoramento e Ocorrências de Transporte");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Logística de Cargas • Rastreamento de Incidentes de Frota e Doca em Tempo Real");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(156, 163, 175));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle, BorderLayout.NORTH);
        titleBlock.add(lblSub, BorderLayout.SOUTH);

        titleRow.add(titleBlock, BorderLayout.WEST);

        headerPanel.add(titleRow, BorderLayout.NORTH);

        // Grade de Indicadores (KPIs)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setOpaque(false);

        cardTotal = new KpiCard("Total de Ocorrências", "0", "Histórico registrado", new Color(147, 197, 253));
        cardAbertas = new KpiCard("Ocorrências Abertas", "0", "Aguardando tratativa", new Color(251, 191, 36));
        cardCriticas = new KpiCard("Incidentes Críticos", "0", "Sinistros / Paradas graves", new Color(248, 113, 113));
        cardResolvidas = new KpiCard("Resolvidas", "0", "Concluídas com sucesso", new Color(52, 211, 153));

        kpiGrid.add(cardTotal);
        kpiGrid.add(cardAbertas);
        kpiGrid.add(cardCriticas);
        kpiGrid.add(cardResolvidas);

        headerPanel.add(kpiGrid, BorderLayout.CENTER);

        return headerPanel;
    }

    /**
     * Constrói o painel central contendo a barra de filtros, tabela de ocorrências e barra de ações.
     */
    private JPanel criarPainelCentral() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));

        // Barra superior de Filtros e Busca
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.setOpaque(false);

        // Campo de Busca
        txtBusca = new JTextField(18);
        txtBusca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar placa, motorista, termo...");
        txtBusca.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtBusca.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { aplicarFiltros(); }
            @Override
            public void removeUpdate(DocumentEvent e) { aplicarFiltros(); }
            @Override
            public void changedUpdate(DocumentEvent e) { aplicarFiltros(); }
        });

        // Filtro por Polo Regional
        cbFiltroPolo = new JComboBox<>();
        cbFiltroPolo.addItem("Todos os Polos");
        for (PoloRegional polo : PoloRegional.values()) {
            cbFiltroPolo.addItem(polo);
        }
        cbFiltroPolo.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        cbFiltroPolo.addActionListener(e -> aplicarFiltros());

        // Filtro por Status
        cbFiltroStatus = new JComboBox<>();
        cbFiltroStatus.addItem("Todos os Status");
        for (StatusOcorrencia s : StatusOcorrencia.values()) {
            cbFiltroStatus.addItem(s);
        }
        cbFiltroStatus.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        cbFiltroStatus.addActionListener(e -> aplicarFiltros());

        // Filtro por Gravidade
        cbFiltroGravidade = new JComboBox<>();
        cbFiltroGravidade.addItem("Todas as Gravidades");
        for (Gravidade g : Gravidade.values()) {
            cbFiltroGravidade.addItem(g);
        }
        cbFiltroGravidade.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        cbFiltroGravidade.addActionListener(e -> aplicarFiltros());

        JButton btnLimparFiltros = new JButton("Limpar Filtros");
        btnLimparFiltros.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnLimparFiltros.addActionListener(e -> limparFiltros());

        filterBar.add(txtBusca);
        filterBar.add(cbFiltroPolo);
        filterBar.add(cbFiltroStatus);
        filterBar.add(cbFiltroGravidade);
        filterBar.add(btnLimparFiltros);

        // Barra de Ações Rápidas (Direita / Topo da Tabela)
        JPanel actionsToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionsToolBar.setOpaque(false);

        JButton btnNovo = new JButton("+ Novo Registro");
        btnNovo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnNovo.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnNovo.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #2563eb; foreground: #ffffff;");
        btnNovo.addActionListener(e -> formPanel.limparCampos());

        btnVerDetalhes = new JButton("Ver Detalhes");
        btnVerDetalhes.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnVerDetalhes.setEnabled(false);
        btnVerDetalhes.addActionListener(e -> abrirDetalhesSelecionado());

        btnAlterarStatus = new JButton("Alterar Status ▼");
        btnAlterarStatus.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnAlterarStatus.setEnabled(false);
        btnAlterarStatus.addActionListener(e -> exibirMenuAlterarStatus());

        btnExcluir = new JButton("Excluir");
        btnExcluir.putClientProperty(FlatClientProperties.STYLE, "arc: 8; foreground: #f87171;");
        btnExcluir.setEnabled(false);
        btnExcluir.addActionListener(e -> excluirSelecionado());

        JButton btnExportar = new JButton("Exportar CSV");
        btnExportar.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnExportar.addActionListener(e -> exportarParaCsv());

        actionsToolBar.add(btnNovo);
        actionsToolBar.add(btnVerDetalhes);
        actionsToolBar.add(btnAlterarStatus);
        actionsToolBar.add(btnExcluir);
        actionsToolBar.add(btnExportar);

        JPanel topControls = new JPanel(new BorderLayout());
        topControls.setOpaque(false);
        topControls.add(filterBar, BorderLayout.WEST);
        topControls.add(actionsToolBar, BorderLayout.EAST);

        centerPanel.add(topControls, BorderLayout.NORTH);

        // Configuração da JTable
        tableModel = new OcorrenciaTableModel();
        tabela = new JTable(tableModel);
        configurarTabela();

        JScrollPane scrollTabela = new JScrollPane(tabela);
        scrollTabela.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81)));
        centerPanel.add(scrollTabela, BorderLayout.CENTER);

        return centerPanel;
    }

    /**
     * Configura propriedades estéticas, renderizadores e eventos da JTable.
     */
    private void configurarTabela() {
        tabela.setRowHeight(36);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setShowVerticalLines(false);
        tabela.setIntercellSpacing(new Dimension(0, 1));
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 36));

        // Renderizadores customizados
        tabela.getColumnModel().getColumn(1).setCellRenderer(new DateTimeCellRenderer()); // Data
        tabela.getColumnModel().getColumn(4).setCellRenderer(new PlacaCellRenderer());    // Placa
        tabela.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());    // Gravidade
        tabela.getColumnModel().getColumn(7).setCellRenderer(new BadgeCellRenderer());    // Status

        // Larguras preferenciais
        tabela.getColumnModel().getColumn(0).setPreferredWidth(85);   // ID
        tabela.getColumnModel().getColumn(1).setPreferredWidth(125);  // Data/Hora
        tabela.getColumnModel().getColumn(2).setPreferredWidth(190);  // Polo
        tabela.getColumnModel().getColumn(3).setPreferredWidth(170);  // Tipo
        tabela.getColumnModel().getColumn(4).setPreferredWidth(95);   // Placa
        tabela.getColumnModel().getColumn(5).setPreferredWidth(150);  // Motorista
        tabela.getColumnModel().getColumn(6).setPreferredWidth(110);  // Gravidade
        tabela.getColumnModel().getColumn(7).setPreferredWidth(110);  // Status
        tabela.getColumnModel().getColumn(8).setPreferredWidth(260);  // Descrição

        // Habilita/Desabilita botões conforme seleção
        tabela.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = tabela.getSelectedRow() >= 0;
            btnVerDetalhes.setEnabled(hasSelection);
            btnAlterarStatus.setEnabled(hasSelection);
            btnExcluir.setEnabled(hasSelection);
        });

        // Duplo clique para abrir detalhes
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) {
                    abrirDetalhesSelecionado();
                }
            }
        });
    }

    /**
     * Barra de status e rodapé com contadores e informações operacionais.
     */
    private JPanel criarBarraRodape() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(55, 65, 81)),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        footer.setBackground(new Color(24, 28, 36));

        lblStatusRodape = new JLabel("Carregando...");
        lblStatusRodape.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatusRodape.setForeground(new Color(156, 163, 175));

        JLabel lblStorage = new JLabel("Persistência: dados/ocorrencias.json (100% Offline)");
        lblStorage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStorage.setForeground(new Color(107, 114, 128));

        footer.add(lblStatusRodape, BorderLayout.WEST);
        footer.add(lblStorage, BorderLayout.EAST);

        return footer;
    }

    /**
     * Atualiza tabela, indicadores de KPI e rodapé com base nos dados mais recentes.
     */
    public void atualizarDadosCompletos() {
        // Atualiza KPIs
        OcorrenciaService.KpiResumo resumo = service.obterResumoKpis();
        cardTotal.setValor(resumo.total());
        cardAbertas.setValor(resumo.abertas());
        cardCriticas.setValor(resumo.criticas());
        cardResolvidas.setValor(resumo.resolvidas());

        // Aplica filtros na tabela
        aplicarFiltros();
    }

    /**
     * Aplica busca textual e filtros combinados na listagem de ocorrências.
     */
    private void aplicarFiltros() {
        String termo = txtBusca.getText();

        PoloRegional polo = null;
        if (cbFiltroPolo.getSelectedItem() instanceof PoloRegional p) {
            polo = p;
        }

        StatusOcorrencia status = null;
        if (cbFiltroStatus.getSelectedItem() instanceof StatusOcorrencia s) {
            status = s;
        }

        Gravidade gravidade = null;
        if (cbFiltroGravidade.getSelectedItem() instanceof Gravidade g) {
            gravidade = g;
        }

        List<Ocorrencia> filtradas = service.filtrar(termo, polo, status, gravidade);
        tableModel.setOcorrencias(filtradas);

        int totalGeral = service.listarTodas().size();
        lblStatusRodape.setText(String.format("Exibindo %d de %d ocorrências registradas | Filtros ativos: %s",
                filtradas.size(), totalGeral, (termo.isBlank() && polo == null && status == null && gravidade == null) ? "Nenhum" : "Sim"));
    }

    private void limparFiltros() {
        txtBusca.setText("");
        cbFiltroPolo.setSelectedIndex(0);
        cbFiltroStatus.setSelectedIndex(0);
        cbFiltroGravidade.setSelectedIndex(0);
        aplicarFiltros();
    }

    private Ocorrencia getOcorrenciaSelecionada() {
        int selectedRow = tabela.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = tabela.convertRowIndexToModel(selectedRow);
            return tableModel.getOcorrenciaAt(modelRow);
        }
        return null;
    }

    private void abrirDetalhesSelecionado() {
        Ocorrencia o = getOcorrenciaSelecionada();
        if (o != null) {
            OcorrenciaDetailDialog dialog = new OcorrenciaDetailDialog(this, o, service, this::atualizarDadosCompletos);
            dialog.setVisible(true);
        }
    }

    /**
     * Menu flutuante de alteração rápida de status para a linha selecionada.
     */
    private void exibirMenuAlterarStatus() {
        Ocorrencia o = getOcorrenciaSelecionada();
        if (o == null) return;

        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemPendente = new JMenuItem("Marcar como: PENDENTE");
        itemPendente.addActionListener(e -> {
            service.alterarStatus(o.getId(), StatusOcorrencia.PENDENTE, null);
            atualizarDadosCompletos();
        });

        JMenuItem itemAnalise = new JMenuItem("Marcar como: EM ANÁLISE");
        itemAnalise.addActionListener(e -> {
            service.alterarStatus(o.getId(), StatusOcorrencia.EM_ANALISE, null);
            atualizarDadosCompletos();
        });

        JMenuItem itemResolver = new JMenuItem("Marcar como: RESOLVIDO...");
        itemResolver.addActionListener(e -> {
            String obs = JOptionPane.showInputDialog(this, "Observação de resolução (opcional):", "Resolver Ocorrência", JOptionPane.QUESTION_MESSAGE);
            if (obs != null) {
                service.alterarStatus(o.getId(), StatusOcorrencia.RESOLVIDO, obs);
                atualizarDadosCompletos();
            }
        });

        menu.add(itemPendente);
        menu.add(itemAnalise);
        menu.addSeparator();
        menu.add(itemResolver);

        menu.show(btnAlterarStatus, 0, btnAlterarStatus.getHeight());
    }

    private void excluirSelecionado() {
        Ocorrencia o = getOcorrenciaSelecionada();
        if (o == null) return;

        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja excluir a ocorrência " + o.getId() + " (" + o.getTipoOcorrencia() + ")?",
                "Confirmar Exclusão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (resposta == JOptionPane.YES_OPTION) {
            boolean removido = service.excluir(o.getId());
            if (removido) {
                JOptionPane.showMessageDialog(this, "Ocorrência excluída com sucesso.");
                atualizarDadosCompletos();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao excluir a ocorrência.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Exporta os dados filtrados atualmente na tabela para formato CSV delimitado por ponto e vírgula.
     */
    private void exportarParaCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar Relatório de Ocorrências em CSV");
        chooser.setSelectedFile(new File("relatorio_ocorrencias.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv)", "csv"));

        int userSelection = chooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            if (!arquivo.getName().toLowerCase().endsWith(".csv")) {
                arquivo = new File(arquivo.getParentFile(), arquivo.getName() + ".csv");
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(arquivo, StandardCharsets.UTF_8))) {
                // BOM UTF-8 para correta abertura no Excel brasileiro
                pw.write('\ufeff');

                // Cabeçalho
                pw.println("ID;Data/Hora;Polo Regional;Tipo;Placa;Motorista;Gravidade;Status;Descricao;Data Resolucao;Obs Resolucao");

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Ocorrencia o = tableModel.getOcorrenciaAt(i);
                    pw.printf("%s;%s;%s;%s;%s;%s;%s;%s;\"%s\";%s;\"%s\"%n",
                            o.getId(),
                            DateUtils.formatar(o.getDataHora()),
                            o.getPoloRegional().name(),
                            o.getTipoOcorrencia().name(),
                            o.getPlacaVeiculo(),
                            o.getMotorista().replace(";", ","),
                            o.getGravidade().name(),
                            o.getStatus().name(),
                            o.getDescricao().replace("\"", "\"\"").replace("\n", " "),
                            DateUtils.formatar(o.getDataResolucao()),
                            o.getObservacaoResolucao() != null ? o.getObservacaoResolucao().replace("\"", "\"\"") : ""
                    );
                }

                JOptionPane.showMessageDialog(this, "Arquivo CSV exportado com sucesso em:\n" + arquivo.getAbsolutePath(),
                        "Exportação Concluída", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar CSV: " + ex.getMessage(),
                        "Erro de Gravação", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
