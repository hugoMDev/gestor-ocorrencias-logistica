package com.logistica.ocorrencias.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.service.OcorrenciaService;
import com.logistica.ocorrencias.util.DateUtils;
import com.logistica.ocorrencias.util.PlacaValidator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * Diálogo modal para visualização detalhada e alteração de status de uma ocorrência.
 */
public class OcorrenciaDetailDialog extends JDialog {

    private final OcorrenciaService service;
    private final Ocorrencia ocorrencia;
    private final Runnable onUpdatedCallback;

    public OcorrenciaDetailDialog(JFrame parent, Ocorrencia ocorrencia, OcorrenciaService service, Runnable onUpdatedCallback) {
        super(parent, "Detalhes da Ocorrência - " + ocorrencia.getId(), true);
        this.service = service;
        this.ocorrencia = ocorrencia;
        this.onUpdatedCallback = onUpdatedCallback;

        setSize(650, 580);
        setMinimumSize(new Dimension(580, 520));
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        // Topo com ID e Status
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        topPanel.setBackground(new Color(30, 34, 42));

        JPanel titleBox = new JPanel(new BorderLayout(0, 4));
        titleBox.setOpaque(false);

        JLabel lblId = new JLabel("Ocorrência " + ocorrencia.getId());
        lblId.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblId.setForeground(Color.WHITE);

        JLabel lblData = new JLabel("Aberta em " + DateUtils.formatar(ocorrencia.getDataHora()));
        lblData.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblData.setForeground(new Color(156, 163, 175));

        titleBox.add(lblId, BorderLayout.NORTH);
        titleBox.add(lblData, BorderLayout.CENTER);

        // Badge de Status no canto direito
        JLabel lblStatus = new JLabel(ocorrencia.getStatus().getDescricao());
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setForeground(ocorrencia.getStatus().getBadgeForeground());
        lblStatus.setBackground(ocorrencia.getStatus().getBadgeBackground());
        lblStatus.setOpaque(true);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        lblStatus.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");

        topPanel.add(titleBox, BorderLayout.CENTER);
        topPanel.add(lblStatus, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Painel Central com Informações em Grade
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Linha 1: Polo e Tipo
        adicionarCampo(contentPanel, gbc, row++, "Polo Regional:", ocorrencia.getPoloRegional().getDescricao(),
                "Tipo de Incidente:", ocorrencia.getTipoOcorrencia().getDescricao());

        // Linha 2: Placa e Motorista
        adicionarCampo(contentPanel, gbc, row++, "Placa do Veículo:", PlacaValidator.formatarParaExibicao(ocorrencia.getPlacaVeiculo()),
                "Motorista Responsável:", ocorrencia.getMotorista());

        // Linha 3: Monitor e Gravidade
        adicionarCampo(contentPanel, gbc, row++, "Monitor (Operador):", ocorrencia.getMonitor(),
                "Nível de Gravidade:", ocorrencia.getGravidade().getDescricao());

        // Linha 4: Data de Resolução
        adicionarCampo(contentPanel, gbc, row++, "Data de Resolução:", ocorrencia.getDataResolucao() != null ? DateUtils.formatar(ocorrencia.getDataResolucao()) : "Pendente / Em Aberto",
                "Status Atual:", ocorrencia.getStatus().getDescricao());

        // Linha 4: Descrição completa
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;

        JPanel descBox = new JPanel(new BorderLayout(0, 4));
        descBox.setOpaque(false);
        JLabel lblDescTitle = new JLabel("Descrição Completa dos Fatos:");
        lblDescTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDescTitle.setForeground(new Color(209, 213, 219));

        JTextArea txtDesc = new JTextArea(ocorrencia.getDescricao());
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBackground(new Color(24, 28, 36));
        txtDesc.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        scrollDesc.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81)));
        descBox.add(lblDescTitle, BorderLayout.NORTH);
        descBox.add(scrollDesc, BorderLayout.CENTER);

        contentPanel.add(descBox, gbc);

        // Se houver observação de resolução
        if (ocorrencia.getObservacaoResolucao() != null && !ocorrencia.getObservacaoResolucao().isBlank()) {
            gbc.gridy = row++;
            gbc.weighty = 0.3;
            JPanel resBox = new JPanel(new BorderLayout(0, 4));
            resBox.setOpaque(false);
            JLabel lblResTitle = new JLabel("Observações de Resolução:");
            lblResTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblResTitle.setForeground(new Color(52, 211, 153));

            JTextArea txtRes = new JTextArea(ocorrencia.getObservacaoResolucao());
            txtRes.setEditable(false);
            txtRes.setLineWrap(true);
            txtRes.setWrapStyleWord(true);
            txtRes.setBackground(new Color(20, 35, 30));
            txtRes.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            resBox.add(lblResTitle, BorderLayout.NORTH);
            resBox.add(new JScrollPane(txtRes), BorderLayout.CENTER);
            contentPanel.add(resBox, gbc);
        }

        add(contentPanel, BorderLayout.CENTER);

        // Barra inferior com ações
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(BorderFactory.createEmptyBorder(12, 24, 16, 24));
        bottomBar.setBackground(new Color(30, 34, 42));

        // Esquerda: botões de status rápido
        JPanel statusButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusButtons.setOpaque(false);

        JButton btnAnalise = new JButton("Mover p/ Em Análise");
        btnAnalise.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnAnalise.setEnabled(ocorrencia.getStatus() != StatusOcorrencia.EM_ANALISE);
        btnAnalise.addActionListener(e -> alterarStatusPara(StatusOcorrencia.EM_ANALISE));

        JButton btnResolver = new JButton("Resolver Ocorrência");
        btnResolver.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnResolver.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #059669; foreground: #ffffff;");
        btnResolver.setEnabled(ocorrencia.getStatus() != StatusOcorrencia.RESOLVIDO);
        btnResolver.addActionListener(e -> resolverComObservacao());

        statusButtons.add(btnAnalise);
        statusButtons.add(btnResolver);

        // Direita: Copiar e Fechar
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);

        JButton btnCopiar = new JButton("Copiar Placa");
        btnCopiar.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnCopiar.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(ocorrencia.getPlacaVeiculo()), null);
            JOptionPane.showMessageDialog(this, "Placa copiada para a área de transferência!");
        });

        JButton btnFechar = new JButton("Fechar");
        btnFechar.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnFechar.addActionListener(e -> dispose());

        rightButtons.add(btnCopiar);
        rightButtons.add(btnFechar);

        bottomBar.add(statusButtons, BorderLayout.WEST);
        bottomBar.add(rightButtons, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private void adicionarCampo(JPanel panel, GridBagConstraints gbc, int row,
                                String label1, String valor1, String label2, String valor2) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.gridy = row;

        // Campo 1 Label
        gbc.gridx = 0;
        JLabel l1 = new JLabel(label1);
        l1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l1.setForeground(new Color(156, 163, 175));
        panel.add(l1, gbc);

        // Campo 1 Valor
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        JLabel v1 = new JLabel(valor1);
        v1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        v1.setForeground(Color.WHITE);
        panel.add(v1, gbc);

        // Campo 2 Label
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JLabel l2 = new JLabel(label2);
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l2.setForeground(new Color(156, 163, 175));
        panel.add(l2, gbc);

        // Campo 2 Valor
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        JLabel v2 = new JLabel(valor2);
        v2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        v2.setForeground(Color.WHITE);
        panel.add(v2, gbc);
    }

    private void alterarStatusPara(StatusOcorrencia novoStatus) {
        service.alterarStatus(ocorrencia.getId(), novoStatus, null);
        JOptionPane.showMessageDialog(this, "Status alterado para: " + novoStatus.getDescricao());
        dispose();
        if (onUpdatedCallback != null) {
            onUpdatedCallback.run();
        }
    }

    private void resolverComObservacao() {
        String observacao = JOptionPane.showInputDialog(
                this,
                "Informe uma breve observação ou ação tomada para a resolução:",
                "Resolução de Ocorrência",
                JOptionPane.QUESTION_MESSAGE
        );

        if (observacao != null) { // Usuário não cancelou
            service.alterarStatus(ocorrencia.getId(), StatusOcorrencia.RESOLVIDO, observacao);
            JOptionPane.showMessageDialog(this, "Ocorrência resolvida com sucesso!");
            dispose();
            if (onUpdatedCallback != null) {
                onUpdatedCallback.run();
            }
        }
    }
}
