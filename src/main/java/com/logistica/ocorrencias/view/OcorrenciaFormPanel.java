package com.logistica.ocorrencias.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;
import com.logistica.ocorrencias.service.OcorrenciaService;
import com.logistica.ocorrencias.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;

/**
 * Painel lateral moderno de formulário para cadastro e registro ágil de ocorrências logísticas.
 */
public class OcorrenciaFormPanel extends JPanel {

    private final OcorrenciaService service;
    private final Runnable onSalvoCallback;

    private JComboBox<PoloRegional> cbPolo;
    private JComboBox<TipoOcorrencia> cbTipo;
    private JTextField txtPlaca;
    private JTextField txtMotorista;
    private JTextField txtMonitor;
    private JComboBox<Gravidade> cbGravidade;
    private JTextArea txtDescricao;

    private JButton btnSalvar;
    private JButton btnLimpar;

    public OcorrenciaFormPanel(OcorrenciaService service, Runnable onSalvoCallback) {
        this.service = service;
        this.onSalvoCallback = onSalvoCallback;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(360, 600));
        setMinimumSize(new Dimension(320, 500));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(55, 65, 81)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        // Cabeçalho do formulário
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);

        JLabel lblTitulo = new JLabel("Nova Ocorrência");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSubtitulo = new JLabel("Abertura e registro operacional imediato");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(156, 163, 175));

        headerPanel.add(lblTitulo, BorderLayout.NORTH);
        headerPanel.add(lblSubtitulo, BorderLayout.CENTER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        add(headerPanel, BorderLayout.NORTH);

        // Painel central de campos com GridBagLayout
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 8, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        int row = 0;

        // 1. Polo Regional
        gbc.gridy = row++;
        formGrid.add(criarLabel("Polo Regional Operacional:"), gbc);

        cbPolo = new JComboBox<>(PoloRegional.values());
        cbPolo.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        gbc.gridy = row++;
        formGrid.add(cbPolo, gbc);

        // 2. Tipo da Ocorrência
        gbc.gridy = row++;
        formGrid.add(criarLabel("Tipo de Ocorrência:"), gbc);

        cbTipo = new JComboBox<>(TipoOcorrencia.values());
        cbTipo.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        gbc.gridy = row++;
        formGrid.add(cbTipo, gbc);

        // 3. Placa do Veículo
        gbc.gridy = row++;
        formGrid.add(criarLabel("Placa do Veículo (Mercosul ou Tradicional):"), gbc);

        txtPlaca = new JTextField();
        txtPlaca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex: ABC1D23 ou ABC-1234");
        txtPlaca.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtPlaca.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        aplicarFiltroCaixaAlta(txtPlaca, 8);
        gbc.gridy = row++;
        formGrid.add(txtPlaca, gbc);

        // 4. Motorista
        gbc.gridy = row++;
        formGrid.add(criarLabel("Motorista / Condutor:"), gbc);

        txtMotorista = new JTextField();
        txtMotorista.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nome completo do motorista");
        txtMotorista.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtMotorista.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        gbc.gridy = row++;
        formGrid.add(txtMotorista, gbc);

        // 5. Monitor Responsável (Operador que registra)
        gbc.gridy = row++;
        formGrid.add(criarLabel("Monitor Responsável (Operador):"), gbc);

        txtMonitor = new JTextField();
        txtMonitor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nome de quem registra a ocorrência");
        txtMonitor.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtMonitor.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        gbc.gridy = row++;
        formGrid.add(txtMonitor, gbc);

        // 6. Gravidade
        gbc.gridy = row++;
        formGrid.add(criarLabel("Nível de Gravidade:"), gbc);

        cbGravidade = new JComboBox<>(Gravidade.values());
        cbGravidade.setSelectedItem(Gravidade.MEDIA);
        cbGravidade.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        gbc.gridy = row++;
        formGrid.add(cbGravidade, gbc);

        // 6. Descrição Detalhada
        gbc.gridy = row++;
        formGrid.add(criarLabel("Descrição Detalhada dos Fatos:"), gbc);

        txtDescricao = new JTextArea(5, 20);
        txtDescricao.setLineWrap(true);
        txtDescricao.setWrapStyleWord(true);
        txtDescricao.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Detalhe a situação ocorrida na via ou doca...");
        JScrollPane scrollDesc = new JScrollPane(txtDescricao);
        scrollDesc.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formGrid.add(scrollDesc, gbc);

        add(formGrid, BorderLayout.CENTER);

        // Painel inferior de Ações (Botões)
        JPanel actionsPanel = new JPanel(new BorderLayout(10, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        btnLimpar = new JButton("Limpar");
        btnLimpar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnLimpar.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnLimpar.addActionListener(e -> limparCampos());

        btnSalvar = new JButton("Registrar Ocorrência");
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSalvar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnSalvar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #2563eb; foreground: #ffffff;");
        btnSalvar.addActionListener(e -> salvarOcorrencia());

        actionsPanel.add(btnLimpar, BorderLayout.WEST);
        actionsPanel.add(btnSalvar, BorderLayout.CENTER);

        add(actionsPanel, BorderLayout.SOUTH);
    }

    private JLabel criarLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(209, 213, 219));
        return label;
    }

    /**
     * Aplica filtro no documento para garantir maiúsculas automáticas e limite de tamanho.
     */
    private void aplicarFiltroCaixaAlta(JTextField field, int maxLength) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                if ((fb.getDocument().getLength() + string.length()) <= maxLength) {
                    super.insertString(fb, offset, string.toUpperCase(), attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                if ((fb.getDocument().getLength() - length + text.length()) <= maxLength) {
                    super.replace(fb, offset, length, text.toUpperCase(), attrs);
                }
            }
        });
    }

    private void salvarOcorrencia() {
        try {
            Ocorrencia nova = new Ocorrencia(
                    null,
                    LocalDateTime.now(),
                    (PoloRegional) cbPolo.getSelectedItem(),
                    (TipoOcorrencia) cbTipo.getSelectedItem(),
                    txtPlaca.getText(),
                    txtMotorista.getText(),
                    txtMonitor.getText(),
                    (Gravidade) cbGravidade.getSelectedItem(),
                    StatusOcorrencia.PENDENTE,
                    txtDescricao.getText()
            );

            Ocorrencia salva = service.salvar(nova);

            JOptionPane.showMessageDialog(
                    this,
                    "Ocorrência " + salva.getId() + " cadastrada com sucesso!",
                    "Registro Concluído",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limparCampos();

            if (onSalvoCallback != null) {
                onSalvoCallback.run();
            }
        } catch (ValidacaoException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Atenção na Validação",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao cadastrar ocorrência: " + ex.getMessage(),
                    "Erro de Persistência",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void limparCampos() {
        cbPolo.setSelectedIndex(0);
        cbTipo.setSelectedIndex(0);
        txtPlaca.setText("");
        txtMotorista.setText("");
        txtMonitor.setText("");
        cbGravidade.setSelectedItem(Gravidade.MEDIA);
        txtDescricao.setText("");
        txtPlaca.requestFocus();
    }
}
