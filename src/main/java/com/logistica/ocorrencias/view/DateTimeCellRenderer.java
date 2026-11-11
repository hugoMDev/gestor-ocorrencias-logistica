package com.logistica.ocorrencias.view;

import com.logistica.ocorrencias.util.DateUtils;

import javax.swing.table.DefaultTableCellRenderer;
import java.time.LocalDateTime;

/**
 * Renderizador de datas com formatação legível dd/MM/yyyy HH:mm.
 */
public class DateTimeCellRenderer extends DefaultTableCellRenderer {

    public DateTimeCellRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    protected void setValue(Object value) {
        if (value instanceof LocalDateTime ldt) {
            setText(DateUtils.formatar(ldt));
        } else {
            super.setValue(value);
        }
    }
}
