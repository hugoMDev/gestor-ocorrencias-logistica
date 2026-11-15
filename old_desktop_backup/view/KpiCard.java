package com.logistica.ocorrencias.view;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Componente visual de cartão moderno para apresentação de Indicadores Chave (KPIs).
 */
public class KpiCard extends JPanel {

    private final JLabel lblTitulo;
    private final JLabel lblValor;
    private final JLabel lblDescricao;
    private final Color corDestaque;

    public KpiCard(String titulo, String valorInicial, String subtitulo, Color corDestaque) {
        this.corDestaque = corDestaque;
        setLayout(new BorderLayout(8, 4));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setPreferredSize(new Dimension(240, 85));

        // Título superior
        lblTitulo = new JLabel(titulo.toUpperCase());
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitulo.setForeground(new Color(156, 163, 175)); // Cinza claro

        // Valor principal em destaque
        lblValor = new JLabel(valorInicial);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValor.setForeground(Color.WHITE);

        // Subtítulo descritivo
        lblDescricao = new JLabel(subtitulo);
        lblDescricao.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDescricao.setForeground(new Color(107, 114, 128));

        JPanel painelTextos = new JPanel(new BorderLayout(0, 2));
        painelTextos.setOpaque(false);
        painelTextos.add(lblTitulo, BorderLayout.NORTH);
        painelTextos.add(lblValor, BorderLayout.CENTER);
        painelTextos.add(lblDescricao, BorderLayout.SOUTH);

        add(painelTextos, BorderLayout.CENTER);
    }

    public void setValor(int valor) {
        lblValor.setText(String.valueOf(valor));
    }

    public void setValor(String valor) {
        lblValor.setText(valor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fundo com bordas arredondadas e cor elevada suave
        g2.setColor(new Color(36, 40, 48));
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Borda sutil
        g2.setColor(new Color(55, 65, 81));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // Faixa de destaque lateral com cor temática
        g2.setColor(corDestaque);
        g2.fillRoundRect(0, 3, 5, h - 6, 4, 4);

        g2.dispose();
        super.paintComponent(g);
    }
}
