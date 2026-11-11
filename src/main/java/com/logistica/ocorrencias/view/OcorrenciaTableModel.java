package com.logistica.ocorrencias.view;

import com.logistica.ocorrencias.model.Gravidade;
import com.logistica.ocorrencias.model.Ocorrencia;
import com.logistica.ocorrencias.model.PoloRegional;
import com.logistica.ocorrencias.model.StatusOcorrencia;
import com.logistica.ocorrencias.model.TipoOcorrencia;
import com.logistica.ocorrencias.util.DateUtils;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabela para visualização e ordenação das ocorrências logísticas.
 */
public class OcorrenciaTableModel extends AbstractTableModel {

    private final String[] colunas = {
            "ID",
            "Data / Hora",
            "Polo Regional",
            "Tipo da Ocorrência",
            "Placa",
            "Motorista",
            "Gravidade",
            "Status",
            "Descrição"
    };

    private final Class<?>[] tiposColunas = {
            String.class,
            LocalDateTime.class,
            PoloRegional.class,
            TipoOcorrencia.class,
            String.class,
            String.class,
            Gravidade.class,
            StatusOcorrencia.class,
            String.class
    };

    private List<Ocorrencia> ocorrencias = new ArrayList<>();

    public OcorrenciaTableModel() {
    }

    public OcorrenciaTableModel(List<Ocorrencia> ocorrencias) {
        if (ocorrencias != null) {
            this.ocorrencias = new ArrayList<>(ocorrencias);
        }
    }

    public void setOcorrencias(List<Ocorrencia> novasOcorrencias) {
        this.ocorrencias = (novasOcorrencias != null) ? new ArrayList<>(novasOcorrencias) : new ArrayList<>();
        fireTableDataChanged();
    }

    public Ocorrencia getOcorrenciaAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < ocorrencias.size()) {
            return ocorrencias.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return ocorrencias.size();
    }

    @Override
    public int getColumnCount() {
        return colunas.length;
    }

    @Override
    public String getColumnName(int column) {
        return colunas[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return tiposColunas[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= ocorrencias.size()) {
            return null;
        }

        Ocorrencia o = ocorrencias.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> o.getId();
            case 1 -> o.getDataHora();
            case 2 -> o.getPoloRegional();
            case 3 -> o.getTipoOcorrencia();
            case 4 -> o.getPlacaVeiculo();
            case 5 -> o.getMotorista();
            case 6 -> o.getGravidade();
            case 7 -> o.getStatus();
            case 8 -> o.getDescricao();
            default -> null;
        };
    }
}
