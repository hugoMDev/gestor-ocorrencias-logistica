package com.logistica.ocorrencias.view;

import com.logistica.ocorrencias.util.PlacaValidator;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Font;

/**
 * Renderizador para placas de veículos em fonte monoespaçada estilizada.
 */
public class PlacaCellRenderer extends DefaultTableCellRenderer {

    private final Font monoFont = new Font("Consolas", Font.BOLD, 13);

    public PlacaCellRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    protected void setValue(Object value) {
        setFont(monoFont);
        if (value != null) {
            setText(PlacaValidator.formatarParaExibicao(value.toString()));
        } else {
            setText("-");
        }
    }
}
