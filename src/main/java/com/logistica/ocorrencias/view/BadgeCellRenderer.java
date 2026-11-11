package com.logistica.ocorrencias.view;

import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.StatusOcorrencia;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Renderizador de células que estiliza Status e Gravidade em formato de "Badges / Pílulas"
 * com cores sutis e modernas de alto contraste.
 */
public class BadgeCellRenderer extends JLabel implements TableCellRenderer {

    private Color badgeBackground = new Color(45, 55, 72);
    private Color badgeForeground = Color.WHITE;
    private boolean isSelected = false;

    public BadgeCellRenderer() {
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        this.isSelected = isSelected;

        if (value instanceof Gravidade gravidade) {
            setText(gravidade.getDescricao());
            this.badgeForeground = gravidade.getBadgeForeground();
            this.badgeBackground = gravidade.getBadgeBackground();
        } else if (value instanceof StatusOcorrencia status) {
            setText(status.getDescricao());
            this.badgeForeground = status.getBadgeForeground();
            this.badgeBackground = status.getBadgeBackground();
        } else {
            setText(value != null ? value.toString() : "-");
            this.badgeForeground = Color.LIGHT_GRAY;
            this.badgeBackground = new Color(50, 50, 60);
        }

        setForeground(badgeForeground);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Se a linha estiver selecionada, desenha primeiro o fundo da seleção da tabela
        if (isSelected) {
            g2.setColor(new Color(51, 65, 85, 120));
            g2.fillRect(0, 0, width, height);
        }

        // Pílula / Badge
        int pillHeight = 22;
        int pillWidth = Math.min(width - 12, 120);
        int x = (width - pillWidth) / 2;
        int y = (height - pillHeight) / 2;

        g2.setColor(badgeBackground);
        g2.fillRoundRect(x, y, pillWidth, pillHeight, 12, 12);

        // Borda sutil com transparência na cor do texto
        g2.setColor(new Color(badgeForeground.getRed(), badgeForeground.getGreen(), badgeForeground.getBlue(), 120));
        g2.drawRoundRect(x, y, pillWidth, pillHeight, 12, 12);

        g2.dispose();
        super.paintComponent(g);
    }
}
